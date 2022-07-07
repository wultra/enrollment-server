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

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckLimitException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK;

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
    private final IdentityVerificationResetService identityVerificationResetService;

    /**
     * Service constructor.
     * @param identityVerificationConfig Identity verification configuration.
     * @param otpRepository Onboarding OTP repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param identityVerificationResetService Identity verification reset service.
     */
    @Autowired
    public PresenceCheckLimitService(IdentityVerificationConfig identityVerificationConfig, OnboardingOtpRepository otpRepository, IdentityVerificationRepository identityVerificationRepository, IdentityVerificationResetService identityVerificationResetService) {
        this.identityVerificationConfig = identityVerificationConfig;
        this.otpRepository = otpRepository;
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationResetService = identityVerificationResetService;
    }

    /**
     * Check limit for maximum number of attempts for presence check and OTP verification.
     * @param ownerId Owner identification.
     * @param processId Process identifier.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws PresenceCheckLimitException Thrown when presence check limit is exceeded.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    public void checkPresenceCheckMaxAttemptLimit(OwnerId ownerId, String processId) throws IdentityVerificationException, PresenceCheckLimitException, RemoteCommunicationException, OnboardingProcessLimitException {
        int otpCount = otpRepository.getOtpCount(processId, OtpType.USER_VERIFICATION);
        if (otpCount > identityVerificationConfig.getPresenceCheckMaxFailedAttempts()) {
            Optional<IdentityVerificationEntity> identityVerificationOptional = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());
            if (identityVerificationOptional.isEmpty()) {
                logger.warn("Identity verification was not found, {}.", ownerId);
                throw new IdentityVerificationException("Identity verification was not found");
            }
            IdentityVerificationEntity identityVerification = identityVerificationOptional.get();
            if (!identityVerification.getProcessId().equals(processId)) {
                logger.warn("Process identifier mismatch for owner {}: {}.", ownerId, processId);
                throw new IdentityVerificationException("Process identifier mismatch");
            }
            identityVerification.setStatus(IdentityVerificationStatus.FAILED);
            identityVerification.setErrorDetail(ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK);
            identityVerificationResetService.resetIdentityVerification(ownerId);
            throw new PresenceCheckLimitException("Max failed attempts reached for presence check");
        }
    }

}