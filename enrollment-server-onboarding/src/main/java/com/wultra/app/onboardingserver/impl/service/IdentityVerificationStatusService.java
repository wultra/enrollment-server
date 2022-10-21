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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.IdentityVerificationStatusRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.IdentityVerificationStatusResponse;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;

/**
 * Service implementing document identity verification status services.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationStatusService {

    private final IdentityVerificationService identityVerificationService;

    private final OnboardingServiceImpl onboardingService;

    private final ActivationFlagService activationFlagService;

    /**
     * Service constructor.
     *
     * @param identityVerificationService       Identity verification service.
     * @param onboardingService                 Onboarding service.
     * @param activationFlagService             Activation flags service.
     */
    @Autowired
    public IdentityVerificationStatusService(
            final IdentityVerificationService identityVerificationService,
            final OnboardingServiceImpl onboardingService,
            final ActivationFlagService activationFlagService) {
        this.identityVerificationService = identityVerificationService;
        this.onboardingService = onboardingService;
        this.activationFlagService = activationFlagService;
    }

    /**
     * Check status of identity verification.
     *
     * @param request Identity verification status request.
     * @param ownerId Owner identifier.
     * @return Identity verification status response.
     * @throws RemoteCommunicationException   Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException     Thrown when onboarding process is invalid.
     */
    @Transactional
    @SuppressWarnings("unused") // unused request
    public IdentityVerificationStatusResponse checkIdentityVerificationStatus(IdentityVerificationStatusRequest request, OwnerId ownerId) throws RemoteCommunicationException, OnboardingProcessException {
        IdentityVerificationStatusResponse response = new IdentityVerificationStatusResponse();

        Optional<IdentityVerificationEntity> idVerificationOptional = identityVerificationService.findByOptional(ownerId);

        // Do not lock onboarding process, it is not required for status check
        final OnboardingProcessEntity onboardingProcess = onboardingService.findProcessByActivationId(ownerId.getActivationId());
        if (idVerificationOptional.isEmpty()) {
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            response.setProcessId(onboardingProcess.getId());
            return response;
        }

        final IdentityVerificationEntity idVerification = idVerificationOptional.get();
        response.setProcessId(idVerification.getProcessId());

        // Check for expiration of onboarding process
        if (onboardingService.hasProcessExpired(onboardingProcess)) {
            response.setIdentityVerificationStatus(FAILED);
            response.setIdentityVerificationPhase(IdentityVerificationPhase.COMPLETED);
            return response;
        }

        // Check activation flags, the mobile application needs to start over after cleanup or reaching attempts limit
        if (containsActivationFlagVerificationPending(ownerId)) {
            // Initialization is required because verification is not in progress for current identity verification
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            return response;
        }

        response.setIdentityVerificationStatus(idVerification.getStatus());
        response.setIdentityVerificationPhase(idVerification.getPhase());
        return response;
    }

    private boolean containsActivationFlagVerificationPending(OwnerId ownerId) throws RemoteCommunicationException {
        final List<String> flags = activationFlagService.listActivationFlags(ownerId);
        return flags.contains(ActivationFlagService.ACTIVATION_FLAG_VERIFICATION_PENDING);
    }
}
