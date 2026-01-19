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

        if (!documentVerifications.stream()
                .map(DocumentVerificationEntity::getStatus)
                .allMatch(it -> it == DocumentStatus.ACCEPTED)) {
            logger.debug("Some documents are not accepted for identity verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Some documents not accepted");
        }

        if (!requiredDocumentTypesCheck.evaluate(documentVerifications, processId)) {
            logger.debug("Not all required documents are present for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Required documents not present");
        }

        if (!isPrecompletePhaseAndStateValid(idVerification)) {
            logger.debug("Not valid phase and state for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Not valid phase and state");
        }

        if (!isVerificationOtpValid(idVerification)) {
            logger.debug("Not valid user verification OTP for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Not valid user verification OTP");
        }

        if (!isActivationOtpValid(idVerification)) {
            logger.debug("Not valid activation OTP for verification ID: {}, process ID:{}", identityVerificationId, processId);
            return Result.failed("Not valid activation OTP");
        }

        if (!isActivationValid(idVerification)) {
            logger.debug("Activation is not valid for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Activation is not valid");
        }

        if (!isVerificationPassedSca(idVerification)) {
            logger.debug("Did not pass SCA for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Did not pass SCA");
        }

        if (!isTargetActivationFinished(idVerification)) {
            logger.debug("Target activation is not valid for verification ID: {}, process ID: {}", identityVerificationId, processId);
            return Result.failed("Target activation is not valid");
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

        if (isVerificationOtpDisabled(idVerification)) {
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
        if (isVerificationOtpDisabled(idVerification)) {
            logger.trace("OTP verification is disabled");
            return true;
        }
        return isOtpValid(idVerification, OtpType.USER_VERIFICATION);
    }

    private boolean isActivationOtpValid(final IdentityVerificationEntity idVerification) {
        if (isActivationOtpDisabled(idVerification)) {
            logger.trace("OTP activation is disabled");
            return true;
        }
        return isOtpValid(idVerification, OtpType.ACTIVATION);
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
        final boolean verificationOtpDisabled = isVerificationOtpDisabled(idVerification);
        return (phase == OTP_VERIFICATION && status == VERIFICATION_PENDING) ||
                (phase == PRESENCE_CHECK && status == ACCEPTED && verificationOtpDisabled) ||
                (phase == CLIENT_EVALUATION && status == ACCEPTED && verificationOtpDisabled && !identityVerificationConfig.isPresenceCheckEnabled()) ||
                (phase == ACTIVATION_FINISH && status == IN_PROGRESS);
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

    private boolean isVerificationOtpDisabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationDisabled(idVerification, OnboardingProcessConfigurationValue::otpForIdentityVerification);
    }

    private boolean isActivationOtpDisabled(final IdentityVerificationEntity idVerification) {
        return isConfigurationDisabled(idVerification, OnboardingProcessConfigurationValue::otpForIdentification);
    }

    private boolean isConfigurationDisabled(
            final IdentityVerificationEntity idVerification,
            final Predicate<OnboardingProcessConfigurationValue> predicate) {

        return onboardingProcessRepository.findById(idVerification.getProcessId())
                .map(OnboardingProcessEntity::getProcessConfiguration)
                .map(OnboardingProcessConfigurationEntity::getConfiguration)
                .filter(predicate)
                .isEmpty();
    }
}
