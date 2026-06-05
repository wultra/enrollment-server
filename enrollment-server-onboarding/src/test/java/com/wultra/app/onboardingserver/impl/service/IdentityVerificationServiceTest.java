/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ErrorDetail;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationProcessingService;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesCheck;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.OTP_VERIFICATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link IdentityVerificationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {

    @Mock
    private IdentityVerificationRepository identityVerificationRepository;

    @Mock
    private IdentityVerificationPrecompleteCheck identityVerificationPrecompleteCheck;

    @Mock
    private IdentityVerificationConfig identityVerificationConfig;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private DocumentProcessingService documentProcessingService;

    @Mock
    private DocumentVerificationProvider documentVerificationProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private VerificationProcessingService verificationProcessingService;

    @Mock
    private RequiredDocumentTypesCheck requiredDocumentTypesCheck;

    @Mock
    private CommonOnboardingService processService;

    @Mock
    private OnboardingProcessLimitService processLimitService;

    @InjectMocks
    private IdentityVerificationService tested;

    @Test
    void testProcessVerificationResult_valid() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        when(identityVerificationPrecompleteCheck.evaluate(idVerification))
                .thenReturn(IdentityVerificationPrecompleteCheck.Result.successful());

        final var result = tested.processVerificationResult(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.FinalVerificationResult.OK));
    }

    @Test
    void testProcessVerificationResult_invalidPrecompleteGuard() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        when(identityVerificationPrecompleteCheck.evaluate(idVerification))
                .thenReturn(IdentityVerificationPrecompleteCheck.Result.failed("Not valid OTP"));

        final var result = tested.processVerificationResult(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.FinalVerificationResult.FAILED));
    }

    @Test
    void testCreateDocsMetadata_hideRejectedErrorDetail() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setErrorDetail("Hide specific error occurred.");

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertHidden(errors);
    }

    @Test
    void testCreateDocsMetadata_hideRejectedRejectReason() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setRejectReason("Hide specific rejection reason.");

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertHidden(errors);
    }

    @Test
    void testCreateDocsMetadata_hideFailedErrorDetail() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.FAILED);
        doc.setErrorDetail("Hide some error occurred.");

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertHidden(errors);
    }

    @Test
    void testCreateDocsMetadata_accepted() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.ACCEPTED);

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertTrue(CollectionUtils.isEmpty(errors));
    }

    private static void assertHidden(final List<String> errors) {
        assertEquals(1, errors.size());
        assertEquals("Error verifying the document.", errors.get(0));
    }

    @Test
    void testStartDocumentVerification_requiredDocumentsMetWithFailedDocument() throws Exception {
        final IdentityVerificationEntity idVerification = identityVerification();
        final List<DocumentVerificationEntity> batch = new ArrayList<>(List.of(
                document(DocumentStatus.FAILED),
                document(DocumentStatus.ACCEPTED),
                document(DocumentStatus.ACCEPTED)));

        mockStartDocumentVerification(idVerification, batch, true);
        when(processService.findProcess(idVerification.getProcessId())).thenReturn(new OnboardingProcessEntity());

        final var result = tested.startDocumentVerification(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.VerificationDocumentActionResult.REQUIRED_DOCUMENTS_VERIFIED));
        verify(processLimitService, times(1)).incrementErrorScore(any(), eq(OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED), any());
        verify(processLimitService, never()).incrementErrorScore(any(), eq(OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_REJECTED), any());
        verify(processLimitService, times(1)).checkOnboardingProcessErrorLimits(any());
        assertThat("identity verification must not be marked as errored when proceeding", idVerification.getErrorDetail(), nullValue());
    }

    @Test
    void testStartDocumentVerification_requiredDocumentsNotMetWithFailedDocument() throws Exception {
        final IdentityVerificationEntity idVerification = identityVerification();
        final List<DocumentVerificationEntity> batch = new ArrayList<>(List.of(
                document(DocumentStatus.FAILED),
                document(DocumentStatus.ACCEPTED)));

        mockStartDocumentVerification(idVerification, batch, false);
        when(processService.findProcess(idVerification.getProcessId())).thenReturn(new OnboardingProcessEntity());

        final var result = tested.startDocumentVerification(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.VerificationDocumentActionResult.INSUFFICIENT_DOCUMENT_COUNT));
        verify(processLimitService, times(1)).incrementErrorScore(any(), eq(OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED), any());
        verify(processLimitService, times(1)).checkOnboardingProcessErrorLimits(any());
        assertThat(idVerification.getErrorDetail(), equalTo(ErrorDetail.DOCUMENT_VERIFICATION_FAILED));
    }

    @Test
    void testStartDocumentVerification_twoFailedDocumentsCountedSeparately() throws Exception {
        final IdentityVerificationEntity idVerification = identityVerification();
        final List<DocumentVerificationEntity> batch = new ArrayList<>(List.of(
                document(DocumentStatus.FAILED),
                document(DocumentStatus.FAILED)));

        mockStartDocumentVerification(idVerification, batch, false);
        when(processService.findProcess(idVerification.getProcessId())).thenReturn(new OnboardingProcessEntity());

        tested.startDocumentVerification(new OwnerId(), idVerification);

        verify(processLimitService, times(2)).incrementErrorScore(any(), eq(OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED), any());
        verify(processLimitService, times(1)).checkOnboardingProcessErrorLimits(any());
    }

    @Test
    void testStartDocumentVerification_allAcceptedRequiredDocumentsNotMet() throws Exception {
        final IdentityVerificationEntity idVerification = identityVerification();
        final List<DocumentVerificationEntity> batch = new ArrayList<>(List.of(
                document(DocumentStatus.ACCEPTED)));

        mockStartDocumentVerification(idVerification, batch, false);

        final var result = tested.startDocumentVerification(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.VerificationDocumentActionResult.INSUFFICIENT_DOCUMENT_COUNT));
        verify(processLimitService, never()).incrementErrorScore(any(), any(), any());
        verify(processLimitService, never()).checkOnboardingProcessErrorLimits(any());
    }

    @Test
    void testStartDocumentVerification_requiredDocumentsMetWithoutFailures() throws Exception {
        final IdentityVerificationEntity idVerification = identityVerification();
        final List<DocumentVerificationEntity> batch = new ArrayList<>(List.of(
                document(DocumentStatus.ACCEPTED),
                document(DocumentStatus.ACCEPTED)));

        mockStartDocumentVerification(idVerification, batch, true);

        final var result = tested.startDocumentVerification(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.VerificationDocumentActionResult.REQUIRED_DOCUMENTS_VERIFIED));
        verify(processLimitService, never()).incrementErrorScore(any(), any(), any());
        verify(processLimitService, never()).checkOnboardingProcessErrorLimits(any());
        assertThat(idVerification.getErrorDetail(), nullValue());
    }

    private void mockStartDocumentVerification(
            final IdentityVerificationEntity idVerification,
            final List<DocumentVerificationEntity> batch,
            final boolean allRequiredDocumentsChecked) throws RemoteCommunicationException, DocumentVerificationException {

        when(identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()).thenReturn(true);
        when(documentVerificationRepository.findAllDocumentVerifications(eq(idVerification), anyList())).thenReturn(batch);
        when(documentVerificationRepository.findAllUsedForVerification(idVerification)).thenReturn(batch);
        when(requiredDocumentTypesCheck.evaluate(anyList(), eq(idVerification.getProcessId()))).thenReturn(allRequiredDocumentsChecked);

        final DocumentsVerificationResult verificationResult = DocumentsVerificationResult.builder()
                .verificationId("v1")
                .status(DocumentVerificationStatus.ACCEPTED)
                .build();

        when(documentVerificationProvider.verifyDocuments(any(), anyList())).thenReturn(verificationResult);
    }

    private static IdentityVerificationEntity identityVerification() {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setId("identity-verification-id");
        idVerification.setProcessId("process-id");
        return idVerification;
    }

    private static DocumentVerificationEntity document(final DocumentStatus status) {
        final DocumentVerificationEntity document = new DocumentVerificationEntity();
        document.setStatus(status);
        return document;
    }
}
