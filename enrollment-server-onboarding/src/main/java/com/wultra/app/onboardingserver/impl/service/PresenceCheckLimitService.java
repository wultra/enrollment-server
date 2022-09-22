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
 *
 */

package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckLimitException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for checking presence check limits.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class PresenceCheckLimitService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceCheckLimitService.class);

    private final IdentityVerificationConfig identityVerificationConfig;
    private final OnboardingOtpRepository otpRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final OnboardingProcessRepository onboardingProcessRepository;
    private final ActivationFlagService activationFlagService;

    /**
     * Service constructor.
     * @param identityVerificationConfig Identity verification configuration.
     * @param otpRepository Onboarding OTP repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param activationFlagService Activation flag service.
     */
    @Autowired
    public PresenceCheckLimitService(IdentityVerificationConfig identityVerificationConfig, OnboardingOtpRepository otpRepository, IdentityVerificationRepository identityVerificationRepository, OnboardingProcessRepository onboardingProcessRepository, ActivationFlagService activationFlagService) {
        this.identityVerificationConfig = identityVerificationConfig;
        this.otpRepository = otpRepository;
        this.identityVerificationRepository = identityVerificationRepository;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.activationFlagService = activationFlagService;
    }

    /**
     * Check limit for maximum number of attempts for presence check and OTP verification.
     * @param ownerId Owner identification.
     * @param processId Process identifier.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws PresenceCheckLimitException Thrown when presence check limit is exceeded.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void checkPresenceCheckMaxAttemptLimit(OwnerId ownerId, String processId) throws IdentityVerificationException, PresenceCheckLimitException, RemoteCommunicationException {
        final int otpCount = otpRepository.getOtpCount(processId, OtpType.USER_VERIFICATION);
        if (otpCount > identityVerificationConfig.getPresenceCheckMaxFailedAttempts()) {
            final Optional<IdentityVerificationEntity> identityVerificationOptional = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());
            if (identityVerificationOptional.isEmpty()) {
                logger.warn("Identity verification was not found, {}.", ownerId);
                throw new IdentityVerificationException("Identity verification was not found");
            }
            final IdentityVerificationEntity identityVerification = identityVerificationOptional.get();
            if (!identityVerification.getProcessId().equals(processId)) {
                logger.warn("Process identifier mismatch for owner {}: {}.", ownerId, processId);
                throw new IdentityVerificationException("Process identifier mismatch");
            }
            final Optional<OnboardingProcessEntity> onboardingProcessOptional = onboardingProcessRepository.findById(processId);
            if (onboardingProcessOptional.isEmpty()) {
                logger.warn("Onboarding process not found, {}.", ownerId);
                throw new IdentityVerificationException("Onboarding process not found");
            }

            identityVerification.setStatus(IdentityVerificationStatus.FAILED);
            identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK);
            identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            identityVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
            identityVerificationRepository.save(identityVerification);
            logger.info("Switched to {}/FAILED; process ID: {}", identityVerification.getPhase(), identityVerification.getProcessId());

            final OnboardingProcessEntity onboardingProcess = onboardingProcessOptional.get();
            onboardingProcess.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK);
            onboardingProcess.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            onboardingProcess.setTimestampLastUpdated(ownerId.getTimestamp());
            onboardingProcess.setTimestampFailed(ownerId.getTimestamp());
            onboardingProcess.setStatus(OnboardingStatus.FAILED);
            onboardingProcessRepository.save(onboardingProcess);

            // Remove flag VERIFICATION_IN_PROGRESS and add VERIFICATION_PENDING flag
            activationFlagService.updateActivationFlagsForFailedIdentityVerification(ownerId);

            throw new PresenceCheckLimitException("Max failed attempts reached for presence check");
        }
    }

}