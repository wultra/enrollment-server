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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentDataEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.response.v3.GetActivationStatusResponse;
import com.wultra.security.powerauth.client.v3.PowerAuthClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

    private static final String ID_CARD_FRONT_DOCUMENT_ID = "4e3b6b1a-26df-4d3e-9b97-89cf9b1f4c52";
    private static final String ID_CARD_FRONT_UPLOAD_ID = "5b1e3c4d-6f7a-8b90-1234-56789abcdef0";

    private static final String ID_CARD_BACK_DOCUMENT_ID = "9fa2b0b7-11d2-4d94-bb9d-8f8c3a5f04e6";
    private static final String ID_CARD_BACK_UPLOAD_ID = "0fedcba9-8765-4321-0fed-cba987654321";

    private static final String ID_CARD_FACE_PHOTO_ID = "2a1c3e4f-5b6d-7e8f-9012-3456789abcde";

    private static MockWebServer mockWebServer;
    private static Image idCardFrontImage;
    private static Image idCardBackImage;
    private static String idCardFacePhotoBase64;
    private static String microblinkRejectResponseBody;
    private static String microblinkPassResponseBody;
    private static String verificationRejectJson;
    private static String verificationPassJson;
    private static String idCardFrontExtractionJson;
    private static String idCardBackExtractionJson;

    private MicroblinkVerificationData.Document idCardFrontDocument;
    private MicroblinkVerificationData.Document idCardBackDocument;

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

    @MockitoBean
    private PowerAuthClient powerAuthClient;

    private OwnerId ownerId;

    @DynamicPropertySource
    static void setup(final DynamicPropertyRegistry registry) {
        final String url = mockWebServer.url("").toString();
        registry.add("enrollment-server-onboarding.document-verification.microblink.restClientConfig.baseUrl", () -> url);
    }

    @BeforeAll
    static void suiteSetup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        idCardFrontImage = Image.builder()
                .filename("id_card_front.jpeg")
                .data(new ClassPathResource("id_card_front.jpeg").getContentAsByteArray())
                .build();

        idCardBackImage = Image.builder()
                .filename("id_card_back.jpeg")
                .data(new ClassPathResource("id_card_back.jpeg").getContentAsByteArray())
                .build();

        final var mapper = new ObjectMapper();

        microblinkRejectResponseBody = new ClassPathResource("microblink_reject_response_body.json").getContentAsString(StandardCharsets.UTF_8);
        final var rejectResponseTree = mapper.readTree(microblinkRejectResponseBody);
        verificationRejectJson = rejectResponseTree.path("verification").toString();
        idCardFrontExtractionJson = rejectResponseTree.path("extraction")
                .path("viz")
                .path("front")
                .toString();
        idCardBackExtractionJson = rejectResponseTree.path("extraction")
                .path("viz")
                .path("back")
                .toString();

        microblinkPassResponseBody = new ClassPathResource("microblink_pass_response_body.json").getContentAsString(StandardCharsets.UTF_8);
        final var passResponseTree = mapper.readTree(microblinkPassResponseBody);
        verificationPassJson = passResponseTree.path("verification").toString();

        idCardFacePhotoBase64 = StreamSupport.stream(passResponseTree.path("images").spliterator(), false)
                .filter(node -> "FaceImage".equals(node.path("name").asText()))
                .findFirst()
                .map(node -> node.path("base64").asText())
                .orElseThrow();
    }

    @BeforeEach
    void setup() {
        ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        ownerId.setUserId("37f9c00e-67ad-47e3-9c02-9a87e61cfa12");

        idCardFrontDocument = MicroblinkVerificationData.Document.builder()
                .documentId(ID_CARD_FRONT_DOCUMENT_ID)
                .uploadId(ID_CARD_FRONT_UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.FRONT)
                .image(idCardFrontImage)
                .build();

        idCardBackDocument = MicroblinkVerificationData.Document.builder()
                .documentId(ID_CARD_BACK_DOCUMENT_ID)
                .uploadId(ID_CARD_BACK_UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.BACK)
                .image(idCardBackImage)
                .build();
    }

    @AfterEach
    void cleanup() {
        documentVerificationRepository.deleteAll();
        documentDataRepository.deleteAll();

    }

    @Test
    void testInitVerificationSdk_platformFetchedFromPowerAuthServer_responseWithLicenseKey() throws RemoteCommunicationException, PowerAuthClientException {
        // given
        final var response = new GetActivationStatusResponse();
        response.setPlatform("android");
        when(powerAuthClient.getActivationStatus(ACTIVATION_ID)).thenReturn(response);

        // when
        final var result = microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, Map.of());

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", "dummy-android-license-key")), result);
    }

    @Test
    void testInitVerificationSdk_platformFetchedFromPowerAuthServerFail_exceptionIsThrown() throws PowerAuthClientException {
        // given
        when(powerAuthClient.getActivationStatus(ACTIVATION_ID)).thenThrow(new PowerAuthClientException("Test exception"));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class, () -> microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, Map.of()));

        // then
        assertEquals("Error when fetching mobile platform", exception.getMessage());
    }

    @Test
    void testInitVerificationSdk_androidMobilePlatform_responseWithLicenseKey() throws RemoteCommunicationException {
        // given
        // -

        // when
        final var result = microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, Map.of("platform", "android"));

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", "dummy-android-license-key")), result);
    }

    @Test
    void testInitVerificationSdk_iosMobilePlatform_responseWithLicenseKey() throws RemoteCommunicationException {
        // given
        // -

        // when
        final var result = microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, Map.of("platform", "ios"));

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", "dummy-ios-license-key")), result);
    }

    @Test
    void testSubmitDocuments_documentsUploaded_correctResponseIsReturned() {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentsSubmitResult(result, List.of(idCardFrontDocument, idCardBackDocument));
    }

    @Test
    void testSubmitDocuments_documentsUploaded_documentsStoredInDatabase() {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertStoredDocuments(result, submittedDocuments);
    }

    @Test
    void testVerifyDocuments_documentDataNotFound_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID);

        // when
        final var exception = assertThrows(DocumentVerificationException.class,
                () -> microblinkDocumentVerificationProvider.verifyDocuments(ownerId, uploadIds)
        );

        // then
        assertEquals("No document data found for uploadIds: %s".formatted(uploadIds), exception.getMessage());
    }

    @Test
    void testVerifyDocuments_microblinkServiceIsNotAvailable_exceptionIsThrown() {
        // given
        prepareVerificationDataInDatabase();

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(503)
                .setBody("Service Not Available"));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class,
                () -> microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID))
        );

        // then
        assertEquals("Failed REST API call to Microblink, statusCode=503 SERVICE_UNAVAILABLE, responseBody='Service Not Available'", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_microblinkReturnsRejectResult_correctResponseIsReturned() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInDatabase();

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkRejectResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertVerificationResult(result, DocumentVerificationStatus.REJECTED, microblinkRejectResponseBody);
    }

    @Test
    void testVerifyDocuments_microblinkReturnsRejectResult_facePhotoIsStoredInDatabase() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInDatabase();

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkRejectResponseBody));

        // when
        microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertStoredFacePhoto();
    }

    @Test
    void testVerifyDocuments_microblinkReturnsPassResult_correctResponseIsReturned() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInDatabase();

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkPassResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertVerificationResult(result, DocumentVerificationStatus.ACCEPTED, microblinkPassResponseBody);
    }

    @Test
    void testVerifyDocuments_microblinkReturnsPassResult_facePhotoIsStoredInDatabase() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInDatabase();

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkPassResponseBody));

        // when
        microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertStoredFacePhoto();
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
    void testGetPhoto_photoFound_correctImageIsReturned() throws DocumentVerificationException {
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
    void testCleanupDocuments_verificationDataNotFound_noExceptionIsThrown() {
        // given
        // -

        // when, then
        assertDoesNotThrow(
                () -> microblinkDocumentVerificationProvider.cleanupDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID))
        );
    }

    @Test
    void testCleanupDocuments_verificationDataFound_onlyDocumentDataAreDeleted() {
        // given
        prepareVerificationDataInDatabase();

        // when
        microblinkDocumentVerificationProvider.cleanupDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertEquals(0, documentDataRepository.count());
        assertEquals(2, documentVerificationRepository.count());
    }

    private List<SubmittedDocument> buildSubmittedDocuments(final List<MicroblinkVerificationData.Document> documents) {
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

    private void assertDocumentsSubmitResult(final DocumentsSubmitResult result, final List<MicroblinkVerificationData.Document> expectedDocuments) {
        assertDoesNotThrow(() -> UUID.fromString(result.getExtractedPhotoId()));
        assertNull(result.getErrorDetail());
        assertNull(result.getRejectReason());

        final var actualDocuments = result.getResults();
        final var expectedDocumentIds = expectedDocuments.stream()
                .map(MicroblinkVerificationData.Document::documentId)
                .collect(Collectors.toSet());

        assertEquals(expectedDocumentIds.size(), actualDocuments.size());

        for (final var actualDocument : actualDocuments) {
            assertTrue(expectedDocumentIds.contains(actualDocument.getDocumentId()));
            assertDoesNotThrow(() -> UUID.fromString(actualDocument.getUploadId()));
            assertNull(actualDocument.getRejectReason());
            assertNull(actualDocument.getValidationResult());
            assertNull(actualDocument.getErrorDetail());
            assertEquals("{}", actualDocument.getExtractedData());
        }
    }

    private void assertStoredDocuments(final DocumentsSubmitResult result, final List<SubmittedDocument> submittedDocuments) {
        final var documentDataByUploadId = StreamSupport.stream(documentDataRepository.findAll().spliterator(), false)
                .collect(Collectors.toMap(DocumentDataEntity::getId, i -> i));

        final var documentResultByDocumentId = result.getResults()
                .stream()
                .collect(Collectors.toMap(DocumentSubmitResult::getDocumentId, i -> i));

        assertEquals(submittedDocuments.size(), documentDataByUploadId.size());

        for (final var submittedDocument : submittedDocuments) {
            final var documentId = submittedDocument.getDocumentId();
            final var documentResult = documentResultByDocumentId.get(documentId);
            final var documentData = documentDataByUploadId.get(documentResult.getUploadId());

            assertEquals(documentResult.getUploadId(), documentData.getId());
            assertArrayEquals(submittedDocument.getPhoto().getData(), documentData.getData());
            assertEquals(new Date().getTime(), documentData.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
            assertNull(documentData.getDocumentVerification());
        }
    }

    private void prepareVerificationDataInDatabase() {
        final var idCardFrontDocumentData = new DocumentDataEntity();
        idCardFrontDocumentData.setId(ID_CARD_FRONT_UPLOAD_ID);
        idCardFrontDocumentData.setData(new byte[] { 1, 2 });
        idCardFrontDocumentData.setTimestampCreated(new Date());
        final var idCardFrontDocumentDataSaved = documentDataRepository.save(idCardFrontDocumentData);

        final var idCardBackDocumentData = new DocumentDataEntity();
        idCardBackDocumentData.setId(ID_CARD_BACK_UPLOAD_ID);
        idCardBackDocumentData.setData(new byte[] { 3, 4 });
        idCardBackDocumentData.setTimestampCreated(new Date());
        final var idCardBackDocumentDataSaved = documentDataRepository.save(idCardBackDocumentData);

        final var identityVerification = identityVerificationRepository.findById("e0a627b9-9829-4bec-8c8d-db3be4ff03c1").orElseThrow();

        final var idCardFrontVerification = new DocumentVerificationEntity();
        idCardFrontVerification.setActivationId(ACTIVATION_ID);
        idCardFrontVerification.setIdentityVerification(identityVerification);
        idCardFrontVerification.setType(DocumentType.ID_CARD);
        idCardFrontVerification.setSide(CardSide.FRONT);
        idCardFrontVerification.setProviderName("microblink");
        idCardFrontVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        idCardFrontVerification.setFilename("id_card_front.jpeg");
        idCardFrontVerification.setDocumentData(idCardFrontDocumentDataSaved);
        idCardFrontVerification.setVerificationId(UUID.randomUUID().toString());
        idCardFrontVerification.setPhotoId(ID_CARD_FACE_PHOTO_ID);
        idCardFrontVerification.setOriginalDocumentId(ID_CARD_FRONT_DOCUMENT_ID);
        idCardFrontVerification.setTimestampCreated(new Date());

        idCardFrontDocumentDataSaved.setDocumentVerification(idCardFrontVerification);

        documentVerificationRepository.save(idCardFrontVerification);

        final var idCardBackVerification = new DocumentVerificationEntity();
        idCardBackVerification.setActivationId(ACTIVATION_ID);
        idCardBackVerification.setIdentityVerification(identityVerification);
        idCardBackVerification.setType(DocumentType.ID_CARD);
        idCardBackVerification.setSide(CardSide.BACK);
        idCardBackVerification.setProviderName("microblink");
        idCardBackVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        idCardBackVerification.setFilename("id_card_back.jpeg");
        idCardBackVerification.setDocumentData(idCardBackDocumentDataSaved);
        idCardBackVerification.setVerificationId(UUID.randomUUID().toString());
        idCardBackVerification.setPhotoId(ID_CARD_FACE_PHOTO_ID);
        idCardBackVerification.setOriginalDocumentId(ID_CARD_BACK_DOCUMENT_ID);
        idCardBackVerification.setTimestampCreated(new Date());

        idCardBackDocumentDataSaved.setDocumentVerification(idCardBackVerification);

        documentVerificationRepository.save(idCardBackVerification);
    }

    private void preparePhotoInDatabase() {
        final var entity = new ProcessedDocumentDataEntity();
        entity.setId(ID_CARD_FACE_PHOTO_ID);
        entity.setData(Base64.getDecoder().decode(idCardFacePhotoBase64));
        entity.setDataType(ProcessedDocumentDataType.FACE_IMAGE);
        entity.setTimestampCreated(new Date());

        processedDocumentDataRepository.save(entity);
    }

    private void assertVerificationResult(DocumentsVerificationResult result, final DocumentVerificationStatus status, final String verificationJson) {
        assertDoesNotThrow(() -> UUID.fromString(result.getVerificationId()));
        assertEquals(status, result.getStatus());
        assertNull(result.getVerificationScore());
        assertNull(result.getRejectReason());
        assertNull(result.getErrorDetail());

        final var documents = result.getResults();
        assertEquals(2, documents.size());

        final var documentFront = documents.stream()
                .filter(d -> d.getUploadId().equals(ID_CARD_FRONT_UPLOAD_ID))
                .findFirst()
                .orElseThrow();

        assertNull(documentFront.getRejectReason());
        assertEquals(verificationJson, documentFront.getVerificationResult());
        assertNull(documentFront.getErrorDetail());
        assertEquals(idCardFrontExtractionJson, documentFront.getExtractedData());

        final var documentBack = documents.stream()
                .filter(d -> d.getUploadId().equals(ID_CARD_BACK_UPLOAD_ID))
                .findFirst()
                .orElseThrow();

        assertNull(documentBack.getRejectReason());
        assertEquals(verificationJson, documentBack.getVerificationResult());
        assertNull(documentBack.getErrorDetail());
        assertEquals(idCardBackExtractionJson, documentBack.getExtractedData());
    }

    private void assertStoredFacePhoto() {
        final var storedFacePhoto = processedDocumentDataRepository.findById(ID_CARD_FACE_PHOTO_ID).orElseThrow();

        assertArrayEquals(Base64.getDecoder().decode(idCardFacePhotoBase64), storedFacePhoto.getData());
        assertEquals(ProcessedDocumentDataType.FACE_IMAGE, storedFacePhoto.getDataType());
        assertEquals(new Date().getTime(), storedFacePhoto.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
    }
}
