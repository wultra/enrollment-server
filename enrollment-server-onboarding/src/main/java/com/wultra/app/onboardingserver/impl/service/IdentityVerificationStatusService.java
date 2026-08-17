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
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;

/**
 * Service implementing document identity verification status services.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@AllArgsConstructor
public class IdentityVerificationStatusService {

    private final IdentityVerificationService identityVerificationService;

    private final OnboardingServiceImpl onboardingService;

    private final ActivationFlagService activationFlagService;

    /**
     * Check status of identity verification.
     *
     * @param request Identity verification status request.
     * @param ownerId Owner identifier.
     * @return Identity verification status response.
     * @throws RemoteCommunicationException   Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException     Thrown when onboarding process is invalid.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unused") // unused request
    public IdentityVerificationStatusResponse checkIdentityVerificationStatus(IdentityVerificationStatusRequest request, OwnerId ownerId) throws RemoteCommunicationException, OnboardingProcessException {
        final IdentityVerificationStatusResponse response = new IdentityVerificationStatusResponse();

        // Do not lock onboarding process, it is not required for status check. An activation can have multiple
        // onboarding processes (e.g. re-KYC with existingActivation after an earlier onboarding process), and a
        // process can have multiple identity verification attempts after cleanup or reset. Resolve the latest
        // process first, then load its latest identity verification by process ID.
        final var onboardingProcess = onboardingService.findProcessByActivationId(ownerId.getActivationId());

        final var onboardingProcessId = onboardingProcess.getId();
        final var identityVerification = identityVerificationService.findByProcessIdOptional(onboardingProcessId)
                .orElse(null);

        response.setProcessId(onboardingProcessId);
        response.setProcessType(onboardingProcess.getProcessConfiguration().getProcessType());
        response.setConsentRequired(isConsentPending(onboardingProcess));

        // Check for expiration of onboarding process before returning any status,
        // including the NOT_INITIALIZED case, to avoid returning NOT_INITIALIZED for an expired process.
        if (onboardingService.hasProcessExpired(onboardingProcess)) {
            response.setIdentityVerificationStatus(FAILED);
            response.setIdentityVerificationPhase(IdentityVerificationPhase.COMPLETED);
            return response;
        }

        if (identityVerification == null) {
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            return response;
        }

        // Check activation flags, the mobile application needs to start over after cleanup or reaching attempts limit
        if (containsActivationFlagVerificationPending(ownerId)) {
            // Initialization is required because verification is not in progress for current identity verification
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            return response;
        }

        response.setIdentityVerificationStatus(identityVerification.getStatus());
        response.setIdentityVerificationPhase(identityVerification.getPhase());
        response.setRejectReason(identityVerification.getRejectReason());
        return response;
    }

    private boolean containsActivationFlagVerificationPending(final OwnerId ownerId) throws RemoteCommunicationException {
        return activationFlagService.containsActivationFlagVerificationPending(ownerId.getActivationId());
    }

    /**
     * Checks whether consent is pending for the given onboarding process.
     *
     * @param onboardingProcess the onboarding process to check
     * @return {@code true} if consent is required and still pending (not yet accepted);
     *         {@code false} if consent is not required or has already been accepted
     * @throws IllegalArgumentException if no configuration is found for the given onboarding process
     */
    public boolean isConsentPending(final OnboardingProcessEntity onboardingProcess) {
        final var processConfig = Optional.of(onboardingProcess)
                .map(OnboardingProcessEntity::getProcessConfiguration)
                .map(OnboardingProcessConfigurationEntity::getConfiguration)
                .orElseThrow(() -> new IllegalArgumentException("Configuration not found for process ID: " + onboardingProcess.getId()));

        return processConfig.consentRequired() && Boolean.FALSE.equals(onboardingProcess.getConsentAccepted());
    }
}
