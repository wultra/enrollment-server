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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import com.wultra.app.onboardingserver.common.service.IdentityVerificationLimitService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_UPLOAD;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS;

/**
 * Service implementing creating of identity verification.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@AllArgsConstructor
public class IdentityVerificationCreateService {

    private final IdentityVerificationService identityVerificationService;
    private final ActivationFlagService activationFlagService;
    private final IdentityVerificationLimitService identityVerificationLimitService;
    private final OnboardingProcessConfigurationService onboardingProcessConfigurationService;

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
        // Check limits on identity verifications
        identityVerificationLimitService.checkIdentityVerificationLimit(ownerId);

        assignActivationFlags(ownerId, processId);

        final IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUserId(ownerId.getUserId());
        entity.setProcessId(processId);

        return identityVerificationService.moveToPhaseAndStatus(entity, DOCUMENT_UPLOAD, IN_PROGRESS, ownerId);
    }

    private void assignActivationFlags(final OwnerId ownerId, final String processId) throws IdentityVerificationException, RemoteCommunicationException {
        final var configuration = onboardingProcessConfigurationService.findConfigByProcessId(processId);
        if (configuration.existingActivation()) {
            activationFlagService.addActivationFlag(ownerId, configuration.existingActivationFlag());
        } else {
            activationFlagService.initActivationFlagsForIdentityVerification(ownerId);
        }
    }

}
