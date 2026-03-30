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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentDataEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.SneakyThrows;
import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Microblink document verification provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class MicroblinkDocumentVerificationProviderTest {

    private static final Pattern MICROBLINK_RESPONSE_IMAGE_PATTERN = Pattern.compile(
            "\"images\"\\s*:\\s*\\[.*?\\]\\s*,?",
            Pattern.DOTALL
    );

    private static final String USER_ID = "fc87e60a-85fe-405c-bfa3-9580211e1670";
    private static final String ACTIVATION_ID = "da15f970-d939-46f0-abe7-7858e74ea3b0";

    private static final List<MicroblinkConfigProperties.SdkConfig> MOBILE_SDK_CONFIGS = List.of(
            new MicroblinkConfigProperties.SdkConfig("app1", "ios", "source1-ios-1"),
            new MicroblinkConfigProperties.SdkConfig("app1", "android", "source1-android-1"),
            new MicroblinkConfigProperties.SdkConfig("app2", "ios", "source2-ios-1")
    );

    private static final String DOCUMENT_ID_CARD_FRONT_ID = "id-card-front";
    private static final String DOCUMENT_ID_CARD_FRONT_UPLOAD_ID = "52ca4d10-06ac-442c-934c-9d085ab18934";
    private static final byte[] DOCUMENT_ID_CARD_FRONT_IMAGE_DATA = Base64.getDecoder().decode("ZHVtbXlfZnJvbnRfZG9jdW1lbnQ=");

    private static final String DOCUMENT_ID_CARD_BACK_ID = "id-card-back";
    private static final String DOCUMENT_ID_CARD_BACK_UPLOAD_ID = "bdfb45ce-a808-4b65-86a8-9f5f184c56f6";

    private static final String DOCUMENT_DRIVING_LICENSE_FRONT_ID = "driving-license-front";
    private static final String DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID = "c3e1f7b8-9d2e-4f6a-8b7c-5d4e3f2a1b0c";

    private static final String DOCUMENT_DRIVING_LICENSE_BACK_ID = "driving-license-back";
    private static final String DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID = "d4f2a1b0-c3e1-f7b8-9d2e-4f6a8b7c5d4e";

    private static final String FACE_PHOTO_ID = "c1a4f5e2-3b6d-4f8e-9a1b-2c3d4e5f6a7b";
    private static final byte[] FACE_PHOTO_DATA = Base64.getDecoder().decode("dGVzdF9mYWNlX2ltYWdlX2RhdGE=");

    private static final long TIMESTAMP_ASSERT_DELTA_MS = 1_000;

    private OwnerId ownerId;

    private MicroblinkDocumentVerificationProvider.DocumentVerificationData verificationDocumentCardIdFront;
    private MicroblinkDocumentVerificationProvider.DocumentVerificationData verificationDocumentCardIdBack;
    private MicroblinkDocumentVerificationProvider.DocumentVerificationData verificationDocumentDrivingLicenseFront;
    private MicroblinkDocumentVerificationProvider.DocumentVerificationData verificationDocumentDrivingLicenseBack;

    private SubmittedDocument submittedDocumentIdCardFront;
    private SubmittedDocument submittedDocumentIdCardBack;

    private DocumentVerificationResponse.Extraction idCardExtraction;

    @Mock
    private RestClient restClient;

    @Mock
    private DocumentDataRepository documentDataRepository;

    @Mock
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private MicroblinkConfigProperties microblinkConfigProperties;

    @Mock
    private MicroblinkExtractedDataParser microblinkExtractedDataParser;

    @Mock
    private AuditService auditService;

    private MicroblinkDocumentVerificationProvider provider;

    @Captor
    private ArgumentCaptor<List<DocumentDataEntity>> documentDataEntitiesCaptor;

    @Captor
    private ArgumentCaptor<List<ProcessedDocumentDataEntity>> processedDocumentDataEntityCaptor;

    @BeforeEach
    void setUp() {
        ownerId = new OwnerId();
        ownerId.setUserId(USER_ID);
        ownerId.setActivationId(ACTIVATION_ID);

        verificationDocumentCardIdFront = buildVerificationDataDocument(
                DOCUMENT_ID_CARD_FRONT_ID,
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DocumentType.ID_CARD,
                CardSide.FRONT,
                "document_front.jpg",
                new byte[] {1}
        );

        verificationDocumentCardIdBack = buildVerificationDataDocument(
                DOCUMENT_ID_CARD_BACK_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DocumentType.ID_CARD,
                CardSide.BACK,
                "document_back.jpg",
                new byte[] {2}
        );

        verificationDocumentDrivingLicenseFront = buildVerificationDataDocument(
                DOCUMENT_DRIVING_LICENSE_FRONT_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DocumentType.DRIVING_LICENSE,
                CardSide.FRONT,
                "driving_license_front.jpg",
                new byte[] {3}
        );

        verificationDocumentDrivingLicenseBack = buildVerificationDataDocument(
                DOCUMENT_DRIVING_LICENSE_BACK_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID,
                DocumentType.DRIVING_LICENSE,
                CardSide.BACK,
                "driving_license_back.jpg",
                new byte[] {4}
        );

        submittedDocumentIdCardFront = buildSubmittedDocument(verificationDocumentCardIdFront);
        submittedDocumentIdCardBack = buildSubmittedDocument(verificationDocumentCardIdBack);

        idCardExtraction = new DocumentVerificationResponse.Extraction(
                List.of(),
                new DocumentVerificationResponse.ExtractionClassInfo("Id", null));

        final var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        when(microblinkConfigProperties.getMobileSdkConfigs()).thenReturn(MOBILE_SDK_CONFIGS);

        provider = new MicroblinkDocumentVerificationProvider(
                restClient,
                objectMapper,
                microblinkConfigProperties,
                documentDataRepository,
                processedDocumentDataRepository,
                documentVerificationRepository,
                microblinkExtractedDataParser,
                auditService
        );
    }

    @ParameterizedTest
    @CsvSource({
            ",",
            "app1,",
            ",ios",
            "app2,android"
    })
    void testInitVerificationSdk_sdkConfigNotFound_responseWithoutLicenseKey(final String origin, final String platform) {
        // given
        final var initParams = new HashMap<String, String>();
        initParams.put("origin", origin);
        initParams.put("platform", platform);

        // when
        final var result = provider.initVerificationSdk(ownerId, Collections.unmodifiableMap(initParams));

        // then
        assertEquals(new VerificationSdkInfo(), result);
    }

    @ParameterizedTest
    @CsvSource({
            "app1,ios,source1-ios-1",
            "app1,android,source1-android-1",
            "app2,ios,source2-ios-1"
    })
    void testInitVerificationSdk_sdkConfigFound_responseWithLicenseKey(final String origin, final String platform, final String expectedLicenseKey) {
        // given
        final var initParams = Map.of("origin", origin, "platform", platform);

        // when
        final var result = provider.initVerificationSdk(ownerId, initParams);

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", expectedLicenseKey)), result);
    }

    @Test
    void testSubmitDocuments_multipleDocumentsOfSameTypeAndSide_exceptionIsThrown() {
        // given
        submittedDocumentIdCardBack.setSide(CardSide.FRONT);

        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.submitDocuments(ownerId, submittedDocuments));

        // then
        assertEquals("Multiple documents of type ID_CARD and side FRONT found. Document ids: [id-card-front, id-card-back]", exception.getMessage());
    }

    @Test
    void testSubmitDocuments_clientThrowsException_exceptionIsThrown() throws RestClientException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenThrow(new RestClientException("Test exception", HttpStatus.SERVICE_UNAVAILABLE, "Test error body", null));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var exception = assertThrows(RemoteCommunicationException.class, () -> provider.submitDocuments(ownerId, submittedDocuments));

        // then
        assertEquals("Failed REST API call to Microblink, statusCode=503 SERVICE_UNAVAILABLE, responseBody='Test error body'", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void testSubmitDocuments_clientResponseWithoutBody_exceptionIsThrown() throws RestClientException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok().build());

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.submitDocuments(ownerId, submittedDocuments));

        // then
        assertEquals("Response body is empty", exception.getMessage());
    }

    @Test
    void testSubmitDocuments_exceptionWhenParsingResponseBody_exceptionIsThrown() throws RestClientException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok("{ invalidJson, \"traceId\": \"123\" }"));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.submitDocuments(ownerId, submittedDocuments));

        // then
        assertEquals("Failed to parse Microblink API response. Microblink traceId: 123", exception.getMessage());
    }

    @Test
    void testSubmitDocuments_documentForFacePhotoNotProvided_responseWithoutFacePhotoId() throws RestClientException, DocumentVerificationException, RemoteCommunicationException {
        // given
        submittedDocumentIdCardFront.setType(DocumentType.UNKNOWN);
        submittedDocumentIdCardBack.setType(DocumentType.UNKNOWN);
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var response = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertNull(response.getExtractedPhotoId());
    }

    @Test
    void testSubmitDocuments_microblinkResponseWithoutPhoto_responseWithoutFacePhotoId() throws RestClientException, DocumentVerificationException, RemoteCommunicationException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var response = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertNull(response.getExtractedPhotoId());
    }

    @Test
    void testSubmitDocuments_facePhotoExtracted_responseWithFacePhotoId() throws RestClientException, DocumentVerificationException, RemoteCommunicationException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", buildFaceImageJson(), "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var response = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDoesNotThrow(() -> UUID.fromString(response.getExtractedPhotoId()));
    }

    @Test
    void testSubmitDocuments_facePhotoExtracted_photoIsSaved() throws RestClientException, DocumentVerificationException, RemoteCommunicationException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", buildFaceImageJson(), "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(processedDocumentDataRepository).saveAll(processedDocumentDataEntityCaptor.capture());

        final var storedProcessedData = processedDocumentDataEntityCaptor.getValue();
        assertEquals(1, storedProcessedData.size());
        assertStoredFaceImage(storedProcessedData.get(0));
    }

    @Test
    void testSubmitDocuments_microblinkRejectResponse_correctResponse() throws RestClientException, DocumentVerificationException, RemoteCommunicationException, JsonProcessingException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(
                CheckResult.FAIL,
                Type.ID,
                "[]",
                buildFaceImageJson(),
                buildMessage()
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkExtractedDataParser.parseExtractedData("[{\"front\":\"dummy\"}]", idCardExtraction)).thenReturn("[{\"front\":\"dummy\"}]");
        when(microblinkExtractedDataParser.parseExtractedData("[]", idCardExtraction)).thenReturn("[]");

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertResultForRejectResponse(result, responseJson);
    }

    @Test
    void testSubmitDocuments_microblinkPassResponse_correctResponse() throws RestClientException, DocumentVerificationException, RemoteCommunicationException, JsonProcessingException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.ID,
                "[]",
                buildFaceImageJson(),
                "[]"
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkExtractedDataParser.parseExtractedData("[{\"front\":\"dummy\"}]", idCardExtraction)).thenReturn("[{\"front\":\"dummy\"}]");
        when(microblinkExtractedDataParser.parseExtractedData("[]", idCardExtraction)).thenReturn("[]");

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertResultForPassResponse(result, responseJson);
    }

    @Test
    void testSubmitDocuments_successfulUpload_documentDataAreSaved() throws RestClientException, DocumentVerificationException, RemoteCommunicationException {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront, submittedDocumentIdCardBack);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.ID,
                "[]",
                buildFaceImageJson(),
                "[]"
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(documentDataRepository).saveAll(documentDataEntitiesCaptor.capture());
        assertSavedDocumentsData(documentDataEntitiesCaptor.getValue(), result);
    }

    @Test
    void testSubmitDocuments_documentVerificationEntityWithoutSide_firstDocumentVerificationIdForDocumentTypeIsSet() throws Exception {
        // given
        final var submittedDocuments = List.of(submittedDocumentIdCardFront);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                null
        );

        final var responseJson = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.ID,
                "[]",
                buildImagesJson(),
                "[]"
        );

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        when(microblinkConfigProperties.getRequestOptions()).thenReturn(buildRequestOptions());

        final var verificationDocumentCardIdBackWithoutSide = verificationDocumentCardIdBack.toBuilder()
                .side(null)
                .build();

        final var documentVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBackWithoutSide),
                Set.of());

        when(documentVerificationRepository.findAllByActivationIdByTypes(ACTIVATION_ID, Set.of(DocumentType.ID_CARD)))
                .thenReturn(documentVerifications);

        // when
        provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(processedDocumentDataRepository).saveAll(processedDocumentDataEntityCaptor.capture());
        assertSavedProcessedDocumentData(processedDocumentDataEntityCaptor.getValue());
    }

    @Test
    void testVerifyDocuments_missingAllVerificationData_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(List.of());

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("No document verification data found for uploadIds: [52ca4d10-06ac-442c-934c-9d085ab18934, bdfb45ce-a808-4b65-86a8-9f5f184c56f6]", error.getMessage());
    }

    @Test
    void testVerifyDocuments_missingOneDocumentVerificationData_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentResult = new DocumentResultEntity();
        documentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, buildExtractedDataJson("John"), "[]", "[]")
        );

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront), Set.of(documentResult));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("No document verification data found for uploadId: bdfb45ce-a808-4b65-86a8-9f5f184c56f6", error.getMessage());
    }

    @Test
    void testVerifyDocuments_missingDocumentResult_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront), Set.of());
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("No document result data found for uploadId: 52ca4d10-06ac-442c-934c-9d085ab18934", error.getMessage());
    }

    @Test
    void testVerifyDocuments_documentTypeDoesNotMatchClaimedOne_exceptionIsThrown() {
        // given
        when(microblinkConfigProperties.isExtractedDataCheckEnabled()).thenReturn(true);

        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentResult = new DocumentResultEntity();
        documentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.DL, "[]", "[]", "[]")
        );

        final var documentVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack),
                Set.of(documentResult));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Extracted document type DRIVING_LICENSE does not match claimed type ID_CARD", error.getMessage());
    }

    @Test
    void testVerifyDocuments_unsupportedDocumentType_exceptionIsThrown() {
        // given
        when(microblinkConfigProperties.isExtractedDataCheckEnabled()).thenReturn(true);

        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentResult = new DocumentResultEntity();
        documentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.CITIZENSHIP_CERTIFICATE, "[]", "[]", "[]")
        );

        final var documentVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack),
                Set.of(documentResult));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Unsupported extracted document type CitizenshipCertificate", error.getMessage());
    }

    @Test
    void testVerifyDocuments_missingExtractedValueForCrosscheck_exceptionIsThrown() {
        // given
        when(microblinkConfigProperties.isExtractedDataCheckEnabled()).thenReturn(true);

        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID);

        final var documentResult = new DocumentResultEntity();
        documentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", "[]", "[]")
        );

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront), Set.of(documentResult));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Field FirstName not found in extracted data", error.getMessage());
    }

    @Test
    void testVerifyDocuments_extractedDataCheckDisabled_exceptionIsThrown() throws DocumentVerificationException {
        // given
        when(microblinkConfigProperties.isExtractedDataCheckEnabled()).thenReturn(false);

        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var idCardDocumentResult = new DocumentResultEntity();
        idCardDocumentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.PASSPORT, buildExtractedDataJson("John"), "[]", "[]")
        );

        final var idCardVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack),
                Set.of(idCardDocumentResult)
        );
        final var drivingLicenseDocumentResult = new DocumentResultEntity();
        drivingLicenseDocumentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.DL, "[]", "[]", "[]")
        );

        final var drivingLicenseVerifications = buildDocumentVerifications(
                List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack),
                Set.of(drivingLicenseDocumentResult)
        );

        final var verifications = Stream.concat(idCardVerifications.stream(), drivingLicenseVerifications.stream())
                .toList();
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(verifications);

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertEquals(DocumentVerificationStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void testVerifyDocuments_crosscheckFails_exceptionIsThrown() {
        // given
        when(microblinkConfigProperties.isExtractedDataCheckEnabled()).thenReturn(true);

        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var idCardDocumentResult = new DocumentResultEntity();
        idCardDocumentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, buildExtractedDataJson("John"), "[]", "[]")
        );

        final var idCardVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack),
                Set.of(idCardDocumentResult)
        );
        final var drivingLicenseDocumentResult = new DocumentResultEntity();
        drivingLicenseDocumentResult.setVerificationResult(
                buildMicroblinkResponseJson(CheckResult.PASS, Type.DL, buildExtractedDataJson("Bob"), "[]", "[]")
        );

        final var drivingLicenseVerifications = buildDocumentVerifications(
                List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack),
                Set.of(drivingLicenseDocumentResult)
        );

        final var verifications = Stream.concat(idCardVerifications.stream(), drivingLicenseVerifications.stream())
                .toList();
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(verifications);

        // when
        final var error = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Crosscheck failed for field firstName", error.getMessage());
    }

    @Test
    void testVerifyDocuments_rejectValidationForOneDocument_correctResultIsReturned() throws RemoteCommunicationException, DocumentVerificationException, JsonProcessingException {
        // given
        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var idCardVerificationResult = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, buildExtractedDataJson("John"), "[]", "[]");

        final var idCardDocumentResult = new DocumentResultEntity();
        idCardDocumentResult.setVerificationResult(idCardVerificationResult);
        idCardDocumentResult.setExtractedData("{ \"type\": \"ID\" }");

        final var idCardVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack),
                Set.of(idCardDocumentResult)
        );

        final var drivingLicenseVerificationResult = buildMicroblinkResponseJson(CheckResult.FAIL, Type.DL, buildExtractedDataJson("John"), "[]", buildMessage());

        final var drivingLicenseDocumentResult = new DocumentResultEntity();
        drivingLicenseDocumentResult.setVerificationResult(drivingLicenseVerificationResult);
        drivingLicenseDocumentResult.setExtractedData("{ \"type\": \"DL\" }");

        final var drivingLicenseVerifications = buildDocumentVerifications(
                List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack),
                Set.of(drivingLicenseDocumentResult)
        );

        final var verifications = Stream.concat(idCardVerifications.stream(), drivingLicenseVerifications.stream())
                .toList();
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(verifications);

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertValidationResultReject(result, idCardVerificationResult, drivingLicenseVerificationResult);
    }

    @Test
    void testVerifyDocuments_successfulVerification_correctResultIsReturned() throws RemoteCommunicationException, DocumentVerificationException {
        // given
        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var idCardVerificationResult = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, buildExtractedDataJson("John"), "[]", "[]");

        final var idCardDocumentResult = new DocumentResultEntity();
        idCardDocumentResult.setVerificationResult(idCardVerificationResult);
        idCardDocumentResult.setExtractedData("{ \"type\": \"ID\" }");

        final var idCardVerifications = buildDocumentVerifications(
                List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack),
                Set.of(idCardDocumentResult)
        );

        final var drivingLicenseVerificationResult = buildMicroblinkResponseJson(CheckResult.PASS, Type.DL, buildExtractedDataJson("John"), "[]", "[]");

        final var drivingLicenseDocumentResult = new DocumentResultEntity();
        drivingLicenseDocumentResult.setVerificationResult(drivingLicenseVerificationResult);
        drivingLicenseDocumentResult.setExtractedData("{ \"type\": \"DL\" }");

        final var drivingLicenseVerifications = buildDocumentVerifications(
                List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack),
                Set.of(drivingLicenseDocumentResult)
        );

        final var verifications = Stream.concat(idCardVerifications.stream(), drivingLicenseVerifications.stream())
                .toList();
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(verifications);

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertValidationResultPass(result, idCardVerificationResult, drivingLicenseVerificationResult);
    }

    @Test
    void testCleanupDocuments_verificationDataDoesNotExists_exceptionIsNotThrown() {
        // given
        // -

        // when / then
        assertDoesNotThrow(() -> provider.cleanupDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID)));
    }

    @Test
    void testGetPhoto_photoDoesNotExist_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.getPhoto(FACE_PHOTO_ID));

        // then
        assertEquals("Photo with id c1a4f5e2-3b6d-4f8e-9a1b-2c3d4e5f6a7b not found", exception.getMessage());
    }

    @Test
    void testGetPhoto_photoExists_correctResponseIsReturned() throws DocumentVerificationException {
        // given
        final var facePhotoData = buildFacePhotoData();

        when(processedDocumentDataRepository.findById(FACE_PHOTO_ID)).thenReturn(Optional.of(facePhotoData));

        // when
        final var response = provider.getPhoto(FACE_PHOTO_ID);

        // then
        final var expectedResponse = Image.builder()
                .filename("FaceImage.jpg")
                .data(FACE_PHOTO_DATA)
                .build();

        assertEquals(expectedResponse, response);
    }

    @Test
    void testParseRejectionReasons_correctResponseIsReturned() {
        // given
        final var verificationResult = """
                    "verification": {
                        "certaintyLevel": "High",
                        "recommendedOutcome": "Reject",
                        "type": "DetailedCheck",
                        "result": "Fail",
                        "performedChecks": 10
                    }
                """;

        final var documentResult = new DocumentResultEntity();
        documentResult.setVerificationResult(verificationResult);

        // when
        final var result = provider.parseRejectionReasons(documentResult);

        // then
        assertArrayEquals(List.of(verificationResult).toArray(), result.toArray());
    }

    private DocumentVerificationRequest buildMicroblinkRequest(final byte[] documentFrontImageData, final byte[] documentBackImageData) {
        final var frontImageSource = new DocumentVerificationImageSource();
        frontImageSource.setBase64(
                Base64.getEncoder().encodeToString(documentFrontImageData)
        );

        final var backImageSource = Optional.ofNullable(documentBackImageData)
                .map(it -> {
                    final var image = new DocumentVerificationImageSource();
                    image.setBase64(Base64.getEncoder().encodeToString(it));
                    return image;
                })
                .orElse(null);

        final var options = new DocumentVerificationProcessingOptions();
        options.setReturnImageFormat(ImageFormat.JPG);
        options.setReturnFaceImage(true);
        options.setReturnFullDocumentImage(true);

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setOptions(options);
        request.setUseCase(useCase);
        return request;
    }

    private static String buildMessage() {
        return """
                [{
                  "code": "E004",
                  "message": "Test microblink message",
                  "status": "Error"
                }]
                """;
    }

    private static String buildMicroblinkResponseJson(
            final CheckResult checkResult,
            final Type extractedType,
            final String overallExtractionJson,
            final String imagesJson,
            final String messages
    ) {
        return """
                {
                    "verification": {
                        "result": "%s"
                    },
                    "extraction": {
                        "overall": %s,
                        "viz": {
                            "front": [
                                {
                                    "front": "dummy"
                                }
                            ],
                            "back": []
                        },
                        "classInfo": {
                            "type": "%s"
                        }
                    },
                    "runtime": {
                        "traceId": "00-0ffe7a27e6129c701d980635456f220f-001de07a3723b393-01"
                    },
                    "images": %s,
                    "messages": %s
                }
                """.formatted(checkResult, overallExtractionJson, extractedType, imagesJson, messages);
    }

    private static String buildExtractedDataJson(final String firstName) {
        return """
                [
                    {
                        "field": "FirstName",
                        "value": "%s"
                    },
                    {
                        "field": "LastName",
                        "value": "Doe"
                    },
                    {
                        "field": "DateOfBirth",
                        "day": 1,
                        "month": 2,
                        "year": 1990
                    }
                ]
                """.formatted(firstName);
    }

    private static String buildFaceImageJson() {
        return """
                [
                    {
                        "name": "FaceImage",
                        "base64": "dGVzdF9mYWNlX2ltYWdlX2RhdGE="
                    }
                ]
                """;
    }

    private static String buildImagesJson() {
        return """
                [
                    {
                        "name": "FaceImage",
                        "base64": "dGVzdF9mYWNlX2ltYWdlX2RhdGE="
                    },
                    {
                        "name": "FullDocumentFrontImage",
                        "base64": "ZHVtbXlfZnJvbnRfZG9jdW1lbnQ="
                    }
                ]
                """;
    }

    private void assertSavedDocumentsData(final List<DocumentDataEntity> storedEntities, final DocumentsSubmitResult submitResult) {
        final var documentResults = submitResult.getResults();

        assertEquals(documentResults.size(), storedEntities.size());

        final var documentIdToDocumentData = documentResults.stream()
                .collect(Collectors.toMap(
                        DocumentSubmitResult::getDocumentId,
                        i -> storedEntities.stream()
                                .filter(e -> Objects.equals(e.getId(), i.getUploadId()))
                                .findFirst()
                                .orElseThrow()
                ));

        final var idCardFrontDocumentData = documentIdToDocumentData.get(DOCUMENT_ID_CARD_FRONT_ID);
        assertDoesNotThrow(() -> UUID.fromString(idCardFrontDocumentData.getId()));
        assertArrayEquals(verificationDocumentCardIdFront.image().getData(), idCardFrontDocumentData.getData());
        assertEquals(new Date().getTime(), idCardFrontDocumentData.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);

        final var idCardBackDocumentData = documentIdToDocumentData.get(DOCUMENT_ID_CARD_BACK_ID);
        assertDoesNotThrow(() -> UUID.fromString(idCardBackDocumentData.getId()));
        assertArrayEquals(verificationDocumentCardIdBack.image().getData(), idCardBackDocumentData.getData());
        assertEquals(new Date().getTime(), idCardBackDocumentData.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
    }

    private static SubmittedDocument buildSubmittedDocument(
            final MicroblinkDocumentVerificationProvider.DocumentVerificationData documentVerificationData
    ) {
        final var document = new SubmittedDocument();
        document.setDocumentId(documentVerificationData.documentId());
        document.setType(documentVerificationData.type());
        document.setSide(documentVerificationData.side());
        document.setPhoto(
                Image.builder()
                        .filename(documentVerificationData.image().getFilename())
                        .data(Arrays.clone(documentVerificationData.image().getData()))
                        .build()
        );

        return document;
    }

    private static MicroblinkDocumentVerificationProvider.DocumentVerificationData buildVerificationDataDocument(
            final String documentId,
            final String uploadId,
            final DocumentType type,
            final CardSide side,
            final String imageFilename,
            final byte[] imageData
    ) {
        final var image = Image.builder()
                .filename(imageFilename)
                .data(imageData)
                .build();

        return MicroblinkDocumentVerificationProvider.DocumentVerificationData.builder()
                .documentId(documentId)
                .uploadId(uploadId)
                .type(type)
                .side(side)
                .image(image)
                .build();
    }

    private static void assertStoredFaceImage(final ProcessedDocumentDataEntity entity) {
        assertDoesNotThrow(() -> UUID.fromString(entity.getId()));
        assertArrayEquals(FACE_PHOTO_DATA, entity.getData());
        assertEquals(ProcessedDocumentDataType.FACE_IMAGE, entity.getDataType());
        assertEquals(new Date().getTime(), entity.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
    }

    private static ProcessedDocumentDataEntity buildFacePhotoData() {
        final var entity = new ProcessedDocumentDataEntity();
        entity.setId(FACE_PHOTO_ID);
        entity.setData(FACE_PHOTO_DATA);
        entity.setTimestampCreated(new Date());
        entity.setDataType(ProcessedDocumentDataType.FACE_IMAGE);

        return entity;
    }

    private static List<DocumentVerificationEntity> buildDocumentVerifications(
            final List<MicroblinkDocumentVerificationProvider.DocumentVerificationData> verificationDocuments,
            final Set<DocumentResultEntity> documentResults
    ) {
        final var entities = new ArrayList<DocumentVerificationEntity>();

        for (final var verificationDocument : verificationDocuments) {
            final var entity = new DocumentVerificationEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setType(verificationDocument.type());
            entity.setSide(verificationDocument.side());
            entity.setUploadId(verificationDocument.uploadId());
            entity.setPhotoId(FACE_PHOTO_ID);
            entity.setResults(documentResults);
            entity.setTimestampCreated(new Date());

            entities.add(entity);
        }

        return entities;
    }

    private void assertResultForRejectResponse(final DocumentsSubmitResult result, final String microblinkResponseJson) throws JsonProcessingException {
        assertEquals("Rejected documents: [id-card-front, id-card-back]", result.getRejectReason());
        assertNull(result.getErrorDetail());

        final var documentsResult = result.getResults();
        assertEquals(2, documentsResult.size());

        final var frontDocumentResult = documentsResult.stream()
                .filter(d -> DOCUMENT_ID_CARD_FRONT_ID.equals(d.getDocumentId()))
                .findFirst()
                .orElseThrow();

        final var expectedValidationResultJson = buildExpectedValidationResult(microblinkResponseJson);

        assertDoesNotThrow(() -> UUID.fromString(frontDocumentResult.getUploadId()));
        assertEquals("[Test microblink message]", frontDocumentResult.getRejectReason());
        assertJsonEquals(expectedValidationResultJson, frontDocumentResult.getValidationResult());
        assertNull(frontDocumentResult.getErrorDetail());
        assertEquals("[{\"front\":\"dummy\"}]", frontDocumentResult.getExtractedData());

        final var backDocumentResult = documentsResult.stream()
                .filter(d -> DOCUMENT_ID_CARD_BACK_ID.equals(d.getDocumentId()))
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(backDocumentResult.getUploadId()));
        assertEquals("[Test microblink message]", backDocumentResult.getRejectReason());
        assertJsonEquals(expectedValidationResultJson, backDocumentResult.getValidationResult());
        assertNull(backDocumentResult.getErrorDetail());
        assertEquals("[]", backDocumentResult.getExtractedData());
    }

    private static String buildExpectedValidationResult(final String json) throws JsonProcessingException {
        final var result = MICROBLINK_RESPONSE_IMAGE_PATTERN.matcher(json)
                .replaceAll("");

        return new ObjectMapper().readTree(result).toString();
    }

    private void assertResultForPassResponse(final DocumentsSubmitResult result, final String microblinkResponseJson) throws JsonProcessingException {
        assertNull(result.getRejectReason());
        assertNull(result.getErrorDetail());

        final var documentsResult = result.getResults();
        assertEquals(2, documentsResult.size());

        final var frontDocumentResult = documentsResult.stream()
                .filter(d -> DOCUMENT_ID_CARD_FRONT_ID.equals(d.getDocumentId()))
                .findFirst()
                .orElseThrow();

        final var expectedValidationResultJson = buildExpectedValidationResult(microblinkResponseJson);

        assertDoesNotThrow(() -> UUID.fromString(frontDocumentResult.getUploadId()));
        assertNull(frontDocumentResult.getRejectReason());
        assertJsonEquals(expectedValidationResultJson, frontDocumentResult.getValidationResult());
        assertNull(frontDocumentResult.getErrorDetail());
        assertEquals("[{\"front\":\"dummy\"}]", frontDocumentResult.getExtractedData());

        final var backDocumentResult = documentsResult.stream()
                .filter(d -> DOCUMENT_ID_CARD_BACK_ID.equals(d.getDocumentId()))
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(backDocumentResult.getUploadId()));
        assertNull(backDocumentResult.getRejectReason());
        assertJsonEquals(expectedValidationResultJson, backDocumentResult.getValidationResult());
        assertNull(backDocumentResult.getErrorDetail());
        assertEquals("[]", backDocumentResult.getExtractedData());
    }

    private void assertValidationResultReject(
            final DocumentsVerificationResult result,
            final String idCardValidationResult,
            final String drivingLicenseValidationResult
    ) {
        assertDoesNotThrow(() -> UUID.fromString(result.getVerificationId()));
        assertEquals(DocumentVerificationStatus.REJECTED, result.getStatus());
        assertEquals("Rejected document upload ids: [c3e1f7b8-9d2e-4f6a-8b7c-5d4e3f2a1b0c, d4f2a1b0-c3e1-f7b8-9d2e-4f6a8b7c5d4e]", result.getRejectReason());
        assertNull(result.getErrorDetail());

        final var documentsResult = result.getResults();
        assertEquals(4, documentsResult.size());

        assertDocumentValidationResult(documentsResult, DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, null, "{ \"type\": \"ID\" }", idCardValidationResult);
        assertDocumentValidationResult(documentsResult, DOCUMENT_ID_CARD_BACK_UPLOAD_ID, null, "{ \"type\": \"ID\" }", idCardValidationResult);
        assertDocumentValidationResult(documentsResult, DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID, "[Test microblink message]", "{ \"type\": \"DL\" }", drivingLicenseValidationResult);
        assertDocumentValidationResult(documentsResult, DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID, "[Test microblink message]", "{ \"type\": \"DL\" }", drivingLicenseValidationResult);
    }

    private void assertValidationResultPass(
            final DocumentsVerificationResult result,
            final String idCardValidationResult,
            final String drivingLicenseValidationResult
    ) {
        assertDoesNotThrow(() -> UUID.fromString(result.getVerificationId()));
        assertEquals(DocumentVerificationStatus.ACCEPTED, result.getStatus());
        assertNull(result.getRejectReason());
        assertNull(result.getErrorDetail());

        final var documentsResult = result.getResults();
        assertEquals(4, documentsResult.size());

        assertDocumentValidationResult(documentsResult, DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, null, "{ \"type\": \"ID\" }", idCardValidationResult);
        assertDocumentValidationResult(documentsResult, DOCUMENT_ID_CARD_BACK_UPLOAD_ID, null, "{ \"type\": \"ID\" }", idCardValidationResult);
        assertDocumentValidationResult(documentsResult, DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID, null, "{ \"type\": \"DL\" }", drivingLicenseValidationResult);
        assertDocumentValidationResult(documentsResult, DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID, null, "{ \"type\": \"DL\" }", drivingLicenseValidationResult);
    }

    private static void assertDocumentValidationResult(
            final List<DocumentVerificationResult> documentsResult,
            final String uploadId,
            final String expectedRejectReason,
            final String expectedExtractedData,
            final String expectedValidationResult
    ) {
        final var result = documentsResult.stream()
                .filter(r -> r.getUploadId().equals(uploadId))
                .findFirst()
                .orElseThrow();

        assertEquals(expectedRejectReason, result.getRejectReason());
        assertEquals(expectedValidationResult, result.getVerificationResult());
        assertNull(result.getErrorDetail());
        assertEquals(expectedExtractedData, result.getExtractedData());
    }

    private static void assertSavedProcessedDocumentData(final List<ProcessedDocumentDataEntity> entities) {
        assertEquals(2, entities.size());

        final var faceImage = entities.stream()
                .filter(it -> it.getDataType() == ProcessedDocumentDataType.FACE_IMAGE)
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(faceImage.getId()));
        assertArrayEquals(FACE_PHOTO_DATA, faceImage.getData());
        assertEquals(new Date().getTime(), faceImage.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
        assertDoesNotThrow(() -> UUID.fromString(faceImage.getDocumentVerificationId()));

        final var documentImage = entities.stream()
                .filter(it -> it.getDataType() == ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE)
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(documentImage.getId()));
        assertArrayEquals(DOCUMENT_ID_CARD_FRONT_IMAGE_DATA, documentImage.getData());
        assertEquals(new Date().getTime(), documentImage.getTimestampCreated().getTime(), TIMESTAMP_ASSERT_DELTA_MS);
        assertDoesNotThrow(() -> UUID.fromString(documentImage.getDocumentVerificationId()));
    }

    private static DocumentVerificationProcessingOptions buildRequestOptions() {
        final var options = new DocumentVerificationProcessingOptions();
        options.setReturnImageFormat(ImageFormat.JPG);
        options.setReturnFaceImage(true);
        options.setReturnFullDocumentImage(true);
        return options;
    }

    @SneakyThrows
    private static void assertJsonEquals(final String expected, final String actual) {
        JSONAssert.assertEquals(expected, actual, true);
    }
}
