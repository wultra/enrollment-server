/*
 * Signer Cloud
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

import com.github.benmanes.caffeine.cache.Cache;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Microblink document verification provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class MicroblinkDocumentVerificationProviderTest {

    private static final String USER_ID = "fc87e60a-85fe-405c-bfa3-9580211e1670";
    private static final String ACTIVATION_ID = "da15f970-d939-46f0-abe7-7858e74ea3b0";

    private static final String DOCUMENT_FRONT_UPLOAD_ID = "52ca4d10-06ac-442c-934c-9d085ab18934";
    private static final String DOCUMENT_BACK_UPLOAD_ID = "bdfb45ce-a808-4b65-86a8-9f5f184c56f6";

    private OwnerId ownerId;
    private SubmittedDocument submittedDocumentFront;
    private SubmittedDocument submittedDocumentBack;

    @Mock
    private RestClient restClient;

    @Mock
    private Cache<UUID, SubmittedDocument> microblinkDocumentCache;

    @InjectMocks
    private MicroblinkDocumentVerificationProvider provider;

    @BeforeEach
    void setUp() {
        ownerId = new OwnerId();
        ownerId.setUserId(USER_ID);
        ownerId.setActivationId(ACTIVATION_ID);

        submittedDocumentFront = new SubmittedDocument();
        submittedDocumentFront.setDocumentId("document-front-side");
        submittedDocumentFront.setType(DocumentType.ID_CARD);
        submittedDocumentFront.setSide(CardSide.FRONT);
        submittedDocumentFront.setPhoto(
                Image.builder()
                        .filename("document_front.jpg")
                        .data(new byte[] {1, 2, 3})
                        .build()
        );

        submittedDocumentBack = new SubmittedDocument();
        submittedDocumentBack.setDocumentId("document-back-side");
        submittedDocumentBack.setType(DocumentType.ID_CARD);
        submittedDocumentBack.setSide(CardSide.BACK);
        submittedDocumentBack.setPhoto(
                Image.builder()
                        .filename("document_front.jpg")
                        .data(new byte[] {4, 5, 6})
                        .build()
        );
    }

    @Test
    void testInitVerificationSdk() {
        // given
        // -

        // when
        final var sdkInfo = provider.initVerificationSdk(ownerId, Collections.emptyMap());

        // then
        assertEquals(new VerificationSdkInfo(), sdkInfo);
    }

    @Test
    void testSubmitDocuments_emptyDocumentsLists_resultWithEmptyDocuments() {
        // given
        final List<SubmittedDocument> submittedDocuments = Collections.emptyList();

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertEquals(new DocumentsSubmitResult(), result);
    }

    @Test
    void testSubmitDocuments_emptyDocumentsLists_nothingIsStoredIntoCache() {
        // given
        final List<SubmittedDocument> submittedDocuments = Collections.emptyList();

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(microblinkDocumentCache, never()).put(any(), any());
    }

    @Test
    void testSubmitDocuments_documentWithBothSides_resultWithUploadedDocuments() {
        // given
        final var submittedDocuments = List.of(submittedDocumentFront, submittedDocumentBack);

        // when
        final var result = provider.submitDocuments(ownerId, submittedDocuments);

        // then
        assertDocumentSubmitResult(result.getResults(), "document-front-side");
        assertDocumentSubmitResult(result.getResults(), "document-back-side");
    }

    @Test
    void testSubmitDocuments_documentWithBothSides_documentsAreStoredIntoCache() {
        // given
        final var submittedDocuments = List.of(submittedDocumentFront, submittedDocumentBack);

        // when
        provider.submitDocuments(ownerId, submittedDocuments);

        // then
        verify(microblinkDocumentCache).put(any(UUID.class), eq(submittedDocumentFront));
        verify(microblinkDocumentCache).put(any(UUID.class), eq(submittedDocumentBack));
    }

    @Test
    void testVerifyDocuments_oneDocumentIsMissing_exceptionIsThrown() {
        // given
        final var cachedDocuments = Map.of(
                UUID.fromString(DOCUMENT_BACK_UPLOAD_ID), submittedDocumentBack
        );

        final var uploadIds = List.of(DOCUMENT_FRONT_UPLOAD_ID, DOCUMENT_BACK_UPLOAD_ID);
        final var uploadUuids = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        when(microblinkDocumentCache.getAllPresent(uploadUuids)).thenReturn(cachedDocuments);

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Document site FRONT is missing", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_restClientException_exceptionIsThrown() throws RestClientException {
        // given
        final var cachedDocuments = Map.of(
                UUID.fromString(DOCUMENT_BACK_UPLOAD_ID), submittedDocumentBack,
                UUID.fromString(DOCUMENT_FRONT_UPLOAD_ID), submittedDocumentFront
        );

        final var uploadIds = List.of(DOCUMENT_FRONT_UPLOAD_ID, DOCUMENT_BACK_UPLOAD_ID);
        final var uploadUuids = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        when(microblinkDocumentCache.getAllPresent(uploadUuids)).thenReturn(cachedDocuments);
        when(restClient.post("/api/v2/docver", buildMicroblinkRequest(), new ParameterizedTypeReference<DocumentVerificationResponse>() {}))
                .thenThrow(new RestClientException("Test exception", HttpStatus.SERVICE_UNAVAILABLE, "Test error body", null));

        // when
        final var exception = assertThrows(RemoteCommunicationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals(
                "Failed REST call to verify documents 52ca4d10-06ac-442c-934c-9d085ab18934,bdfb45ce-a808-4b65-86a8-9f5f184c56f6 in Microblink, statusCode=503 SERVICE_UNAVAILABLE, responseBody='Test error body', OwnerId(activationId=da15f970-d939-46f0-abe7-7858e74ea3b0, userId=fc87e60a-85fe-405c-bfa3-9580211e1670)",
                exception.getMessage()
        );
        assertNotNull(exception.getCause());
    }

    @Test
    void testVerifyDocuments_responseWithoutBody_exceptionIsThrown() throws RestClientException {
        // given
        final var cachedDocuments = Map.of(
                UUID.fromString(DOCUMENT_BACK_UPLOAD_ID), submittedDocumentBack,
                UUID.fromString(DOCUMENT_FRONT_UPLOAD_ID), submittedDocumentFront
        );

        final var uploadIds = List.of(DOCUMENT_FRONT_UPLOAD_ID, DOCUMENT_BACK_UPLOAD_ID);
        final var uploadUuids = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        when(microblinkDocumentCache.getAllPresent(uploadUuids)).thenReturn(cachedDocuments);
        when(restClient.post("/api/v2/docver", buildMicroblinkRequest(), new ParameterizedTypeReference<DocumentVerificationResponse>() {}))
                .thenReturn(ResponseEntity.ok().build());

        // when
        final var exception = assertThrows(DocumentVerificationException.class, () -> provider.verifyDocuments(ownerId, uploadIds));

        // then
        assertEquals("Response body is empty", exception.getMessage());
    }

    @Test
    void testVerifyDocuments_responseWithVerificationFail_nokResponse() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var cachedDocuments = Map.of(
                UUID.fromString(DOCUMENT_BACK_UPLOAD_ID), submittedDocumentBack,
                UUID.fromString(DOCUMENT_FRONT_UPLOAD_ID), submittedDocumentFront
        );

        final var uploadIds = List.of(DOCUMENT_FRONT_UPLOAD_ID, DOCUMENT_BACK_UPLOAD_ID);
        final var uploadUuids = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        when(microblinkDocumentCache.getAllPresent(uploadUuids)).thenReturn(cachedDocuments);

        final var microblinkResponse = buildMicroblinkResponse(CheckResult.FAIL);
        when(restClient.post("/api/v2/docver", buildMicroblinkRequest(), new ParameterizedTypeReference<DocumentVerificationResponse>() {}))
                .thenReturn(ResponseEntity.ok(microblinkResponse));

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertDocumentsVerificationResult(result, DocumentVerificationStatus.REJECTED);
    }

    @Test
    void testVerifyDocuments_responseWithVerificationSuccess_okResponse() throws RestClientException, RemoteCommunicationException, DocumentVerificationException {
        // given
        final var cachedDocuments = Map.of(
                UUID.fromString(DOCUMENT_BACK_UPLOAD_ID), submittedDocumentBack,
                UUID.fromString(DOCUMENT_FRONT_UPLOAD_ID), submittedDocumentFront
        );

        final var uploadIds = List.of(DOCUMENT_FRONT_UPLOAD_ID, DOCUMENT_BACK_UPLOAD_ID);
        final var uploadUuids = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        when(microblinkDocumentCache.getAllPresent(uploadUuids)).thenReturn(cachedDocuments);

        final var microblinkResponse = buildMicroblinkResponse(CheckResult.PASS);
        when(restClient.post("/api/v2/docver", buildMicroblinkRequest(), new ParameterizedTypeReference<DocumentVerificationResponse>() {}))
                .thenReturn(ResponseEntity.ok(microblinkResponse));

        // when
        final var result = provider.verifyDocuments(ownerId, uploadIds);

        // then
        assertDocumentsVerificationResult(result, DocumentVerificationStatus.ACCEPTED);
    }

    @Test
    void testCleanupDocuments() {
        // given
        final var uploadIds = List.of(DOCUMENT_FRONT_UPLOAD_ID, DOCUMENT_BACK_UPLOAD_ID);

        // when
        provider.cleanupDocuments(ownerId, uploadIds);

        // then
        final var expectedUuids = List.of(
                UUID.fromString(DOCUMENT_FRONT_UPLOAD_ID),
                UUID.fromString(DOCUMENT_BACK_UPLOAD_ID)
        );

        verify(microblinkDocumentCache).invalidateAll(expectedUuids);
    }

    private void assertDocumentSubmitResult(final List<DocumentSubmitResult> documents, final String documentId) {
        final var document = documents.stream()
                .filter(r -> r.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow();

        assertDoesNotThrow(() -> UUID.fromString(document.getUploadId()));
        assertNull(document.getRejectReason());
        assertNull(document.getValidationResult());
        assertNull(document.getErrorDetail());
        assertNull(document.getExtractedData());
    }

    private DocumentVerificationRequest buildMicroblinkRequest() {
        final var frontImageSource = new DocumentVerificationImageSource();
        frontImageSource.setBase64(
                Base64.getEncoder().encodeToString(submittedDocumentFront.getPhoto().getData())
        );

        final var backImageSource = new DocumentVerificationImageSource();
        backImageSource.setBase64(
                Base64.getEncoder().encodeToString(submittedDocumentBack.getPhoto().getData())
        );

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setUseCase(useCase);
        return request;
    }

    private DocumentVerificationResponse buildMicroblinkResponse(final CheckResult checkResult) {
        final var verification = new DetailedCheck();
        verification.setCertaintyLevel(CertaintyLevel.HIGH);
        verification.setRecommendedOutcome(RecommendedOutcome.REJECT);
        verification.result(checkResult);

        final var runtime = new RuntimeInformation();
        runtime.setTraceId("00-0ffe7a27e6129c701d980635456f220f-001de07a3723b393-01");

        final var response = new DocumentVerificationResponse();
        response.processingStatus(ProcessingStatus.COMPLETED);
        response.setVerification(verification);
        response.setRuntime(runtime);
        return response;
    }

    private void assertDocumentsVerificationResult(final DocumentsVerificationResult result, final DocumentVerificationStatus verificationStatus) {
        assertEquals("00-0ffe7a27e6129c701d980635456f220f-001de07a3723b393-01", result.getVerificationId());
        assertEquals(verificationStatus, result.getStatus());
    }
}
