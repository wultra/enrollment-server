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
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.ScaResultEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesCheck;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.*;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.VERIFICATION_PENDING;

/**
 * Validate all critical conditions were met before finishing the onboarding.
 * <p>
 * This should never happen for the state machine.
 * It works as a safety stop.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO (racansky, 2022-10-14) consider make it Guard for Spring State Machine
@Component
@Slf4j
@AllArgsConstructor
class IdentityVerificationPrecompleteCheck {

    private final IdentityVerificationConfig identityVerificationConfig;

    private final RequiredDocumentTypesCheck requiredDocumentTypesCheck;

    private final OnboardingOtpRepository onboardingOtpRepository;

    private final ScaResultRepository scaResultRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final ActivationService activationService;

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

        if (!requiredDocumentTypesCheck.evaluate(documentVerifications, identityVerificationId)) {
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

        return Result.successful();
    }

    private boolean isVerificationPassedSca(final IdentityVerificationEntity idVerification) {
        return scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(idVerification)
                .map(ScaResultEntity::getScaResult)
                .filter(it -> it == ScaResultEntity.Result.SUCCESS)
                .isPresent();
    }

    private boolean isActivationValid(IdentityVerificationEntity idVerification) throws RemoteCommunicationException {
        final ActivationStatus activationStatus = activationService.fetchActivationStatus(idVerification.getActivationId());
        return activationStatus == ActivationStatus.ACTIVE;
    }

    private boolean isVerificationOtpValid(final IdentityVerificationEntity idVerification) {
        if (!identityVerificationConfig.isVerificationOtpEnabled()) {
            logger.trace("OTP verification is disabled");
            return true;
        }
        return isOtpValid(idVerification, OtpType.USER_VERIFICATION);
    }

    private boolean isActivationOtpValid(final IdentityVerificationEntity idVerification) {
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
        return (phase == OTP_VERIFICATION && status == VERIFICATION_PENDING) ||
                (phase == PRESENCE_CHECK && status == ACCEPTED && !identityVerificationConfig.isVerificationOtpEnabled()) ||
                (phase == CLIENT_EVALUATION && status == ACCEPTED && !identityVerificationConfig.isVerificationOtpEnabled() && !identityVerificationConfig.isPresenceCheckEnabled());
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
}
