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
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
@EnableAutoConfiguration(exclude = org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class)
@ActiveProfiles("test")
class MicroblinkDocumentVerificationProviderIntTest {

    private static final String ACTIVATION_ID = "c5a2e6f1-8c9b-4b8f-9c41-4c9a8b5e213a";

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
    private CacheManager cacheManager;

    @Autowired
    private MicroblinkDocumentVerificationProvider microblinkDocumentVerificationProvider;

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
        cacheManager.getCache("microblinkPhotoCache").clear();
        cacheManager.getCache("microblinkDocumentsCache").clear();
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
    void testInitVerificationSdk_appleMobilePlatform_responseWithLicenseKey() throws RemoteCommunicationException {
        // given
        // -

        // when
        final var result = microblinkDocumentVerificationProvider.initVerificationSdk(ownerId, Map.of("platform", "apple"));

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", "dummy-apple-license-key")), result);
    }

    @Test
    void testSubmitDocuments_newItemInCacheCreated_correctResponseIsReturned() {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentsSubmitResult(result, List.of(idCardFrontDocument, idCardBackDocument));
    }

    @Test
    void testSubmitDocuments_newItemInCacheCreated_itemStoredInCache() {
        // given
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertCachedItem(result, List.of(idCardFrontDocument, idCardBackDocument));
    }

    @Test
    void testSubmitDocuments_itemInCacheIsUpdated_correctResponseIsReturned() {
        // given
        final var idCardFrontOld = idCardFrontDocument.toBuilder()
                        .image(Image.builder()
                                .data(new byte[] { 1, 2 })
                                .filename("id_card_front_old.jpg")
                                .build())
                .build();

        final var idCardBackOld = idCardBackDocument.toBuilder()
                        .image(Image.builder()
                                .data(new byte[] { 3, 4 })
                                .filename("id_card_back_old.jpg")
                                .build())
                .build();

        prepareVerificationDataInCache(List.of(idCardFrontOld, idCardBackOld));
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentsSubmitResult(result, List.of(idCardFrontDocument, idCardBackDocument));
    }

    @Test
    void testSubmitDocuments_itemInCacheIsUpdated_itemStoredInCache() {
        // given
        final var idCardFrontOld = idCardFrontDocument.toBuilder()
                .image(Image.builder()
                        .data(new byte[] { 1, 2 })
                        .filename("id_card_front_old.jpg")
                        .build())
                .build();

        final var idCardBackOld = idCardBackDocument.toBuilder()
                .image(Image.builder()
                        .data(new byte[] { 3, 4 })
                        .filename("id_card_back_old.jpg")
                        .build())
                .build();

        prepareVerificationDataInCache(List.of(idCardFrontOld, idCardBackOld));
        final var submittedDocuments = buildSubmittedDocuments(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        final var result = microblinkDocumentVerificationProvider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertCachedItem(result, List.of(idCardFrontDocument, idCardBackDocument));
    }

    @Test
    void testVerifyDocuments_verificationDataNotInCache_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(DocumentVerificationException.class,
                () -> microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID))
        );

        // then
        assertEquals("Verification data not found", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_microblinkServiceIsNotAvailable_exceptionIsThrown() {
        // given
        prepareVerificationDataInCache(List.of(idCardFrontDocument, idCardBackDocument));

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
        prepareVerificationDataInCache(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkRejectResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertVerificationResult(result, DocumentVerificationStatus.REJECTED, verificationRejectJson);
    }

    @Test
    void testVerifyDocuments_microblinkReturnsRejectResult_facePhotoIsStoredInCache() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInCache(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkRejectResponseBody));

        // when
        microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        final var cachedPhoto = cacheManager.getCache("microblinkPhotoCache").get(ID_CARD_FACE_PHOTO_ID, String.class);
        assertEquals(idCardFacePhotoBase64, cachedPhoto);
    }

    @Test
    void testVerifyDocuments_microblinkReturnsPassResult_correctResponseIsReturned() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInCache(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkPassResponseBody));

        // when
        microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        final var cachedPhoto = cacheManager.getCache("microblinkPhotoCache").get(ID_CARD_FACE_PHOTO_ID, String.class);
        assertEquals(idCardFacePhotoBase64, cachedPhoto);
    }

    @Test
    void testVerifyDocuments_microblinkReturnsPassResult_facePhotoIsStoredInCache() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        prepareVerificationDataInCache(List.of(idCardFrontDocument, idCardBackDocument));

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(microblinkPassResponseBody));

        // when
        final var result = microblinkDocumentVerificationProvider.verifyDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        assertVerificationResult(result, DocumentVerificationStatus.ACCEPTED, verificationPassJson);
    }

    @Test
    void testGetPhoto_photoNotInCache_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> microblinkDocumentVerificationProvider.getPhoto(ID_CARD_FACE_PHOTO_ID));

        // then
        assertEquals("Photo with id 2a1c3e4f-5b6d-7e8f-9012-3456789abcde not found", exception.getMessage());
    }

    @Test
    void testGetPhoto_photoInCache_correctImageIsReturned() throws DocumentVerificationException {
        // given
        preparePhotoInCache();

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
    void testCleanupDocuments_verificationDataNotInCache_noExceptionIsThrown() {
        // given
        // -

        // when, then
        assertDoesNotThrow(
                () -> microblinkDocumentVerificationProvider.cleanupDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID))
        );
    }

    @Test
    void testCleanupDocuments_verificationDataInCache_documentsAreRemovedFromCache() {
        // given
        prepareVerificationDataInCache(List.of(idCardFrontDocument, idCardBackDocument));

        // when
        microblinkDocumentVerificationProvider.cleanupDocuments(ownerId, List.of(ID_CARD_FRONT_UPLOAD_ID, ID_CARD_BACK_UPLOAD_ID));

        // then
        final var expectedCachedItem = MicroblinkVerificationData.builder()
                .documents(Collections.emptyList())
                .facePhotoId(ID_CARD_FACE_PHOTO_ID)
                .build();

        final var actualCachedItem =  cacheManager.getCache("microblinkDocumentsCache")
                .get(ownerId.getActivationId(), MicroblinkVerificationData.class);

        assertEquals(expectedCachedItem, actualCachedItem);
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
            assertNull(actualDocument.getExtractedData());
        }
    }

    private void assertCachedItem(final DocumentsSubmitResult result, final List<MicroblinkVerificationData.Document> expectedDocuments) {
        final var uploadIdByDocumentId = result.getResults().stream()
                .collect(Collectors.toMap(DocumentSubmitResult::getDocumentId, DocumentSubmitResult::getUploadId));

        final var documents = expectedDocuments.stream()
                .map(i -> i.toBuilder().uploadId(uploadIdByDocumentId.get(i.documentId())).build())
                .toList();

        final var expectedCachedItem = MicroblinkVerificationData.builder()
                .documents(documents)
                .facePhotoId(result.getExtractedPhotoId())
                .build();

        final var actualCachedItem = cacheManager.getCache("microblinkDocumentsCache")
                .get(ownerId.getActivationId(), MicroblinkVerificationData.class);

        assertEquals(expectedCachedItem, actualCachedItem);
    }

    private void prepareVerificationDataInCache(final List<MicroblinkVerificationData.Document> documents) {
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(documents)
                .facePhotoId(ID_CARD_FACE_PHOTO_ID)
                .build();

        final var cache = cacheManager.getCache("microblinkDocumentsCache");
        cache.put(ownerId.getActivationId(), verificationData);
    }

    private void preparePhotoInCache() {
        final var cache = cacheManager.getCache("microblinkPhotoCache");
        cache.put(ID_CARD_FACE_PHOTO_ID, idCardFacePhotoBase64);
    }

    private void assertVerificationResult(DocumentsVerificationResult result, final DocumentVerificationStatus status, final String verificationJson) {
        assertNull(result.getVerificationId());
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
}
