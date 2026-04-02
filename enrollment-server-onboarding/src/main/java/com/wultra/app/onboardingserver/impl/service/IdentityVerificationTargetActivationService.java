/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.CreateTargetActivationRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.CreateTargetActivationResponse;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.impl.util.PowerAuthUtil;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import com.wultra.security.powerauth.client.model.response.InitActivationResponse;
import com.wultra.security.powerauth.client.model.response.v4.GetActivationStatusResponse;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating target activation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class IdentityVerificationTargetActivationService {

    private final OnboardingServiceImpl onboardingService;

    private final IdentityVerificationService identityVerificationService;

    private final LookupUserService lookupUserService;

    private final ActivationService activationService;

    private final AuditService auditService;

    /**
     * Create target activation.
     *
     * @param request request
     * @param apiAuthentication API authentication.
     * @return response
     * @throws OnboardingProcessException in case of any business error
     */
    @Transactional
    public CreateTargetActivationResponse createTargetActivation(final CreateTargetActivationRequest request, final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, RemoteCommunicationException {
        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.processId();

        final OnboardingProcessEntity process = onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);
        validateIdentityVerificationPhase(ownerId);

        final String userId = lookupUserService.lookupUser(process, request.identification())
                .orElseThrow(() -> new OnboardingProcessException("Unable to lookup user for " + ownerId));

        logger.info("Updating processId: {}, current userId: {} to a new userId: {}", processId, process.getUserId(), userId);
        process.setUserId(userId);
        onboardingService.updateProcess(process);

        final var initActivationContext = ActivationService.InitTargetActivationContext.builder()
                .applicationId(apiAuthentication.getApplicationId())
                .userId(userId)
                .parentActivationId(apiAuthentication.getActivationContext().getActivationId())
                .build();

        final String activationCode = fetchActivationCode(process, initActivationContext);

        return CreateTargetActivationResponse.builder()
                .activationCode(activationCode)
                .build();
    }

    private String fetchActivationCode(final OnboardingProcessEntity process, final ActivationService.InitTargetActivationContext initActivationContext) throws OnboardingProcessException, RemoteCommunicationException {
        final String targetActivationId = process.getTargetActivationId();
        if (targetActivationId == null) {
            return createActivationAndUpdateProcess(initActivationContext, process);
        } else {
            final GetActivationStatusResponse activationStatusResponse = activationService.fetchActivationStatusResponse(targetActivationId);
            final ActivationStatus activationStatus = activationStatusResponse.getActivationStatus();
            if (activationStatus == ActivationStatus.CREATED) {
                return activationStatusResponse.getActivationCode();
            } else if (activationStatus == ActivationStatus.REMOVED) {
                return createActivationAndUpdateProcess(initActivationContext, process);
            } else if (activationStatus == ActivationStatus.PENDING_COMMIT) {
                logger.info("Target activationId: {} is in PENDING_COMMIT state, removing it and creating a new one", targetActivationId);
                activationService.removeActivation(targetActivationId);
                auditService.auditActivation(process, targetActivationId,"Remove activation for user: {}", process.getUserId());
                return createActivationAndUpdateProcess(initActivationContext, process);
            } else {
                throw new OnboardingProcessException("Unexpected activation status: " + activationStatus);
            }
        }
    }

    private String createActivationAndUpdateProcess(final ActivationService.InitTargetActivationContext initActivationContext, final OnboardingProcessEntity process) throws RemoteCommunicationException {
        final InitActivationResponse response = activationService.initTargetActivation(initActivationContext);
        process.setTargetActivationId(response.getActivationId());
        onboardingService.updateProcess(process);
        auditService.auditActivation(process, response.getActivationId(), "Create target activation for user: {}", process.getUserId());
        return response.getActivationCode();
    }

    private void validateIdentityVerificationPhase(final OwnerId ownerId) throws OnboardingProcessException {
        final IdentityVerificationEntity identityVerification = identityVerificationService.findByOptional(ownerId)
                .orElseThrow(() -> new OnboardingProcessException("Unable to find identity verification for " + ownerId));

        if (identityVerification.getPhase() != IdentityVerificationPhase.ACTIVATION_FINISH) {
            throw new OnboardingProcessException("Identity verification ID: %s is in phase %s but expected ACTIVATION_FINISH"
                    .formatted(identityVerification.getId(), identityVerification.getPhase()));
        }
    }

    /**
     * Check if target activation is finished for the given process ID.
     *
     * @param processId Process ID.
     * @return whether target activation is finished for the given process ID
     * @throws RemoteCommunicationException Thrown when communication with the PowerAuth server fails.
     */
    @Transactional(readOnly = true)
    public boolean isTargetActivationFinished(final String processId) throws RemoteCommunicationException {
        final String targetActivationId;
        try {
            targetActivationId = onboardingService.findProcess(processId).getTargetActivationId();
        } catch (OnboardingProcessException e) {
            logger.warn("Unable to find processId: {}", processId, e);
            return false;
        }

        if (targetActivationId == null) {
            logger.debug("Target activation ID is null for processId: {}", processId);
            return false;
        }

        return isActivationValid(targetActivationId);
    }

    private boolean isActivationValid(final String activationId) throws RemoteCommunicationException {
        final ActivationStatus activationStatus = activationService.fetchActivationStatus(activationId);
        return activationStatus == ActivationStatus.ACTIVE;
    }

    /**
     * Check if target activation is enabled for the given process ID.
     *
     * @param processId Process ID.
     * @return {@code true} if the temporary target activation feature is enabled in the process configuration,
     *         {@code false} otherwise
     * @throws OnboardingProcessException if the onboarding process cannot be found or its configuration cannot be read.
     */
    @Transactional(readOnly = true)
    public boolean isTargetActivationEnabled(final String processId) throws OnboardingProcessException {
        return onboardingService.findProcess(processId)
                .getProcessConfiguration()
                .getConfiguration()
                .useTemporaryActivation();
    }
}
