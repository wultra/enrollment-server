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
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
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

    private OwnerId ownerId;

    private MicroblinkVerificationData.Document verificationDocumentCardIdFront;
    private MicroblinkVerificationData.Document verificationDocumentCardIdBack;
    private MicroblinkVerificationData.Document verificationDocumentDrivingLicenseFront;
    private MicroblinkVerificationData.Document verificationDocumentDrivingLicenseBack;

    private SubmittedDocument submittedDocumentIdCardFront;
    private SubmittedDocument submittedDocumentIdCardBack;

    @Mock
    private Cache verificationDataCache;

    @Mock
    private Cache photoCache;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RestClient restClient;

    @Mock
    private PowerAuthClient powerAuthClient;

    private MicroblinkDocumentVerificationProvider provider;

    @Captor
    private ArgumentCaptor<MicroblinkVerificationData> verificationDataCaptor;

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

        when(cacheManager.getCache("microblinkDocumentsCache")).thenReturn(verificationDataCache);
        when(cacheManager.getCache("microblinkPhotoCache")).thenReturn(photoCache);

        final var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final var licenseKeys = Map.of(
                MicroblinkMobilePlatform.APPLE, "apple-license-key",
                MicroblinkMobilePlatform.ANDROID, "android-license-key"
        );

        provider = new MicroblinkDocumentVerificationProvider(
                cacheManager,
                restClient,
                new DocumentVerificationResponseParser(objectMapper),
                licenseKeys,
                powerAuthClient
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
            "apple,apple-license-key",
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
    void testSubmitDocuments_verificationDataIsCreated_correctResponseIsReturned() {
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
    void testSubmitDocuments_verificationDataIsCreated_documentsAreStoredIntoCache() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        // when
        provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(verificationDataCache).put(eq(ACTIVATION_ID), verificationDataCaptor.capture());
        assertVerificationDataAfterSubmit(verificationDataCaptor.getValue(), List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
    }

    @Test
    void testSubmitDocuments_documentsAreAdded_correctResponseIsReturned() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        final var verificationData = MicroblinkVerificationData.builder()
                .facePhotoId(FACE_PHOTO_ID)
                .documents(List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack))
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentsSubmitResult(result, List.of(DOCUMENT_ID_CARD_FRONT_ID, DOCUMENT_ID_CARD_BACK_ID));
    }

    @Test
    void testSubmitDocuments_documentsAreAdded_documentsAreStoredIntoCache() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        final var verificationData = MicroblinkVerificationData.builder()
                .facePhotoId(FACE_PHOTO_ID)
                .documents(List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack))
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(verificationDataCache).put(eq(ACTIVATION_ID), verificationDataCaptor.capture());
        assertVerificationDataAfterSubmit(
                verificationDataCaptor.getValue(),
                List.of(verificationDocumentDrivingLicenseFront, verificationDocumentDrivingLicenseBack, verificationDocumentCardIdFront, verificationDocumentCardIdBack)
        );
    }

    @Test
    void testSubmitDocuments_documentsAreUpdated_correctResponseIsReturned() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        final var verificationData = MicroblinkVerificationData.builder()
                .facePhotoId(FACE_PHOTO_ID)
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentsSubmitResult(result, List.of(DOCUMENT_ID_CARD_FRONT_ID, DOCUMENT_ID_CARD_BACK_ID));
    }

    @Test
    void testSubmitDocuments_documentsAreUpdated_documentsAreStoredIntoCache() {
        // given
        final var submittedDocuments = List.of(
                submittedDocumentIdCardFront,
                submittedDocumentIdCardBack
        );

        final var verificationData = MicroblinkVerificationData.builder()
                .facePhotoId(FACE_PHOTO_ID)
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(verificationDataCache).put(eq(ACTIVATION_ID), verificationDataCaptor.capture());
        assertVerificationDataAfterSubmit(verificationDataCaptor.getValue(), List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack));
    }

    @Test
    void testVerifyDocuments_verificationDataNotFoundInCache_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of()));

        // then
        assertEquals("Verification data not found", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_verificationDataWithoutDocuments_exceptionIsThrown() {
        // given
        final var verificationData = new MicroblinkVerificationData(Collections.emptyList(), FACE_PHOTO_ID);

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of()));

        // then
        assertEquals("Verification data without documents", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_documentWithUploadIdIsMissing_exceptionIsThrown() {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of("missingUploadId")));

        // then
        assertEquals("Documents with uploadIds missingUploadId not found", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_multipleDocumentsWithSameTypeAndSide_exceptionIsThrown() {
        // given
        final var verificationDocumentCardIdFrontDuplicate = verificationDocumentCardIdFront.toBuilder()
                .documentId("duplicated-id-card-front")
                .uploadId(UUID.randomUUID().toString())
                .build();

        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdFrontDuplicate))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, verificationDocumentCardIdFrontDuplicate.uploadId())));

        // then
        assertEquals("Multiple documents of type ID_CARD and side FRONT found. Document ids: id-card-front,duplicated-id-card-front", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_documentForFacePhotoNotProvided_exceptionIsThrown() {
        // given
        final var documentWithoutFacePhoto = verificationDocumentCardIdFront.toBuilder()
                .type(DocumentType.UNKNOWN)
                .build();
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(documentWithoutFacePhoto))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(documentWithoutFacePhoto.uploadId())));

        // then
        assertEquals("No document of preferred type for face photo extraction found", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_restClientException_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );
        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenThrow(new RestClientException("Test exception", HttpStatus.SERVICE_UNAVAILABLE, "Test error body", null));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID)));

        // then
        assertEquals("Failed REST API call to Microblink, statusCode=503 SERVICE_UNAVAILABLE, responseBody='Test error body'", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_responseWithoutBody_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );
        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok().build());

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID)));

        // then
        assertEquals("Response body is empty", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_extractedDocumentTypeDoesNotMatchClaimedOne_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.DL, "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID)));

        // then
        assertEquals("Extracted document type DRIVING_LICENSE does not match claimed type ID_CARD", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_extractedDocumentTypeIsNotSupported_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var responseJson = buildMicroblinkResponseJson(CheckResult.PASS, Type.EMPLOYMENT_PASS, "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(responseJson));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID)));

        // then
        assertEquals("Unsupported extracted document type EmploymentPass", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_mandatoryFieldNotExtracted_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var apiResponse = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, "[]", "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(apiResponse));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID)));

        // then
        assertEquals("Field FirstName not found in extracted data", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_facePhotoNotExtracted_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var extractedDataJson = buildExtractedDataJson("John");
        final var apiResponse = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, extractedDataJson, "[]");

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(apiResponse));

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID)));

        // then
        assertEquals("Face image not extracted from document of type ID_CARD", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_facePhotoExtracted_photoIsStoredIntoCache() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        final var apiRequest = buildMicroblinkRequest(
                verificationDocumentCardIdFront.image().getData(),
                verificationDocumentCardIdBack.image().getData()
        );

        final var extractedDataJson = buildExtractedDataJson("John");
        final var apiResponse = buildMicroblinkResponseJson(CheckResult.PASS, Type.ID, extractedDataJson, buildImageJson());

        when(restClient.post("/api/v2/docver", apiRequest, new ParameterizedTypeReference<String>() {}))
                .thenReturn(ResponseEntity.ok(apiResponse));

        // when
        provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID));

        // then
        verify(photoCache).put(FACE_PHOTO_ID, "dGVzdF9mYWNlX2ltYWdlX2RhdGE=");
    }

    @Test
    void testVerifyDocuments_documentsExtractedDataDoesNotMatch_exceptionIsThrown() throws RestClientException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(
                        List.of(
                                verificationDocumentCardIdFront,
                                verificationDocumentCardIdBack,
                                verificationDocumentDrivingLicenseFront,
                                verificationDocumentDrivingLicenseBack
                        ))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

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
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID, DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID, DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID)));

        // then
        assertEquals("Cross-check of extracted data failed on field FirstName", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_documentWithRejectResult_rejectResultIsReturned() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(
                        List.of(
                                verificationDocumentCardIdFront,
                                verificationDocumentCardIdBack,
                                verificationDocumentDrivingLicenseFront,
                                verificationDocumentDrivingLicenseBack
                        ))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

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
        final var result = provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID, DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID, DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID));

        // then
        assertEquals(DocumentVerificationStatus.REJECTED, result.getStatus());
    }

    @Test
    void testVerifyDocuments_allDocumentsPass_acceptResultIsReturned() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var verificationData = MicroblinkVerificationData.builder()
                .documents(
                        List.of(
                                verificationDocumentCardIdFront,
                                verificationDocumentCardIdBack,
                                verificationDocumentDrivingLicenseFront,
                                verificationDocumentDrivingLicenseBack
                        ))
                .facePhotoId(FACE_PHOTO_ID)
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

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
        final var result = provider.verifyDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID, DOCUMENT_ID_CARD_BACK_UPLOAD_ID, DOCUMENT_DRIVING_LICENSE_FRONT_UPLOAD_ID, DOCUMENT_DRIVING_LICENSE_BACK_UPLOAD_ID));

        // then
        assertEquals(DocumentVerificationStatus.ACCEPTED, result.getStatus());
    }

    // - only requested uploadIds are processed

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
        final var verificationData = MicroblinkVerificationData.builder()
                .facePhotoId(FACE_PHOTO_ID)
                .documents(List.of(verificationDocumentCardIdFront, verificationDocumentCardIdBack))
                .build();

        when(verificationDataCache.get(ACTIVATION_ID, MicroblinkVerificationData.class)).thenReturn(verificationData);

        // when
        provider.cleanupDocuments(ownerId, List.of(DOCUMENT_ID_CARD_FRONT_UPLOAD_ID));

        // then
        final var updatedVerificationData = MicroblinkVerificationData.builder()
                .facePhotoId(FACE_PHOTO_ID)
                .documents(List.of(verificationDocumentCardIdBack))
                .build();

        verify(verificationDataCache).put(ACTIVATION_ID, updatedVerificationData);
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
        when(photoCache.get(FACE_PHOTO_ID, String.class)).thenReturn("dGVzdC1waG90bw==");

        // when
        final var response = provider.getPhoto(FACE_PHOTO_ID);

        // then
        final var expectedResponse = Image.builder()
                .filename("FaceImage.jpg")
                .data(Base64.getDecoder().decode("dGVzdC1waG90bw=="))
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
        assertNull(document.getExtractedData());
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

    private void assertVerificationDataAfterSubmit(final MicroblinkVerificationData verificationData, List<MicroblinkVerificationData.Document> expectedDocuments) {
        assertDoesNotThrow(() -> UUID.fromString(verificationData.facePhotoId()));

        final var documents = verificationData.documents();
        assertEquals(expectedDocuments.size(), documents.size());

        expectedDocuments.forEach(expectedDocument -> assertDocument(expectedDocument, documents));
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
}
