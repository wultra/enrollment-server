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
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProcessLimitException;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.wultra.app.onboardingserver.impl.service.ActivationFlagService.ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS;

/**
 * Service for checking identity verification limits.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationLimitService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationLimitService.class);

    private final IdentityVerificationRepository identityVerificationRepository;
    private final IdentityVerificationConfig identityVerificationConfig;
    private final OnboardingProcessRepository onboardingProcessRepository;
    private final ActivationFlagService activationFlagService;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param identityVerificationConfig Identity verification config.
     * @param onboardingProcessRepository Onboarding process service.
     * @param activationFlagService Activation flag service.
     */
    @Autowired
    public IdentityVerificationLimitService(IdentityVerificationRepository identityVerificationRepository, IdentityVerificationConfig identityVerificationConfig, OnboardingProcessRepository onboardingProcessRepository, ActivationFlagService activationFlagService) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationConfig = identityVerificationConfig;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.activationFlagService = activationFlagService;
    }

    /**
     * Check attempt limit of failed identity verifications. Fail onboarding process in the attempt count has exceeded the limit.
     * @param ownerId Owner identification.
     * @throws PowerAuthClientException Thrown when activation flag request fails.
     * @throws IdentityVerificationException Thrown in case identity verification is invalid.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    public void checkIdentityVerificationLimit(OwnerId ownerId) throws PowerAuthClientException, IdentityVerificationException, OnboardingProcessLimitException {
        // Make sure that the maximum attempt number of identity verifications is not exceeded based on count of database rows.
        List<IdentityVerificationEntity> identityVerifications = identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());
        if (identityVerifications.size() >= identityVerificationConfig.getVerificationMaxFailedAttempts()) {
            Optional<OnboardingProcessEntity> onboardingProcessOptional = onboardingProcessRepository.findProcessByActivationId(ownerId.getActivationId());
            if (onboardingProcessOptional.isEmpty()) {
                logger.warn("Onboarding process not found, {}.", ownerId);
                throw new IdentityVerificationException("Onboarding process not found");
            }

            // In case any of the identity verifications is not FAILED or REJECTED yet, fail it.
            identityVerifications.stream()
                    .filter(verification -> verification.getStatus() != IdentityVerificationStatus.FAILED
                            && verification.getStatus() != IdentityVerificationStatus.REJECTED)
                    .forEach(verification -> verification.setStatus(IdentityVerificationStatus.FAILED));
            identityVerificationRepository.saveAll(identityVerifications);

            OnboardingProcessEntity onboardingProcess = onboardingProcessOptional.get();
            onboardingProcess.setErrorDetail(OnboardingProcessEntity.ERROR_MAX_FAILED_ATTEMPTS);
            onboardingProcess.setStatus(OnboardingStatus.FAILED);
            onboardingProcessRepository.save(onboardingProcess);

            // Remove flag VERIFICATION_IN_PROGRESS
            List<String> activationFlagsToRemove = Collections.singletonList(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);
            activationFlagService.removeActivationFlags(ownerId, activationFlagsToRemove);
            logger.warn("Max failed attempts reached for identity verification, {}.", ownerId);
            throw new OnboardingProcessLimitException("Max failed attempts reached for identity verification");
        }
    }
}