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
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.ScaResultEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesCheck;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.*;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link IdentityVerificationPrecompleteCheck}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationPrecompleteCheckTest {

    @Mock
    private RequiredDocumentTypesCheck requiredDocumentTypesCheck;

    @Mock
    private IdentityVerificationConfig identityVerificationConfig;

    @Mock
    private OnboardingOtpRepository onboardingOtpRepository;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private ActivationService activationService;

    @Mock
    private ScaResultRepository scaResultRepository;

    @InjectMocks
    private IdentityVerificationPrecompleteCheck tested;

    @Test
    void testProcessDocumentVerificationResult_valid() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(VERIFICATION_PENDING);

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setScaResult(ScaResultEntity.Result.SUCCESS);

        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);
        when(identityVerificationConfig.isVerificationOtpEnabled())
                .thenReturn(true);

        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.USER_VERIFICATION))
                .thenReturn(Optional.of(createOtp()));
        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.ACTIVATION))
                .thenReturn(Optional.of(createOtp()));
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);
        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(idVerification))
                .thenReturn(Optional.of(scaResult));

        final var result = tested.evaluate(idVerification);

        assertTrue(result.isSuccessful());
    }

    @Test
    void testProcessDocumentVerificationResult_invalidVerificationOtp() throws Exception {
        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);
        when(identityVerificationConfig.isVerificationOtpEnabled())
                .thenReturn(true);

        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.USER_VERIFICATION))
                .thenReturn(Optional.of(createFailedOtp()));

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(VERIFICATION_PENDING);

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
        assertEquals("Not valid user verification OTP", result.getErrorDetail());
    }

    @Test
    void testProcessDocumentVerificationResult_invalidActivationOtp() throws Exception {
        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);

        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.ACTIVATION))
                .thenReturn(Optional.of(createFailedOtp()));

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(VERIFICATION_PENDING);

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
        assertEquals("Not valid activation OTP", result.getErrorDetail());
    }

    @Test
    void testProcessDocumentVerificationResult_validStateWithoutOtp() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setPhase(PRESENCE_CHECK);
        idVerification.setStatus(ACCEPTED);
        idVerification.setActivationId("activation-1");

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setScaResult(ScaResultEntity.Result.SUCCESS);

        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);
        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.ACTIVATION))
                .thenReturn(Optional.of(createOtp()));

        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(idVerification))
                .thenReturn(Optional.of(scaResult));

        final var result = tested.evaluate(idVerification);

        assertTrue(result.isSuccessful());
    }

    @Test
    void testProcessDocumentVerificationResult_validStateWithoutOtpAndPresenceCheck() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(CLIENT_EVALUATION);
        idVerification.setStatus(ACCEPTED);

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setScaResult(ScaResultEntity.Result.SUCCESS);

        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);
        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.ACTIVATION))
                .thenReturn(Optional.of(createOtp()));
        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(idVerification))
                .thenReturn(Optional.of(scaResult));

        final var result = tested.evaluate(idVerification);

        assertTrue(result.isSuccessful());
    }

    @Test
    void testProcessDocumentVerificationResult_invalidActivation() throws Exception {
        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.REMOVED);
        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.ACTIVATION))
                .thenReturn(Optional.of(createOtp()));

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(CLIENT_EVALUATION);
        idVerification.setStatus(ACCEPTED);

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
        assertEquals("Activation is not valid", result.getErrorDetail());
    }

    @Test
    void testProcessDocumentVerificationResult_failedDocument() throws Exception {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.FAILED);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();

        when(documentVerificationRepository.findAllDocumentVerifications(idVerification, DocumentStatus.ALL_PROCESSED))
                .thenReturn(List.of(documentVerification));

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
        assertEquals("Some documents not accepted", result.getErrorDetail());
    }

    @Test
    void testProcessDocumentVerificationResult_missingRequiredDocuments() throws Exception {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.ACCEPTED);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();

        when(documentVerificationRepository.findAllDocumentVerifications(idVerification, DocumentStatus.ALL_PROCESSED))
                .thenReturn(List.of(documentVerification));

        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(false);

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
        assertEquals("Required documents not present", result.getErrorDetail());
    }

    @Test
    void testProcessDocumentVerificationResult_invalidStatus() throws Exception {
        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);

        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(CLIENT_EVALUATION);
        idVerification.setStatus(IN_PROGRESS);

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
        assertEquals("Not valid phase and state", result.getErrorDetail());
    }

    @Test
    void testProcessDocumentVerificationResult_invalidSca() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setProcessId("process-1");
        idVerification.setActivationId("activation-1");
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(VERIFICATION_PENDING);

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setScaResult(ScaResultEntity.Result.FAILED);

        when(requiredDocumentTypesCheck.evaluate(any(), any()))
                .thenReturn(true);
        when(identityVerificationConfig.isVerificationOtpEnabled())
                .thenReturn(true);

        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.USER_VERIFICATION))
                .thenReturn(Optional.of(createOtp()));
        when(onboardingOtpRepository.findNewestByProcessIdAndType("process-1", OtpType.ACTIVATION))
                .thenReturn(Optional.of(createOtp()));
        when(activationService.fetchActivationStatus("activation-1"))
                .thenReturn(ActivationStatus.ACTIVE);
        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(idVerification))
                .thenReturn(Optional.of(scaResult));

        final var result = tested.evaluate(idVerification);

        assertFalse(result.isSuccessful());
    }

    private static OnboardingOtpEntity createOtp() {
        final OnboardingOtpEntity otp = new OnboardingOtpEntity();
        otp.setStatus(OtpStatus.VERIFIED);
        return otp;
    }

    private static OnboardingOtpEntity createFailedOtp() {
        final OnboardingOtpEntity otp = new OnboardingOtpEntity();
        otp.setStatus(OtpStatus.FAILED);
        return otp;
    }
}
