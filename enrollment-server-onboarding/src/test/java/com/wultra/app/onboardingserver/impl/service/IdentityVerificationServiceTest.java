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

import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesGuard;
import com.wultra.security.powerauth.client.v3.ActivationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.*;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test for {@link IdentityVerificationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private IdentityVerificationRepository identityVerificationRepository;

    @Mock
    private RequiredDocumentTypesGuard requiredDocumentTypesGuard;

    @Mock
    private IdentityVerificationConfig identityVerificationConfig;

    @Mock
    private OnboardingOtpRepository onboardingOtpRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ActivationService activationService;

    @InjectMocks
    private IdentityVerificationService tested;

    @Test
    void testProcessDocumentVerificationResult_valid() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);
        when(identityVerificationConfig.isVerificationOtpEnabled())
                .thenReturn(true);

        final OnboardingOtpEntity otp = new OnboardingOtpEntity();
        otp.setStatus(OtpStatus.VERIFIED);
        when(onboardingOtpRepository.findLastOtp("process-1", OtpType.USER_VERIFICATION))
                .thenReturn(Optional.of(otp));
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertSuccessfulIdentityVerification();
    }

    @Test
    void testProcessDocumentVerificationResult_invalidOtp() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);
        when(identityVerificationConfig.isVerificationOtpEnabled())
                .thenReturn(true);

        final OnboardingOtpEntity otp = new OnboardingOtpEntity();
        otp.setStatus(OtpStatus.FAILED);
        when(onboardingOtpRepository.findLastOtp("process-1", OtpType.USER_VERIFICATION))
                .thenReturn(Optional.of(otp));

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertFailedIdentityVerification("Not valid OTP");
    }

    @Test
    void testProcessDocumentVerificationResult_invalidPresenceCheck() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);
        when(identityVerificationConfig.isPresenceCheckEnabled())
                .thenReturn(true);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);
        idVerification.setRejectOrigin(RejectOrigin.PRESENCE_CHECK);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertFailedIdentityVerification("Presence check did not pass");
    }

    @Test
    void testProcessDocumentVerificationResult_validStateWithoutOtp() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(PRESENCE_CHECK);
        idVerification.setStatus(ACCEPTED);
        idVerification.setActivationId("activation-1");

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertSuccessfulIdentityVerification();
    }

    @Test
    void testProcessDocumentVerificationResult_validStateWithoutOtpAndPresenceCheck() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(CLIENT_EVALUATION);
        idVerification.setStatus(ACCEPTED);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertSuccessfulIdentityVerification();
    }

    @Test
    void testProcessDocumentVerificationResult_invalidActivation() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.REMOVED);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(CLIENT_EVALUATION);
        idVerification.setStatus(ACCEPTED);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertFailedIdentityVerification("Activation is not valid");
    }

    @Test
    void testProcessDocumentVerificationResult_failedDocument() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();

        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.FAILED);

        when(documentVerificationRepository.findAllDocumentVerifications(idVerification, IdentityVerificationService.DOCUMENT_STATUSES_PROCESSED))
                .thenReturn(List.of(documentVerification));

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertFailedIdentityVerification("Some documents not accepted");
    }

    @Test
    void testProcessDocumentVerificationResult_missingRequiredDocuments() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();

        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.ACCEPTED);

        when(documentVerificationRepository.findAllDocumentVerifications(idVerification, IdentityVerificationService.DOCUMENT_STATUSES_PROCESSED))
                .thenReturn(List.of(documentVerification));
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(false);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertFailedIdentityVerification("Required documents not present");
    }

    @Test
    void testProcessDocumentVerificationResult_invalidStatus() throws Exception {
        when(requiredDocumentTypesGuard.evaluate(any(), any()))
                .thenReturn(true);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(CLIENT_EVALUATION);
        idVerification.setStatus(IN_PROGRESS);

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        assertFailedIdentityVerification("Not valid phase and state");
    }

    private void assertSuccessfulIdentityVerification() {
        final ArgumentCaptor<IdentityVerificationEntity> argumentCaptor = ArgumentCaptor.forClass(IdentityVerificationEntity.class);
        verify(identityVerificationRepository, atLeastOnce()).save(argumentCaptor.capture());
        final IdentityVerificationEntity savedIdentityVerification = argumentCaptor.getValue();

        assertThat(savedIdentityVerification.getPhase(), equalTo(COMPLETED));
        assertThat(savedIdentityVerification.getStatus(), equalTo(ACCEPTED));
    }

    private void assertFailedIdentityVerification(final String errorDetail) {
        final ArgumentCaptor<IdentityVerificationEntity> argumentCaptor = ArgumentCaptor.forClass(IdentityVerificationEntity.class);
        verify(identityVerificationRepository, atLeastOnce()).save(argumentCaptor.capture());
        final IdentityVerificationEntity savedIdentityVerification = argumentCaptor.getValue();

        assertThat(savedIdentityVerification.getPhase(), equalTo(COMPLETED));
        assertThat(savedIdentityVerification.getStatus(), equalTo(FAILED));
        assertThat(savedIdentityVerification.getErrorDetail(), equalTo(errorDetail));
        assertThat(savedIdentityVerification.getErrorOrigin(), equalTo(ErrorOrigin.FINAL_VALIDATION));
    }
}
