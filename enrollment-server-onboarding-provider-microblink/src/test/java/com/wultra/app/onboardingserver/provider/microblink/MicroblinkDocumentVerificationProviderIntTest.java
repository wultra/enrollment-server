/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.provider.microblink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.database.*;
import com.wultra.app.onboardingserver.common.database.entity.DocumentDataEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import okhttp3.mockwebserver.MockWebServer;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for Microblink document verification provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Transactional
@Sql
class MicroblinkDocumentVerificationProviderIntTest {

    private static final long TIMESTAMP_ASSERT_DELTA_MS = 3_000;

    private static final String ACTIVATION_ID = "26c98f91-e373-4bef-8704-7224880a9912";
    private static final String IDENTITY_VERIFICATION_ID = "e0a627b9-9829-4bec-8c8d-db3be4ff03c1";

    private static final String ID_CARD_FRONT_DOCUMENT_ID = "4e3b6b1a-26df-4d3e-9b97-89cf9b1f4c52";
    private static final String ID_CARD_FRONT_UPLOAD_ID = "5b1e3c4d-6f7a-8b90-1234-56789abcdef0";
    private static final String ID_CARD_FRONT_DOCUMENT_IMAGE_BASE64 = "ZHVtbXlfZnJvbnRfZG9jdW1lbnQ=";

    private static final String ID_CARD_BACK_DOCUMENT_ID = "9fa2b0b7-11d2-4d94-bb9d-8f8c3a5f04e6";
    private static final String ID_CARD_BACK_UPLOAD_ID = "0fedcba9-8765-4321-0fed-cba987654321";
    private static final String ID_CARD_BACK_DOCUMENT_IMAGE_BASE64 = "ZHVtbXlfYmFja19kb2N1bWVudA==";

    private static final String ID_CARD_FACE_PHOTO_ID = "2a1c3e4f-5b6d-7e8f-9012-3456789abcde";

    private static final String PASSPORT_DOCUMENT_ID = "d4f5a9c2-8e7b-4a1f-9d34-2b6f3c5e8a91";
    private static final String PASSPORT_UPLOAD_ID = "71b3e2f0-5c9d-4f4a-8e12-9d7c6a4b3f20";

    private static final Pattern MICROBLINK_RESPONSE_IMAGE_PATTERN = Pattern.compile(
            """
                    "images"\\s*:\\s*\\[.*?]\\s*,?""",
            Pattern.DOTALL
    );

    private static MockWebServer mockWebServer;

    private static final Image idCardFrontImage;
    private static final Image idCardBackImage;
    private static final String idCardFacePhotoBase64;
    private static final String microblinkIdCardRejectResponseBody;
    private static final String microblinkIdCardPassResponseBody;
    private static final String idCardFrontExtractionJson;
    private static final String idCardBackExtractionJson;
    private static final String idCardPassValidationResult;
    private static final String idCardRejectValidationResult;
    private static final JsonNode idCardResponseWithoutPersonalDataJson;

    private static final Image passportImage;
    private static final String microblinkPassportPassResponseBody;
    private static final String passportPassExtractionJson;
    private static final String passportPassValidationResult;

    private MicroblinkDocumentVerificationProvider.DocumentVerificationData idCardFrontDocument;
    private MicroblinkDocumentVerificationProvider.DocumentVerificationData idCardBackDocument;
    private MicroblinkDocumentVerificationProvider.DocumentVerificationData passportDocument;

    @Autowired
    private MicroblinkDocumentVerificationProvider microblinkDocumentVerificationProvider;

    @Autowired
    private DocumentDataRepository documentDataRepository;

    @Autowired
    private DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    private IdentityVerificationRepository identityVerificationRepository;

    @Autowired
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    @Autowired
    private DocumentResultRepository documentResultRepository;

    @MockitoBean
    private AuditService auditService;

    private OwnerId ownerId;

    @DynamicPropertySource
    static void setup(final DynamicPropertyRegistry registry) {
        final String url = mockWebServer.url("").toString();
        registry.add("enrollment-server-onboarding.document-verification.microblink.restClientConfig.baseUrl", () -> url);
    }

    static {
        try {
            final var mapper = new ObjectMapper();

            // ID card
            idCardFrontImage = Image.builder()
                    .filename("id_card_front.jpeg")
                    .data(new ClassPathResource("id_card_front.jpeg").getContentAsByteArray())
                    .build();

            idCardBackImage = Image.builder()
                    .filename("id_card_back.jpeg")
                    .data(new ClassPathResource("id_card_back.jpeg").getContentAsByteArray())
                    .build();

            microblinkIdCardRejectResponseBody = new ClassPathResource("microblink_id_card_reject_response_body.json").getContentAsString(StandardCharsets.UTF_8);
            final var rejectResponseTree = mapper.readTree(microblinkIdCardRejectResponseBody);

            idCardRejectValidationResult = MICROBLINK_RESPONSE_IMAGE_PATTERN.matcher(rejectResponseTree.toString())
                    .replaceAll("");

            idCardFrontExtractionJson = rejectResponseTree.path("extraction")
                    .path("viz")
                    .path("front")
                    .toString();
            idCardBackExtractionJson = rejectResponseTree.path("extraction")
                    .path("viz")
                    .path("back")
                    .toString();

            microblinkIdCardPassResponseBody = new ClassPathResource("microblink_id_card_pass_response_body.json").getContentAsString(StandardCharsets.UTF_8);
            final var passResponseTree = mapper.readTree(microblinkIdCardPassResponseBody);

            idCardFacePhotoBase64 = StreamSupport.stream(passResponseTree.path("images").spliterator(), false)
                    .filter(node -> "FaceImage".equals(node.path("name").asText()))
                    .findFirst()
                    .map(node -> node.path("base64").asText())
                    .orElseThrow();

            idCardPassValidationResult = MICROBLINK_RESPONSE_IMAGE_PATTERN.matcher(passResponseTree.toString())
                    .replaceAll("");

            idCardResponseWithoutPersonalDataJson = ((ObjectNode) passResponseTree).remove(List.of("extraction", "images"));

            // Passport
            passportImage = Image.builder()
                    .filename("passport.jpg")
                    .data(new ClassPathResource("passport.jpg").getContentAsByteArray())
                    .build();

            microblinkPassportPassResponseBody = new ClassPathResource("microblink_passport_pass_response_body.json").getContentAsString(StandardCharsets.UTF_8);
            final var passportResponseTree = mapper.readTree(microblinkPassportPassResponseBody);

            passportPassValidationResult = MICROBLINK_RESPONSE_IMAGE_PATTERN.matcher(passportResponseTree.toString())
                    .replaceAll("");

            passportPassExtractionJson = passportResponseTree.path("extraction")
                    .path("viz")
                    .path("front")
                    .toString();
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @BeforeAll
    static void suiteSetup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @BeforeEach
    void setup() {
        ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        ownerId.setUserId("37f9c00e-67ad-47e3-9c02-9a87e61cfa12");

        idCardFrontDocument = MicroblinkDocumentVerificationProvider.DocumentVerificationData.builder()
                .documentId(ID_CARD_FRONT_DOCUMENT_ID)
                .uploadId(ID_CARD_FRONT_UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.FRONT)
                .image(idCardFrontImage)
                .build();

        idCardBackDocument = MicroblinkDocumentVerificationProvider.DocumentVerificationData.builder()
                .documentId(ID_CARD_BACK_DOCUMENT_ID)
                .uploadId(ID_CARD_BACK_UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.BACK)
                .image(idCardBackImage)
                .build();

        passportDocument = MicroblinkDocumentVerificationProvider.DocumentVerificationData.builder()
                .documentId(PASSPORT_DOCUMENT_ID)
                .uploadId(PASSPORT_UPLOAD_ID)
                .type(DocumentType.PASSPORT)
                .side(CardSide.FRONT)
                .image(passportImage)
                .build();
    }

    @AfterEach
    void cleanup() {
        documentVerificationRepository.deleteAll();
        documentDataRepository.deleteAll();
        processedDocumentDataRepository.deleteAll();
    }

    @Test
    void testInitVerificationSdk_sdkConfigNotFound_responseWithoutLicenseKey() {
        // given
        final var initParams = Map.of("origin", "app1", "platform", "android");

        // when
        final var result = microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, initParams);

        // then
        assertEquals(new VerificationSdkInfo(), result);
    }

    @Test
    void testInitVerificationSdk_sdkConfigFound_responseWithLicenseKey() {
        // given
        final var initParams = Map.of("origin", "app1", "platform", "ios");

        // when
        final var result = microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, initParams);

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", "abc")), result);
    }

    @Test
    void testSubmitDocuments_documentWith2SidesUploaded_correctResponseIsReturned() throws Exception {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkIdCardPassResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertIdCardPassSubmitResult(result);
    }

    @Test
    void testSubmitDocuments_documentWith2SidesUploaded_documentDataAreSaved() throws Exception {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkIdCardPassResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertIdCardDocumentsData(result);
    }

    @Test
    void testSubmitDocuments_documentWith2SidesUploaded_processedDocumentDataSaved() throws Exception {
        // given
        prepareIdCardFrontDocumentVerificationInDatabase();
        prepareIdCardBackDocumentVerificationInDatabase();

        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkIdCardPassResponseBody));

        // when
        microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertProcessedDocumentData();
    }

    @Test
    void testSubmitDocuments_documentWith2SidesUploaded_responseLoggedToAudit() throws Exception {
        // given
        prepareIdCardFrontDocumentVerificationInDatabase();
        prepareIdCardBackDocumentVerificationInDatabase();

        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkIdCardPassResponseBody));

        // when
        microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(auditService).auditDocumentVerificationProvider(
                ownerId,
                idCardResponseWithoutPersonalDataJson,
                "Document verification response, user: {}, provider: Microblink, documentType: {}",
                ownerId.getUserId(),
                DocumentType.ID_CARD);
    }

    @Test
    void testSubmitDocuments_microblinkClientException_exceptionIsThrown() {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(503)
                .setBody("Service Not Available"));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class,
                () -> microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments)
        );

        // then
        assertEquals("Failed REST API call to Microblink, statusCode=503 SERVICE_UNAVAILABLE, responseBody='Service Not Available'", exception.getMessage());
    }

    @Test
    void testSubmitDocuments_documentWith1SidesUploaded_correctResponseIsReturned() throws Exception {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(passportDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkPassportPassResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertPassportPassSubmitResult(result);
    }

    @Test
    void testSubmitDocuments_microblinkRejectResponse_correctResponseIsReturned() throws Exception {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkIdCardRejectResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertIdCardRejectSubmitResult(result);
    }

    @Test
    void testGetPhoto_photoNotFound_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> microblinkDocumentVerificationProvider.getPhoto(ID_CARD_FACE_PHOTO_ID));

        // then
        assertEquals("Photo with id 2a1c3e4f-5b6d-7e8f-9012-3456789abcde not found", exception.getMessage());
    }

    @Test
    void testGetPhoto_photoFound_correctImageIsReturned() throws Exception {
        // given
        preparePhotoInDatabase();

        // when
        final var actualImage = microblinkDocumentVerificationProvider.getPhoto(ID_CARD_FACE_PHOTO_ID);

        // then
        final var expectedImage = Image.builder()
                .data(Base64.getDecoder().decode(idCardFacePhotoBase64))
                .filename("FaceImage.jpg")
                .build();

        assertEquals(expectedImage, actualImage);
    }

    @Test
    void testVerifyDocuments_successfulVerification_correctResponseIsReturned() throws Exception {
        // given
        prepareIdCardFrontVerificationDataInDatabase();
        prepareIdCardBackVerificationDataInDatabase();
        preparePassportVerificationDataInDatabase(passportPassValidationResult);

        final var uploadIds = List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID, PASSPORT_UPLOAD_ID);

        // when
        final var response = microblinkDocumentVerificationProvider.verifyDocuments(ownerId, uploadIds);

        // then
        assertDocumentsVerificationResultSuccess(response);
    }

    @Test
    void testVerifyDocuments_failVerification_correctResponseIsReturned() {
        // given
        prepareIdCardFrontVerificationDataInDatabase();
        prepareIdCardBackVerificationDataInDatabase();

        final var passportValidationResult = passportPassValidationResult.replaceAll(
                "\"value\"\\s*:\\s*\"PRENUMELE\"",
                "\"value\": \"INCORRECT_FIRSTNAME\""
        );
        preparePassportVerificationDataInDatabase(passportValidationResult);

        final var uploadIds = List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID, PASSPORT_UPLOAD_ID);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> microblinkDocumentVerificationProvider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Crosscheck failed for field firstName", exception.getMessage());
    }

    private List<SubmittedDocument> buildSubmittedDocuments(final List<MicroblinkDocumentVerificationProvider.DocumentVerificationData> documents) {
        return documents.stream()
                .map(d -> {
                    final var doc = new SubmittedDocument();
                    doc.setDocumentId(d.documentId());
                    doc.setType(d.type());
                    doc.setSide(d.side());
                    doc.setPhoto(d.image());
                    return doc;
                })
                .toList();
    }

    private void assertIdCardPassSubmitResult(final DocumentsSubmitResult result) {
        assertDoesNotThrow(() -> UUID.fromString(result.getExtractedPhotoId()));
        assertNull(result.getErrorDetail());
        assertNull(result.getRejectReason());

        final var actualDocuments = result.getResults();
        assertEquals(2, actualDocuments.size());

        final var frontDocument = actualDocuments.stream()
                .filter(d -> d.getDocumentId().equals(ID_CARD_FRONT_DOCUMENT_ID))
                .findFirst()
                .orElseThrow();

        final var frontNormalizedExtractedData = buildIdCardFrontNormalizedExtractedDataJson();

        assertDoesNotThrow(() -> UUID.fromString(frontDocument.getUploadId()));
        assertNull(frontDocument.getRejectReason());
        assertEquals(frontNormalizedExtractedData, frontDocument.getExtractedData());
        assertJsonEquals(idCardPassValidationResult, frontDocument.getValidationResult());

        final var backDocument = actualDocuments.stream()
                .filter(d -> d.getDocumentId().equals(ID_CARD_BACK_DOCUMENT_ID))
                .findFirst()
                .orElseThrow();

        final var backNormalizedExtractedData = buildIdCardBackNormalizedExtractedDataJson();

        assertDoesNotThrow(() -> UUID.fromString(backDocument.getUploadId()));
        assertNull(backDocument.getRejectReason());
        assertEquals(backNormalizedExtractedData, backDocument.getExtractedData());
        assertJsonEquals(idCardPassValidationResult, backDocument.getValidationResult());
    }

    private void assertIdCardRejectSubmitResult(final DocumentsSubmitResult result) {
        assertDoesNotThrow(() -> UUID.fromString(result.getExtractedPhotoId()));
        assertNull(result.getErrorDetail());
        assertEquals("Rejected documents: [4e3b6b1a-26df-4d3e-9b97-89cf9b1f4c52, 9fa2b0b7-11d2-4d94-bb9d-8f8c3a5f04e6]", result.getRejectReason());

        final var actualDocuments = result.getResults();
        assertEquals(2, actualDocuments.size());

        final var frontDocument = actualDocuments.stream()
                .filter(d -> d.getDocumentId().equals(ID_CARD_FRONT_DOCUMENT_ID))
                .findFirst()
                .orElseThrow();

        final var frontNormalizedExtractedData = buildIdCardFrontNormalizedExtractedDataJson();

        assertDoesNotThrow(() -> UUID.fromString(frontDocument.getUploadId()));
        assertEquals("[The provided document is fully cropped which is not in line with BlinkID Verify image quality guidelines.]", frontDocument.getRejectReason());
        assertEquals(frontNormalizedExtractedData, frontDocument.getExtractedData());
        assertJsonEquals(idCardRejectValidationResult, frontDocument.getValidationResult());

        final var backDocument = actualDocuments.stream()
                .filter(d -> d.getDocumentId().equals(ID_CARD_BACK_DOCUMENT_ID))
                .findFirst()
                .orElseThrow();

        final var backNormalizedExtractedData = buildIdCardBackNormalizedExtractedDataJson();

        assertDoesNotThrow(() -> UUID.fromString(backDocument.getUploadId()));
        assertEquals("[The provided document is fully cropped which is not in line with BlinkID Verify image quality guidelines.]", backDocument.getRejectReason());
        assertEquals(backNormalizedExtractedData, backDocument.getExtractedData());
        assertJsonEquals(idCardRejectValidationResult, backDocument.getValidationResult());
    }

    private void assertPassportPassSubmitResult(final DocumentsSubmitResult result) {
        assertNull(result.getExtractedPhotoId());
        assertNull(result.getErrorDetail());
        assertNull(result.getRejectReason());

        final var actualDocuments = result.getResults();
        assertEquals(1, actualDocuments.size());

        final var document = actualDocuments.stream()
                .findFirst()
                .orElseThrow();

        final var passportNormalizedExtractedData = buildPassportNormalizedExtractedDataJson();

        assertDoesNotThrow(() -> UUID.fromString(document.getUploadId()));
        assertNull(document.getRejectReason());
        assertEquals(passportNormalizedExtractedData, document.getExtractedData());
        assertJsonEquals(passportPassValidationResult, document.getValidationResult());
    }

    private void assertIdCardDocumentsData(final DocumentsSubmitResult result) {
        final var documentDataByUploadId = StreamSupport.stream(documentDataRepository.findAll().spliterator(), false)
                .collect(Collectors.toMap(DocumentDataEntity::getId, i -> i));

        assertEquals(2, documentDataByUploadId.size());

        final var frontDocumentUploadId = result.getResults().stream()
                .filter(d -> d.getDocumentId().equals(ID_CARD_FRONT_DOCUMENT_ID))
                .findFirst()
                .orElseThrow()
                .getUploadId();

        final var frontDocumentData = documentDataByUploadId.get(frontDocumentUploadId);
        assertArrayEquals(idCardFrontImage.getData(), frontDocumentData.getData());
        assertEquals(new Date().getTime(), frontDocumentData.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);

        final var backDocumentUploadId = result.getResults().stream()
                .filter(d -> d.getDocumentId().equals(ID_CARD_BACK_DOCUMENT_ID))
                .findFirst()
                .orElseThrow()
                .getUploadId();

        final var backDocumentData = documentDataByUploadId.get(backDocumentUploadId);
        assertArrayEquals(idCardBackImage.getData(), backDocumentData.getData());
        assertEquals(new Date().getTime(), backDocumentData.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
    }

    private void assertProcessedDocumentData() {
        final var processedDocumentData = StreamSupport.stream(processedDocumentDataRepository.findAll().spliterator(), false)
                .collect(Collectors.toMap(ProcessedDocumentDataEntity::getDataType, Function.identity()));

        assertEquals(3, processedDocumentData.size());

        final var facePhoto = processedDocumentData.get(ProcessedDocumentDataType.FACE_IMAGE);
        assertDoesNotThrow(() -> UUID.fromString(facePhoto.getId()));
        assertEquals(idCardFacePhotoBase64, Base64.getEncoder().encodeToString(facePhoto.getData()));
        assertEquals(new Date().getTime(), facePhoto.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
        assertDoesNotThrow(() -> UUID.fromString(facePhoto.getDocumentVerificationId()));

        final var frontDocument = processedDocumentData.get(ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE);
        assertDoesNotThrow(() -> UUID.fromString(frontDocument.getId()));
        assertEquals(ID_CARD_FRONT_DOCUMENT_IMAGE_BASE64, Base64.getEncoder().encodeToString(frontDocument.getData()));
        assertEquals(new Date().getTime(), frontDocument.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
        assertDoesNotThrow(() -> UUID.fromString(frontDocument.getDocumentVerificationId()));

        final var backDocument = processedDocumentData.get(ProcessedDocumentDataType.DOCUMENT_BACK_SIDE);
        assertDoesNotThrow(() -> UUID.fromString(backDocument.getId()));
        assertEquals(ID_CARD_BACK_DOCUMENT_IMAGE_BASE64, Base64.getEncoder().encodeToString(backDocument.getData()));
        assertEquals(new Date().getTime(), backDocument.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
        assertDoesNotThrow(() -> UUID.fromString(backDocument.getDocumentVerificationId()));
    }

    private void prepareIdCardFrontVerificationDataInDatabase() {
        final var documentData = new DocumentDataEntity();
        documentData.setId(ID_CARD_FRONT_UPLOAD_ID);
        documentData.setData(new byte[] { 1, 2 });
        documentData.setTimestampCreated(new Date());
        documentDataRepository.save(documentData);

        final var documentVerification = prepareIdCardFrontDocumentVerificationInDatabase();

        final var documentResult = new DocumentResultEntity();
        documentResult.setPhase(DocumentProcessingPhase.VERIFICATION);
        documentResult.setVerificationResult(idCardPassValidationResult);
        documentResult.setExtractedData(idCardFrontExtractionJson);
        documentResult.setDocumentVerification(documentVerification);
        documentResult.setTimestampCreated(new Date());

        documentVerification.setResults(Set.of(documentResult));

        documentResultRepository.save(documentResult);
    }

    private void prepareIdCardBackVerificationDataInDatabase() {
        final var documentData = new DocumentDataEntity();
        documentData.setId(ID_CARD_BACK_UPLOAD_ID);
        documentData.setData(new byte[] { 3, 4 });
        documentData.setTimestampCreated(new Date());
        documentDataRepository.save(documentData);

        final var documentVerification = prepareIdCardBackDocumentVerificationInDatabase();

        final var documentResult = new DocumentResultEntity();
        documentResult.setPhase(DocumentProcessingPhase.VERIFICATION);
        documentResult.setVerificationResult(idCardPassValidationResult);
        documentResult.setExtractedData(idCardBackExtractionJson);
        documentResult.setDocumentVerification(documentVerification);
        documentResult.setTimestampCreated(new Date());

        documentVerification.setResults(Set.of(documentResult));

        documentResultRepository.save(documentResult);
    }

    private void preparePassportVerificationDataInDatabase(final String validationResultJson) {
        final var documentData = new DocumentDataEntity();
        documentData.setId(PASSPORT_UPLOAD_ID);
        documentData.setData(new byte[] { 5, 6 });
        documentData.setTimestampCreated(new Date());
        documentDataRepository.save(documentData);

        final var identityVerification = identityVerificationRepository.findById("e0a627b9-9829-4bec-8c8d-db3be4ff03c1").orElseThrow();

        final var documentVerification = new DocumentVerificationEntity();
        documentVerification.setActivationId(ACTIVATION_ID);
        documentVerification.setIdentityVerification(identityVerification);
        documentVerification.setType(DocumentType.PASSPORT);
        documentVerification.setProviderName("microblink");
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        documentVerification.setFilename("passport.jpeg");
        documentVerification.setUploadId(PASSPORT_UPLOAD_ID);
        documentVerification.setVerificationId(UUID.randomUUID().toString());
        documentVerification.setOriginalDocumentId(PASSPORT_DOCUMENT_ID);
        documentVerification.setTimestampCreated(new Date());

        final var savedDocumentVerification = documentVerificationRepository.save(documentVerification);

        final var documentResult = new DocumentResultEntity();
        documentResult.setPhase(DocumentProcessingPhase.VERIFICATION);
        documentResult.setVerificationResult(validationResultJson);
        documentResult.setExtractedData(passportPassExtractionJson);
        documentResult.setTimestampCreated(new Date());
        documentResult.setDocumentVerification(savedDocumentVerification);

        savedDocumentVerification.setResults(Set.of(documentResult));

        documentResultRepository.save(documentResult);
    }

    private DocumentVerificationEntity prepareIdCardFrontDocumentVerificationInDatabase() {
        final var identityVerification = identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID).orElseThrow();

        final var documentVerification = new DocumentVerificationEntity();
        //documentVerification.setId(ID_CARD_FRONT_DOCUMENT_VERIFICATION_ID);
        documentVerification.setActivationId(ACTIVATION_ID);
        documentVerification.setIdentityVerification(identityVerification);
        documentVerification.setType(DocumentType.ID_CARD);
        documentVerification.setSide(CardSide.FRONT);
        documentVerification.setProviderName("microblink");
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        documentVerification.setFilename("id_card_front.jpeg");
        documentVerification.setUploadId(ID_CARD_FRONT_UPLOAD_ID);
        documentVerification.setVerificationId(UUID.randomUUID().toString());
        documentVerification.setPhotoId(ID_CARD_FACE_PHOTO_ID);
        documentVerification.setOriginalDocumentId(ID_CARD_FRONT_DOCUMENT_ID);
        documentVerification.setTimestampCreated(new Date());

        return documentVerificationRepository.save(documentVerification);
    }

    private DocumentVerificationEntity prepareIdCardBackDocumentVerificationInDatabase() {
        final var identityVerification = identityVerificationRepository.findById("e0a627b9-9829-4bec-8c8d-db3be4ff03c1").orElseThrow();

        final var documentVerification = new DocumentVerificationEntity();
        documentVerification.setActivationId(ACTIVATION_ID);
        documentVerification.setIdentityVerification(identityVerification);
        documentVerification.setType(DocumentType.ID_CARD);
        documentVerification.setSide(CardSide.BACK);
        documentVerification.setProviderName("microblink");
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        documentVerification.setFilename("id_card_back.jpeg");
        documentVerification.setUploadId(ID_CARD_BACK_UPLOAD_ID);
        documentVerification.setVerificationId(UUID.randomUUID().toString());
        documentVerification.setPhotoId(ID_CARD_FACE_PHOTO_ID);
        documentVerification.setOriginalDocumentId(ID_CARD_BACK_DOCUMENT_ID);
        documentVerification.setTimestampCreated(new Date());

        return documentVerificationRepository.save(documentVerification);
    }

    private void preparePhotoInDatabase() {
        final var entity = new ProcessedDocumentDataEntity();
        entity.setId(ID_CARD_FACE_PHOTO_ID);
        entity.setData(Base64.getDecoder().decode(idCardFacePhotoBase64));
        entity.setDataType(ProcessedDocumentDataType.FACE_IMAGE);
        entity.setTimestampCreated(new Date());

        processedDocumentDataRepository.save(entity);
    }

    private void assertDocumentsVerificationResultSuccess(final DocumentsVerificationResult result) {
        assertDoesNotThrow(() -> UUID.fromString(result.getVerificationId()));
        assertEquals(DocumentVerificationStatus.ACCEPTED, result.getStatus());
        assertNull(result.getRejectReason());
        assertNull(result.getErrorDetail());

        final var documentResults = result.getResults();
        assertEquals(3, documentResults.size());

        assertDocumentVerificationResult(documentResults, ID_CARD_FRONT_UPLOAD_ID, idCardPassValidationResult, idCardFrontExtractionJson);
        assertDocumentVerificationResult(documentResults, ID_CARD_BACK_UPLOAD_ID, idCardPassValidationResult, idCardBackExtractionJson);
        assertDocumentVerificationResult(documentResults, PASSPORT_UPLOAD_ID, passportPassValidationResult, passportPassExtractionJson);
    }

    private static void assertDocumentVerificationResult(
            final  List<DocumentVerificationResult> documentResults,
            final String uploadId,
            final String expectedValidationResult,
            final String expectedExtractedData
    ) {
        final var documentResult = documentResults.stream()
                .filter(r -> r.getUploadId().equals(uploadId))
                .findFirst()
                .orElseThrow();

        assertNull(documentResult.getRejectReason());
        assertEquals(expectedValidationResult, documentResult.getVerificationResult());
        assertNull(documentResult.getErrorDetail());
        assertEquals(expectedExtractedData, documentResult.getExtractedData());
        assertEquals(10, documentResult.getVerificationScore());
    }

    private static void assertJsonEquals(final String expected, final String actual) {
        try {
            JSONAssert.assertEquals(expected, actual, true);
        } catch (JSONException e) {
            fail("JSON comparison failed", e);
        }
    }

    private static String buildPassportNormalizedExtractedDataJson() {
        return """
                {"givenNames":"PRENUMELE","surname":"NUMELE","dateOfBirth":null,"placeOfBirth":null,"country":"MDA","sex":"X","nationality":"MDA ZZ LL AAAA","personalNumber":null,"documentNumber":"EA0000000","dateOfIssue":null,"dateOfExpiry":"2025-11-14","authority":null}""";
    }

    private static String buildIdCardFrontNormalizedExtractedDataJson() {
        return """
                {"givenNames":"PRENUMELE","surname":"NUMELE","dateOfBirth":null,"placeOfBirth":"Prague","country":"MDA","sex":"X","nationality":"Moldovan","personalNumber":"816008/0610","documentNumber":"EA0000000","dateOfIssue":null,"dateOfExpiry":"2025-10-13","authority":"Chisinau A"}""";
    }

    private static String buildIdCardBackNormalizedExtractedDataJson() {
        return """
                {"givenNames":null,"surname":null,"dateOfBirth":null,"placeOfBirth":null,"country":"MDA","sex":null,"nationality":null,"personalNumber":null,"documentNumber":null,"dateOfIssue":null,"dateOfExpiry":null,"authority":null}""";
    }
}
