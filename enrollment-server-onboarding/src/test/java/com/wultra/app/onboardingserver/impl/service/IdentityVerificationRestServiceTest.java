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

package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitV2Request;
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.security.powerauth.rest.api.spring.authentication.impl.PowerAuthApiAuthenticationImpl;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IdentityVerificationRestService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationRestServiceTest {

    private static final String ACTIVATION_ID = "6f9b5b09-3e72-4f34-9fe4-8a074ef45e94";
    private static final String USER_ID = "f0de3f4a-2b6c-4db7-834d-4eb7c0a1c82b";
    private static final String PROCESS_ID = "5ad34132-bc75-4322-9bf3-6d23844ec8c1";

    private static final String ID_CARD_FRONT_FILENAME = "id_card_front.jpg";
    private static final String ID_CARD_FRONT_ORIGINAL_ID = "e2c4a1ff-6fe2-4a3b-8e11-1c3d98b88b65";
    private static final String ID_CARD_FRONT_UPLOAD_ID = "97a8cbd9-3f02-4b4c-8f0d-9b3c2e358c43";

    private static final String ID_CARD_BACK_FILENAME = "id_card_back.jpg";
    private static final String ID_CARD_BACK_ORIGINAL_ID = "4df93751-12ad-4f67-ac88-b3bc4af02e2b";
    private static final String ID_CARD_BACK_UPLOAD_ID = "b0b3fae5-c4a4-4f52-8e66-4d6b12bfa5df";

    @Mock
    private IdentityVerificationConfig identityVerificationConfig;

    @Mock
    private OnboardingConfig onboardingConfig;

    @Mock
    private DocumentProcessingService documentProcessingService;

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private IdentityVerificationStatusService identityVerificationStatusService;

    @Mock
    private IdentityVerificationOtpService identityVerificationOtpService;

    @Mock
    private OnboardingServiceImpl onboardingService;

    @Mock
    private PresenceCheckService presenceCheckService;

    @Mock
    private StateMachineService stateMachineService;

    @Mock
    private DataExtractionService dataExtractionService;

    @InjectMocks
    private IdentityVerificationRestService tested;

    @Test
    void testSubmitDocuments_validRequest_identityVerificationServiceCalled() throws OnboardingProcessException, RemoteCommunicationException, IdentityVerificationLimitException, DocumentSubmitException, PowerAuthEncryptionException, IdentityVerificationException, OnboardingProcessLimitException, PowerAuthAuthenticationException, DocumentVerificationException {
        // given
        final var requestObject = buildRequestObject();
        final var encryptionContext = new EncryptionContext(null, ACTIVATION_ID, null, null, null);
        final var apiAuthentication = new PowerAuthApiAuthenticationImpl();

        final var onboardingProcessEntity = new OnboardingProcessEntity();
        onboardingProcessEntity.setActivationId(ACTIVATION_ID);
        onboardingProcessEntity.setUserId(USER_ID);

        when(onboardingService.findExistingProcessWithVerificationInProgress(ACTIVATION_ID)).thenReturn(onboardingProcessEntity);

        final var extractedIdCardFront = new Document();
        extractedIdCardFront.setFilename(ID_CARD_FRONT_FILENAME);
        extractedIdCardFront.setData(new byte[] { 1 });

        final var extractedIdCardBack = new Document();
        extractedIdCardBack.setFilename(ID_CARD_BACK_FILENAME);
        extractedIdCardBack.setData(new byte[] { 2 });

        when(dataExtractionService.extractDocuments(new byte[] { 1, 2 })).thenReturn(List.of(extractedIdCardFront, extractedIdCardBack));

        // when
        tested.submitDocuments(requestObject, encryptionContext, apiAuthentication);

        // then
        final var expectedRequest = buildDocumentSubmitV2Request();
        verify(identityVerificationService).submitDocuments(eq(expectedRequest), any(OwnerId.class));
    }

    private static ObjectRequest<DocumentSubmitRequest> buildRequestObject() {
        final var idCardFrontMetadata = new DocumentSubmitRequest.DocumentMetadata();
        idCardFrontMetadata.setOriginalDocumentId(ID_CARD_FRONT_ORIGINAL_ID);
        idCardFrontMetadata.setUploadId(ID_CARD_FRONT_UPLOAD_ID);
        idCardFrontMetadata.setType(DocumentType.ID_CARD);
        idCardFrontMetadata.setSide(CardSide.FRONT);
        idCardFrontMetadata.setFilename(ID_CARD_FRONT_FILENAME);

        final var idCardBackMetadata = new DocumentSubmitRequest.DocumentMetadata();
        idCardBackMetadata.setOriginalDocumentId(ID_CARD_BACK_ORIGINAL_ID);
        idCardBackMetadata.setUploadId(ID_CARD_BACK_UPLOAD_ID);
        idCardBackMetadata.setType(DocumentType.ID_CARD);
        idCardBackMetadata.setSide(CardSide.BACK);
        idCardBackMetadata.setFilename(ID_CARD_BACK_FILENAME);

        final var request = new DocumentSubmitRequest();
        request.setProcessId(PROCESS_ID);
        request.setResubmit(true);
        request.setDocuments(List.of(idCardFrontMetadata, idCardBackMetadata));
        request.setData(new byte[] { 1, 2 });

        return new ObjectRequest<>(request);
    }

    private static DocumentSubmitV2Request buildDocumentSubmitV2Request() {
        return DocumentSubmitV2Request.builder()
                .processId(PROCESS_ID)
                .resubmit(true)
                .documents(List.of(
                        DocumentSubmitV2Request.Document.builder()
                                .originalDocumentId(ID_CARD_FRONT_ORIGINAL_ID)
                                .uploadId(ID_CARD_FRONT_UPLOAD_ID)
                                .type(DocumentType.ID_CARD)
                                .side(CardSide.FRONT)
                                .filename(ID_CARD_FRONT_FILENAME)
                                .data(Base64.getEncoder().encodeToString(new byte[] { 1 }))
                                .build(),
                        DocumentSubmitV2Request.Document.builder()
                                .originalDocumentId(ID_CARD_BACK_ORIGINAL_ID)
                                .uploadId(ID_CARD_BACK_UPLOAD_ID)
                                .type(DocumentType.ID_CARD)
                                .side(CardSide.BACK)
                                .filename(ID_CARD_BACK_FILENAME)
                                .data(Base64.getEncoder().encodeToString(new byte[] { 2 }))
                                .build()
                ))
                .build();
    }
}