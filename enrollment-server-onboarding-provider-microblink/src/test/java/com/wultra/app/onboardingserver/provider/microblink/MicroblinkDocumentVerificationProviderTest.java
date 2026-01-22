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
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponseParser;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.response.v3.GetActivationStatusResponse;
import com.wultra.security.powerauth.client.v3.PowerAuthClient;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

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

    private static final String USER_ID = "fc87e60a-85fe-405c-bfa3-9580211e1670";
    private static final String ACTIVATION_ID = "da15f970-d939-46f0-abe7-7858e74ea3b0";

    private static final String DOCUMENT_ID_CARD_FRONT_ID = "id-card-front";
    private static final String DOCUMENT_ID_CARD_FRONT_UPLOAD_ID = "52ca4d10-06ac-442c-934c-9d085ab18934";

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

    private MicroblinkVerificationData.Document verificationDocumentCardIdFront;
    private MicroblinkVerificationData.Document verificationDocumentCardIdBack;
    private MicroblinkVerificationData.Document verificationDocumentDrivingLicenseFront;
    private MicroblinkVerificationData.Document verificationDocumentDrivingLicenseBack;

    private SubmittedDocument submittedDocumentIdCardFront;
    private SubmittedDocument submittedDocumentIdCardBack;

    @Mock
    private RestClient restClient;

    @Mock
    private PowerAuthClient powerAuthClient;

    @Mock
    private DocumentDataRepository documentDataRepository;

    @Mock
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    private MicroblinkDocumentVerificationProvider provider;

    @Captor
    private ArgumentCaptor<List<DocumentDataEntity>> documentDataEntitiesCaptor;

    @Captor
    private ArgumentCaptor<ProcessedDocumentDataEntity> processedDocumentDataEntityCaptor;

    @Captor
    private ArgumentCaptor<List<DocumentVerificationEntity>> documentVerificationsEntityCaptor;

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

        final var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final var licenseKeys = Map.of(
                MicroblinkMobilePlatform.IOS, "ios-license-key",
                MicroblinkMobilePlatform.ANDROID, "android-license-key"
        );

        provider = new MicroblinkDocumentVerificationProvider(
                restClient,
                new DocumentVerificationResponseParser(objectMapper),
                licenseKeys,
                powerAuthClient,
                documentDataRepository,
                processedDocumentDataRepository,
                documentVerificationRepository
        );
    }

    @Test
    void testInitVerificationSdk_platformFetchFail_exceptionIsThrown() throws PowerAuthClientException {
        // given
        when(powerAuthClient.getActivationStatus(ACTIVATION_ID)).thenThrow(new PowerAuthClientException("Test exception"));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class, () -> provider.initVerificationSdk(ownerId, Map.of()));

        // then
        assertEquals("Error when fetching mobile platform", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void testInitVerificationSdk_unsupportedPlatformFetchedFromPowerAuth_resultWithoutLicenseKey() throws PowerAuthClientException, RemoteCommunicationException {
        // given
        final var response = new GetActivationStatusResponse();
        response.setPlatform("unsupported");

        when(powerAuthClient.getActivationStatus(ACTIVATION_ID)).thenReturn(response);

        // when
        final var result = provider.initVerificationSdk(ownerId, Map.of());

        // then
        assertEquals(new VerificationSdkInfo(), result);
    }

    @Test
    void testInitVerificationSdk_supportedPlatformFetchedFromPowerAuth_resultWithLicenseKey() throws PowerAuthClientException, RemoteCommunicationException {
        // given
        final var response = new GetActivationStatusResponse();
        response.setPlatform("android");

        when(powerAuthClient.getActivationStatus(ACTIVATION_ID)).thenReturn(response);

        // when
        final var result = provider.initVerificationSdk(ownerId, Map.of());

        // then
        assertEquals(new VerificationSdkInfo(Map.of("license-key", "android-license-key")), result);
    }

    @ParameterizedTest
    @CsvSource({
            "ios,ios-license-key",
            "android,android-license-key"
    })
    void testInitVerificationSdk_supportedMobilePlatformProvided_resultWithLicenseKey(final String platform, final String expectedLicense) throws RemoteCommunicationException {
        // given
        // -

        // when
        final var sdkInfo = provider.initVerificationSdk(ownerId, Map.of("platform", platform));

        // then
        final var expectedResult = new VerificationSdkInfo();
        expectedResult.getAttributes().put("license-key", expectedLicense);

        assertEquals(expectedResult, sdkInfo);
    }

    @Test
    void testSubmitDocuments_submitIsSuccessful_correctResponseIsReturned() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentsSubmitResult(result, List.of(DOCUMENT_ID_CARD_FRONT_ID, DOCUMENT_ID_CARD_BACK_ID));
    }

    @Test
    void testSubmitDocuments_submitIsSuccessful_documentsAreStoredInDatabase() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(documentDataRepository).saveAll(documentDataEntitiesCaptor.capture());
        assertStoredDocumentData(documentDataEntitiesCaptor.getValue(), result);
    }

    @Test
    void testVerifyDocuments_documentsDataNotFound_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        when(documentDataRepository.findAllById(uploadIds)).thenReturn(Collections.emptyList());

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("No document data found for uploadIds: [52ca4d10-06ac-442c-934c-9d085ab18934, bdfb45ce-a808-4b65-86a8-9f5f184c56f6]", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_multipleDocumentsWithSameTypeAndSide_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var secondVerificationDocumentCardIdFront = verificationDocumentCardIdFront.toBuilder()
                .uploadId("b7e5c9a4-3f2d-4a8e-9c6b-1d2f3e4a5b6c")
                .build();

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, secondVerificationDocumentCardIdFront));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, secondVerificationDocumentCardIdFront));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Multiple documents of type ID_CARD and side FRONT found. Document data ids: [52ca4d10-06ac-442c-934c-9d085ab18934, b7e5c9a4-3f2d-4a8e-9c6b-1d2f3e4a5b6c]", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_documentForFacePhotoNotProvided_exceptionIsThrown() {
        // given
        final var uploadIds = List.of(verificationDocumentCardIdFront.uploadId());
        final var documentWithoutFacePhoto = verificationDocumentCardIdFront.toBuilder()
                .type(DocumentType.UNKNOWN)
                .build();

        final var documentsData = buildDocumentsData(List.of(documentWithoutFacePhoto));
        final var documentVerifications = buildDocumentVerifications(List.of(documentWithoutFacePhoto));

        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("No document of preferred type for face photo extraction found", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_restClientException_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );
        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenThrow(new RestClientException("Test exception", HttpStatus.SERVICE_UNAVAILABLE, "Test error body", null));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Failed REST API call to Microblink, statusCode=503 SERVICE_UNAVAILABLE, responseBody='Test error body'", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_responseWithoutBody_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );
        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok().build());

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Response body is empty", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_extractedDocumentTypeDoesNotMatchClaimedOne_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));

        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));

        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.DL, "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Extracted document type DRIVING_LICENSE does not match claimed type ID_CARD", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_extractedDocumentTypeIsNotSupported_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.EMPLOYMENT_PASS, "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Unsupported extracted document type EmploymentPass", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_mandatoryFieldNotExtracted_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var apiResponse = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(apiResponse));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Field FirstName not found in extracted data", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_facePhotoNotExtracted_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));

        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var extractedDataJson = buildExtractedDataJson("John");
        final var apiResponse = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, extractedDataJson, "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(apiResponse));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Face image not extracted for face photo id: c1a4f5e2-3b6d-4f8e-9a1b-2c3d4e5f6a7b", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_facePhotoExtracted_photoIsStoredIntoDatabase() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);

        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var extractedDataJson = buildExtractedDataJson("John");
        final var apiResponse = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, extractedDataJson, buildImageJson());

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(apiResponse));

        // when
        provider.verifyDocuments(ownerId, uploadIds);

        // then
        verify(processedDocumentDataRepository).save(processedDocumentDataEntityCaptor.capture());
        assertStoredFaceImage(processedDocumentDataEntityCaptor.getValue());
    }

    @Test
    void testVerifyDocuments_documentsExtractedDataDoesNotMatch_exceptionIsThrown() throws RestClientException {
        // given
        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var documentVerifications = buildDocumentVerifications(List.of(
                verificationDocumentCardIdFront,
                verificationDocumentCardIdBack,
                verificationDocumentDrivingLicenseFront,
                verificationDocumentDrivingLicenseBack
        ));

        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(
                verificationDocumentCardIdFront,
                verificationDocumentCardIdBack,
                verificationDocumentDrivingLicenseFront,
                verificationDocumentDrivingLicenseBack
        ));

        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var idCardApiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var idCardApiResponse = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.ID,
                buildExtractedDataJson("John"),
                buildImageJson()
        );

        final var drivingLicenseApiRequest = buildMicroblinkRequest(
                verificationDocumentDrivingLicenseFront.image().getData(),
                verificationDocumentDrivingLicenseBack.image().getData()
        );

        final var drivingLicenseApiResponse = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.DL,
                buildExtractedDataJson("Dave"),
                "[]"
        );

        when(restClient.post("/api/v2/docver", idCardApiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(idCardApiResponse));
        when(restClient.post("/api/v2/docver", drivingLicenseApiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(drivingLicenseApiResponse));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Cross-check of extracted data failed on field FirstName", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_documentWithRejectResult_rejectResultIsReturned() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var documentVerifications = buildDocumentVerifications(List.of(
                verificationDocumentCardIdFront,
                verificationDocumentCardIdBack,
                verificationDocumentDrivingLicenseFront,
                verificationDocumentDrivingLicenseBack
        ));

        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var documentsData = buildDocumentsData(List.of(
                verificationDocumentCardIdFront,
                verificationDocumentCardIdBack,
                verificationDocumentDrivingLicenseFront,
                verificationDocumentDrivingLicenseBack
        ));

        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);

        final var idCardApiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var idCardApiResponse = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.ID,
                buildExtractedDataJson("John"),
                buildImageJson()
        );

        final var drivingLicenseApiRequest = buildMicroblinkRequest(
                verificationDocumentDrivingLicenseFront.image().getData(),
                verificationDocumentDrivingLicenseBack.image().getData()
        );

        final var drivingLicenseApiResponse = buildMicroblinkResponseJson(
                CheckResult.FAIL,
                Type.DL,
                buildExtractedDataJson("John"),
                "[]"
        );

        when(restClient.post("/api/v2/docver", idCardApiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(idCardApiResponse));
        when(restClient.post("/api/v2/docver", drivingLicenseApiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(drivingLicenseApiResponse));

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertEquals(DocumentVerificationStatus.REJECTED, result.getStatus());
    }

    @Test
    void testVerifyDocuments_allDocumentsPass_acceptResultIsReturned() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var uploadIds = List.of(
                DOCUMENT_ID_CARD_FRONT_UPLOAD_ID,
                DOCUMENT_ID_CARD_BACK_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID,
                DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID
        );

        final var documentsData = buildDocumentsData(List.of(
                verificationDocumentCardIdFront,
                verificationDocumentCardIdBack,
                verificationDocumentDrivingLicenseFront,
                verificationDocumentDrivingLicenseBack
        ));

        final var documentVerifications = buildDocumentVerifications(List.of(
                verificationDocumentCardIdFront,
                verificationDocumentCardIdBack,
                verificationDocumentDrivingLicenseFront,
                verificationDocumentDrivingLicenseBack
        ));

        when(documentDataRepository.findAllById(uploadIds)).thenReturn(documentsData);
        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        final var idCardApiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var idCardApiResponse = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.ID,
                buildExtractedDataJson("John"),
                buildImageJson()
        );

        final var drivingLicenseApiRequest = buildMicroblinkRequest(
                verificationDocumentDrivingLicenseFront.image().getData(),
                verificationDocumentDrivingLicenseBack.image().getData()
        );

        final var drivingLicenseApiResponse = buildMicroblinkResponseJson(
                CheckResult.PASS,
                Type.DL,
                buildExtractedDataJson("John"),
                "[]"
        );

        when(restClient.post("/api/v2/docver", idCardApiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(idCardApiResponse));
        when(restClient.post("/api/v2/docver", drivingLicenseApiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(drivingLicenseApiResponse));

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertEquals(DocumentVerificationStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void testCleanupDocuments_verificationDataDoesNotExists_exceptionIsNotThrown() {
        // given
        // -

        // when / then
        assertDoesNotThrow(() -> provider.cleanupDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID)));
    }

    @Test
    void testCleanupDocuments_verificationDataExists_requestedDocumentsAreCleared() {
        // given
        final var uploadIds = List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID);
        final var documentVerifications = buildDocumentVerifications(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));

        when(documentVerificationRepository.findAllByUploadIds(uploadIds)).thenReturn(documentVerifications);

        // when
        provider.cleanupDocuments(ownerId, uploadIds);

        // then
        verify(documentDataRepository).deleteAllById(uploadIds);
        verify(processedDocumentDataRepository).deleteAllById(Set.of(FACE_PHOTO_ID));
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

    private void assertDocumentsSubmitResult(final DocumentsSubmitResult result, List<String> expectedDocumentIds) {
        final var documentResults = result.getResults();
        assertEquals(expectedDocumentIds.size(), documentResults.size());

        expectedDocumentIds.forEach(documentId -> assertDocumentSubmitResult(documentResults, documentId));

        assertNull(result.getRejectReason());
        assertNull(result.getErrorDetail());
        assertDoesNotThrow(() -> UUID.fromString(result.getExtractedPhotoId()));
    }

    private static void assertDocumentSubmitResult(final List<DocumentSubmitResult> result, final String documentId) {
        final var document = result.stream()
                .filter(r -> r.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(document.getUploadId()));
        assertNull(document.getRejectReason());
        assertNull(document.getValidationResult());
        assertNull(document.getErrorDetail());
        assertEquals("{}", document.getExtractedData());
    }

    private DocumentVerificationRequest buildMicroblinkRequest(final byte[] documentFrontImageData, final byte[] documentBackImageData) {
        final var frontImageSource = new DocumentVerificationImageSource();
        frontImageSource.setBase64(
                Base64.getEncoder().encodeToString(documentFrontImageData)
        );

        final var backImageSource = new DocumentVerificationImageSource();
        backImageSource.setBase64(
                Base64.getEncoder().encodeToString(documentBackImageData)
        );

        final var options = new DocumentVerificationProcessingOptions();
        options.setReturnImageFormat(ImageFormat.JPG);
        options.setReturnFaceImage(true);

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setOptions(options);
        request.setUseCase(useCase);
        return request;
    }

    private static String buildMicroblinkResponseJson(final CheckResult checkResult, final Type extractedType, final String overallExtractionJson, final String imagesJson) {
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
                    "images": %s
                }
                """.formatted(checkResult, overallExtractionJson, extractedType, imagesJson);
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

    private static String buildImageJson() {
        return """
                [
                    {
                        "name": "FaceImage",
                        "base64": "dGVzdF9mYWNlX2ltYWdlX2RhdGE="
                    }
                ]
                """;
    }

    private void assertStoredDocumentData(final List<DocumentDataEntity> storedEntities, final DocumentsSubmitResult submitResult) {
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

    private static void assertDocument(
            final MicroblinkVerificationData.Document expectedDocument,
            final List<MicroblinkVerificationData.Document> documents
    ) {
        final var actualDocument = documents.stream()
                .filter(d -> d.documentId().equals(expectedDocument.documentId()))
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(actualDocument.uploadId()));
        assertEquals(expectedDocument.type(), actualDocument.type());
        assertEquals(expectedDocument.side(), actualDocument.side());
        assertEquals(expectedDocument.image(), actualDocument.image());
    }

    private static SubmittedDocument buildSubmittedDocument(
            MicroblinkVerificationData.Document verificationDocument
    ) {
        final var document = new SubmittedDocument();
        document.setDocumentId(verificationDocument.documentId());
        document.setType(verificationDocument.type());
        document.setSide(verificationDocument.side());
        document.setPhoto(
                Image.builder()
                        .filename(verificationDocument.image().getFilename())
                        .data(Arrays.clone(verificationDocument.image().getData()))
                        .build()
        );

        return document;
    }

    private static MicroblinkVerificationData.Document buildVerificationDataDocument(
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

        return MicroblinkVerificationData.Document.builder()
                .documentId(documentId)
                .uploadId(uploadId)
                .type(type)
                .side(side)
                .image(image)
                .build();
    }

    private static List<DocumentDataEntity> buildDocumentsData(final List<MicroblinkVerificationData.Document> verificationData) {
        final var documentsData = new ArrayList<DocumentDataEntity>();

        for (final var document : verificationData) {
            final var documentData = new DocumentDataEntity();
            documentData.setId(document.uploadId());
            documentData.setData(document.image().getData());
            documentData.setTimestampCreated(new Date());

            documentsData.add(documentData);
        }

        return documentsData;
    }

    private static void assertStoredFaceImage(final ProcessedDocumentDataEntity entity) {
        assertEquals(FACE_PHOTO_ID, entity.getId());
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

    private static List<DocumentVerificationEntity> buildDocumentVerifications(final List<MicroblinkVerificationData.Document> verificationDocuments) {
        final var entities = new ArrayList<DocumentVerificationEntity>();

        for (final var verificationDocument : verificationDocuments) {
            final var entity = new DocumentVerificationEntity();
            entity.setType(verificationDocument.type());
            entity.setSide(verificationDocument.side());
            entity.setUploadId(verificationDocument.uploadId());
            entity.setPhotoId(FACE_PHOTO_ID);

            entities.add(entity);
        }

        return entities;
    }
}
