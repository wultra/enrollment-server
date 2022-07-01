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

import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

import static com.wultra.app.onboardingserver.impl.service.ActivationFlagService.ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS;
import static com.wultra.app.onboardingserver.impl.service.ActivationFlagService.ACTIVATION_FLAG_VERIFICATION_PENDING;

/**
 * Service implementing creating of identity verification.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationCreateService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationCreateService.class);

    private final IdentityVerificationRepository identityVerificationRepository;
    private final ActivationFlagService activationFlagService;
    private final IdentityVerificationLimitService identityVerificationLimitService;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param activationFlagService Activation flag service.
     * @param identityVerificationLimitService Identity verification limit service.
     */
    @Autowired
    public IdentityVerificationCreateService(IdentityVerificationRepository identityVerificationRepository, ActivationFlagService activationFlagService, IdentityVerificationLimitService identityVerificationLimitService) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.activationFlagService = activationFlagService;
        this.identityVerificationLimitService = identityVerificationLimitService;
    }

    /**
     * Creates new identity for the verification process.
     *
     * @param ownerId Owner identification.
     * @return Identity verification entity
     * @throws IdentityVerificationException Thrown when identity verification initialization fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    @Transactional
    public IdentityVerificationEntity createIdentityVerification(OwnerId ownerId, String processId) throws IdentityVerificationException, RemoteCommunicationException, OnboardingProcessLimitException {
        try {
            // Check limits on identity verifications
            identityVerificationLimitService.checkIdentityVerificationLimit(ownerId);

            List<String> activationFlags = activationFlagService.listActivationFlags(ownerId);
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                throw new IdentityVerificationException("Activation flag VERIFICATION_PENDING not found when initializing identity verification");
            }
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_PENDING);
            activationFlags.add(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            activationFlagService.updateActivationFlags(ownerId, activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }

        final IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
        entity.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUserId(ownerId.getUserId());
        entity.setProcessId(processId);

        return identityVerificationRepository.save(entity);
    }

}
