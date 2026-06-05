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
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesCheck;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.*;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;

/**
 * Validate all critical conditions were met before finishing the onboarding.
 * <p>
 * This should never happen for the state machine.
 * It works as a safety stop.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO (racansky, 2022-10-14, #1458) consider make it Guard for Spring State Machine
@Component
@Slf4j
@AllArgsConstructor
class IdentityVerificationPrecompleteCheck {

    private static final String REQUIRED_DOCUMENTS_NOT_PRESENT = "Required documents not present";
    private static final String NOT_VALID_PHASE_AND_STATE = "Not valid phase and state";
    private static final String NOT_VALID_USER_VERIFICATION_OTP = "Not valid user verification OTP";
    private static final String NOT_VALID_ACTIVATION_OTP = "Not valid activation OTP";
    private static final String NOT_VALID_ACTIVATION = "Activation is not valid";
    private static final String NOT_VALID_SCA = "Did not pass SCA";
    private static final String NOT_VALID_TARGET_ACTIVATION = "Target activation is not valid";

    private final IdentityVerificationConfig identityVerificationConfig;

    private final RequiredDocumentTypesCheck requiredDocumentTypesCheck;

    private final OnboardingOtpRepository onboardingOtpRepository;

    private final ScaResultRepository scaResultRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final ActivationService activationService;

    // TODO (racansky, 2022-10-14, #1458) when changed to a guard, there is no reference IdentityVerificationService -> PrecompleteCheck anymore
    @Lazy // break circular reference of constructor injection
    private final IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    /**
     * Evaluate all precomplete conditions.
     *
     * @param idVerification identity verification to evaluate
     * @return evaluation result
     */
    Result evaluate(final IdentityVerificationEntity idVerification) throws RemoteCommunicationException {
        final List<DocumentVerificationEntity> documentVerifications = documentVerificationRepository
                .findAllDocumentVerifications(idVerification, DocumentStatus.ALL_PROCESSED);

        final String processId = idVerification.getProcessId();
        final String identityVerificationId = idVerification.getId();

        if (!requiredDocumentTypesCheck.evaluate(documentVerifications, processId)) {
            logger.debug("Not all required documents are present for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed(REQUIRED_DOCUMENTS_NOT_PRESENT);
        }

        if (!isPrecompletePhaseAndStateValid(idVerification)) {
            logger.debug("Not valid phase and state for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed(NOT_VALID_PHASE_AND_STATE);
        }

        if (!isVerificationOtpValid(idVerification)) {
            logger.debug("Not valid user verification OTP for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed(NOT_VALID_USER_VERIFICATION_OTP);
        }

        if (!isActivationOtpValid(idVerification)) {
            logger.debug("Not valid activation OTP for verification ID: {}, process ID:{}", identityVerificationId, processId);
            return Result.failed(NOT_VALID_ACTIVATION_OTP);
        }

        if (!isActivationValid(idVerification)) {
            logger.debug("Activation is not valid for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed(NOT_VALID_ACTIVATION);
        }

        if (!isVerificationPassedSca(idVerification)) {
            logger.debug("Did not pass SCA for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed(NOT_VALID_SCA);
        }

        if (!isTargetActivationFinished(idVerification)) {
            logger.debug("Target activation is not valid for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed(NOT_VALID_TARGET_ACTIVATION);
        }

        return Result.successful();
    }

    private boolean isTargetActivationFinished(final IdentityVerificationEntity idVerification) throws RemoteCommunicationException {
        final String processId = idVerification.getProcessId();

        try {
            final boolean isTemporaryActivationDisabled = !identityVerificationTargetActivationService.isTargetActivationEnabled(processId);
            if (isTemporaryActivationDisabled) {
                logger.trace("Temporary activation is disabled");
                return true;
            }
        } catch (OnboardingProcessException e) {
            logger.warn("Unable to find processId: {}", processId, e);
            return false;
        }

        return identityVerificationTargetActivationService.isTargetActivationFinished(processId);
    }

    private boolean isVerificationPassedSca(final IdentityVerificationEntity idVerification) {
        final ScaResultEntity scaResultEntity = scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(idVerification).orElse(null);
        if (scaResultEntity == null) {
            return false;
        }

        if (!isVerificationOtpEnabled(idVerification)) {
            logger.debug("OTP is disable, verifying only presence check result");
            return scaResultEntity.getPresenceCheckResult() == ScaResultEntity.Result.SUCCESS;
        }

        return scaResultEntity.getScaResult() == ScaResultEntity.Result.SUCCESS;
    }

    private boolean isActivationValid(final IdentityVerificationEntity idVerification) throws RemoteCommunicationException {
        final String processId = idVerification.getProcessId();
        final String activationId = idVerification.getActivationId();

        final ActivationStatus activationStatus = activationService.fetchActivationStatus(activationId);
        final boolean isTemporaryActivationEnabled;

        try {
            isTemporaryActivationEnabled = identityVerificationTargetActivationService.isTargetActivationEnabled(processId);
        } catch (OnboardingProcessException e) {
            logger.warn("Unable to find processId: {}", processId, e);
            return false;
        }

        logger.debug("Verifying activationId: {}, status: {}, isTemporaryActivationEnabled: {}", activationId, activationStatus, isTemporaryActivationEnabled);
        if (isTemporaryActivationEnabled) {
            return activationStatus == ActivationStatus.REMOVED;
        } else {
            return activationStatus == ActivationStatus.ACTIVE;
        }
    }

    private boolean isVerificationOtpValid(final IdentityVerificationEntity idVerification) {
        if (isVerificationOtpEnabled(idVerification)) {
            return isOtpValid(idVerification, OtpType.USER_VERIFICATION);
        } else {
            logger.trace("OTP verification is disabled");
            return true;
        }
    }

    private boolean isActivationOtpValid(final IdentityVerificationEntity idVerification) {
        if (isActivationOtpEnabled(idVerification)) {
            return isOtpValid(idVerification, OtpType.ACTIVATION);
        } else {
            logger.trace("OTP activation is disabled");
            return true;
        }
    }

    private boolean isOtpValid(IdentityVerificationEntity idVerification, OtpType otpType) {
        return onboardingOtpRepository.findNewestByProcessIdAndType(idVerification.getProcessId(), otpType)
                .map(OnboardingOtpEntity::getStatus)
                .filter(it -> it == OtpStatus.VERIFIED)
                .isPresent();
    }

    private boolean isPrecompletePhaseAndStateValid(final IdentityVerificationEntity idVerification) {
        final IdentityVerificationPhase phase = idVerification.getPhase();
        final IdentityVerificationStatus status = idVerification.getStatus();
        final boolean clientEvaluationEnabled = isVerificationClientEvaluationEnabled(idVerification);
        final boolean approvalEnabled = isVerificationApprovalEnabled(idVerification);
        final boolean verificationOtpEnabled = isVerificationOtpEnabled(idVerification);
        final boolean useTemporaryActivation = useTemporaryActivationEnabled(idVerification);

        // return possible steps based on the chronology of the controls

        if (useTemporaryActivation) {

            return (phase == ACTIVATION_FINISH && status == IN_PROGRESS);

        } else {

            if (approvalEnabled) {
                return (phase == ONBOARDING_APPROVAL && status == ACCEPTED);
            }

            if (verificationOtpEnabled) {
                return (phase == OTP_VERIFICATION && status == VERIFICATION_PENDING);
            }

            if (identityVerificationConfig.isPresenceCheckEnabled()) {
                return (phase == PRESENCE_CHECK && status == ACCEPTED );
            }

            if (clientEvaluationEnabled) {
                return (phase == CLIENT_EVALUATION && status == ACCEPTED );
            }
        }
        return false;
    }

    @Getter
    @Builder
    public static final class Result {
        private boolean successful;
        private String errorDetail;

        public static Result successful() {
            return Result.builder()
                    .successful(true)
                    .build();
        }

        public static Result failed(final String errorDetail) {
            return Result.builder()
                    .successful(false)
                    .errorDetail(errorDetail)
                    .build();
        }
    }

    private boolean isVerificationOtpEnabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationEnabled(idVerification, OnboardingProcessConfigurationValue::otpForIdentityVerification);
    }

    private boolean isActivationOtpEnabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationEnabled(idVerification, OnboardingProcessConfigurationValue::otpForIdentification);
    }

    private boolean isVerificationApprovalEnabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationEnabled(idVerification, OnboardingProcessConfigurationValue::approvalEnabled);
    }

    private boolean isVerificationClientEvaluationEnabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationEnabled(idVerification, OnboardingProcessConfigurationValue::clientEvaluationEnabled);
    }

    private boolean useTemporaryActivationEnabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationEnabled(idVerification, OnboardingProcessConfigurationValue::useTemporaryActivation);
    }

    private boolean isConfigurationEnabled(
            final IdentityVerificationEntity idVerification,
            final Predicate<OnboardingProcessConfigurationValue> predicate) {

        return onboardingProcessRepository.findById(idVerification.getProcessId())
                .map(OnboardingProcessEntity::getProcessConfiguration)
                .map(OnboardingProcessConfigurationEntity::getConfiguration)
                .filter(predicate)
                .isPresent();
    }
}
