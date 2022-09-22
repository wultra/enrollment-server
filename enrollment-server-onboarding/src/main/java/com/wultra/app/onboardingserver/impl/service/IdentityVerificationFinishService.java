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
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Service implementing finishing of identity verification.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationFinishService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationFinishService.class);

    private final OnboardingServiceImpl onboardingService;
    private final IdentityVerificationService identityVerificationService;
    private final ActivationFlagService activationFlagService;

    /**
     * Service constructor.
     * @param onboardingService Onboarding service.
     * @param identityVerificationService Identity verification service.
     * @param activationFlagService Activation flags service.
     */
    @Autowired
    public IdentityVerificationFinishService(OnboardingServiceImpl onboardingService, IdentityVerificationService identityVerificationService, ActivationFlagService activationFlagService) {
        this.onboardingService = onboardingService;
        this.identityVerificationService = identityVerificationService;
        this.activationFlagService = activationFlagService;
    }

    /**
     * Finish identity verification by removing the VERIFICATION_IN_PROGRESS flag.
     *
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process termination fails.
     * @throws IdentityVerificationException Thrown when identity verification is already finished.
     */
    @Transactional
    public void finishIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException {
        final Date now = ownerId.getTimestamp();

        // Remove flag ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS
        activationFlagService.updateActivationFlagsForSucceededIdentityVerification(ownerId);

        // Find latest identity verification record and set the timestamp when it was finished
        final IdentityVerificationEntity identityVerification = identityVerificationService.findBy(ownerId);
        identityVerification.setTimestampLastUpdated(now);
        identityVerification.setTimestampFinished(now);
        identityVerificationService.updateIdentityVerification(identityVerification);

        // Terminate onboarding process
        final OnboardingProcessEntity processEntity = onboardingService.findExistingProcessWithVerificationInProgress(ownerId.getActivationId());
        processEntity.setStatus(OnboardingStatus.FINISHED);
        processEntity.setTimestampLastUpdated(now);
        processEntity.setTimestampFinished(now);
        onboardingService.updateProcess(processEntity);
    }

}
