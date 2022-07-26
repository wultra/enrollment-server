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
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsRequest;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Service implementing document identity verification status services.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationStatusService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationStatusService.class);

    private final IdentityVerificationService identityVerificationService;

    private final OnboardingServiceImpl onboardingService;

    private final HttpCustomizationService httpCustomizationService;

    private final StateMachineService stateMachineService;

    private final PowerAuthClient powerAuthClient;

    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    /**
     * Service constructor.
     * @param identityVerificationService       Identity verification service.
     * @param onboardingService                 Onboarding service.
     * @param httpCustomizationService          HTTP customization service.
     * @param powerAuthClient                   PowerAuth client.
     */
    @Autowired
    public IdentityVerificationStatusService(
            IdentityVerificationService identityVerificationService,
            OnboardingServiceImpl onboardingService,
            HttpCustomizationService httpCustomizationService,
            StateMachineService stateMachineService,
            PowerAuthClient powerAuthClient) {
        this.identityVerificationService = identityVerificationService;
        this.onboardingService = onboardingService;
        this.httpCustomizationService = httpCustomizationService;
        this.stateMachineService = stateMachineService;
        this.powerAuthClient = powerAuthClient;
    }

    /**
     * Check status of identity verification.
     *
     * @param request Identity verification status request.
     * @param ownerId Owner identifier.
     * @return Identity verification status response.
     * @throws IdentityVerificationException  Thrown when identity verification could not be started.
     * @throws RemoteCommunicationException   Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException     Thrown when onboarding process is invalid.
     * @throws OnboardingOtpDeliveryException Thrown when OTP could not be sent when changing status.
     */
    @Transactional
    public IdentityVerificationStatusResponse checkIdentityVerificationStatus(IdentityVerificationStatusRequest request, OwnerId ownerId) throws IdentityVerificationException, RemoteCommunicationException, OnboardingProcessException, OnboardingOtpDeliveryException {
        IdentityVerificationStatusResponse response = new IdentityVerificationStatusResponse();

        Optional<IdentityVerificationEntity> idVerificationOptional = identityVerificationService.findByOptional(ownerId);

        final OnboardingProcessEntity onboardingProcess = onboardingService.findProcessByActivationId(ownerId.getActivationId());
        if (idVerificationOptional.isEmpty()) {
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            response.setProcessId(onboardingProcess.getId());
            return response;
        }

        IdentityVerificationEntity idVerification = idVerificationOptional.get();
        response.setProcessId(idVerification.getProcessId());

        // Check for expiration of onboarding process
        if (onboardingService.hasProcessExpired(onboardingProcess)) {
            // Trigger immediate processing of expired processes
            onboardingService.terminateInactiveProcesses();
            response.setIdentityVerificationStatus(IdentityVerificationStatus.FAILED);
            response.setIdentityVerificationPhase(null);
            return response;
        }

        // Check activation flags, the identity verification entity may need to be re-initialized after cleanup
        try {
            final ListActivationFlagsRequest listRequest = new ListActivationFlagsRequest();
            listRequest.setActivationId(ownerId.getActivationId());
            final ListActivationFlagsResponse flagResponse = powerAuthClient.listActivationFlags(
                    listRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );
            List<String> flags = flagResponse.getActivationFlags();
            if (!flags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
                // Initialization is required because verification is not in progress for current identity verification
                response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
                response.setIdentityVerificationPhase(null);
                return response;
            }
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }

        StateMachine<EnrollmentState, EnrollmentEvent> state =
                stateMachineService.processStateMachineEvent(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        idVerification = state.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        response.setIdentityVerificationStatus(idVerification.getStatus());
        response.setIdentityVerificationPhase(idVerification.getPhase());
        return response;
    }

}
