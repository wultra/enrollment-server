/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.common.service.CommonProcessLimitService;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.wultra.app.onboardingserver.impl.service.ActivationFlagService.ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS;
import static com.wultra.app.onboardingserver.impl.service.ActivationFlagService.ACTIVATION_FLAG_VERIFICATION_PENDING;

/**
 * Service implementing reset of identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationResetService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationResetService.class);

    private final ActivationFlagService activationFlagService;
    private final IdentityVerificationLimitService identityVerificationLimitService;
    private final CommonProcessLimitService processLimitService;
    private final CommonOnboardingService processService;

    /**
     * Service constructor.
     * @param activationFlagService Activation flag service.
     * @param identityVerificationLimitService Identity verification limit service.
     * @param processLimitService Onboarding process limit service.
     * @param processService Common onboarding process service.
     */
    @Autowired
    public IdentityVerificationResetService(ActivationFlagService activationFlagService, IdentityVerificationLimitService identityVerificationLimitService, CommonProcessLimitService processLimitService, CommonOnboardingService processService) {
        this.activationFlagService = activationFlagService;
        this.identityVerificationLimitService = identityVerificationLimitService;
        this.processLimitService = processLimitService;
        this.processService = processService;
    }

    /**
     * Reset identity verification by setting activation flag to VERIFICATION_PENDING.
     *
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when identity verification reset fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    public void resetIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException, OnboardingProcessException {
        try {
            OnboardingProcessEntity process = processService.findProcessByActivationId(ownerId.getActivationId());

            // Increase process error score
            processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_IDENTITY_VERIFICATION_RESET);

            // Check process error limits
            process = processLimitService.checkOnboardingProcessErrorLimits(process);
            if (process.getStatus() == OnboardingStatus.FAILED && OnboardingProcessEntity.ERROR_MAX_PROCESS_ERROR_SCORE_EXCEEDED.equals(process.getErrorDetail())) {
                handleFailedProcess(ownerId);
            }

            // Check limits on identity verifications (error handling is handled by service)
            identityVerificationLimitService.checkIdentityVerificationLimit(ownerId);

            // Update activation flags for reset of identity verification
            updateActivationFlagsForReset(ownerId);

        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }
    }

    /**
     * Update activation flags for reset of identity verification.
     * @param ownerId Owner identification.
     * @throws PowerAuthClientException Thrown when communication with PowerAuth server fails.
     */
    private void updateActivationFlagsForReset(OwnerId ownerId) throws PowerAuthClientException {
        final List<String> activationFlags = activationFlagService.listActivationFlags(ownerId);

        // Remove flag VERIFICATION_IN_PROGRESS
        activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

        // Add flag VERIFICATION_PENDING to restart the identity verification process
        if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
            activationFlags.add(ACTIVATION_FLAG_VERIFICATION_PENDING);
        }

        activationFlagService.updateActivationFlags(ownerId, activationFlags);
    }

    /**
     * Handle a process which just failed due to error score limit.
     * @param ownerId Owner identification.
     * @throws OnboardingProcessLimitException Exception thrown due to failed process.
     * @throws PowerAuthClientException Thrown when communication with PowerAuth server fails.
     */
    private void handleFailedProcess(OwnerId ownerId) throws OnboardingProcessLimitException, PowerAuthClientException {
        // Remove flag VERIFICATION_IN_PROGRESS
        List<String> activationFlagsToRemove = Collections.singletonList(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);
        activationFlagService.removeActivationFlags(ownerId, activationFlagsToRemove);

        logger.warn("Max error score reached for onboarding process, {}.", ownerId);
        throw new OnboardingProcessLimitException("Max error score reached for onboarding process");
    }
}
