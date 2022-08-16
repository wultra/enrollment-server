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
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import com.wultra.app.onboardingserver.impl.service.internal.JsonSerializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationStatusService.class);

    private final IdentityVerificationService identityVerificationService;

    private final JsonSerializationService jsonSerializationService;

    private final PresenceCheckService presenceCheckService;

    private final IdentityVerificationFinishService identityVerificationFinishService;

    private final OnboardingServiceImpl onboardingService;

    private final IdentityVerificationOtpService identityVerificationOtpService;

    private final ActivationFlagService activationFlagService;

    private final StateMachineService stateMachineService;

    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    /**
     * Service constructor.
     *
     * @param identityVerificationService       Identity verification service.
     * @param jsonSerializationService          JSON serialization service.
     * @param presenceCheckService              Presence check service.
     * @param identityVerificationFinishService Identity verification finish service.
     * @param onboardingService                 Onboarding service.
     * @param identityVerificationOtpService    Identity verification OTP service.
     * @param activationFlagService             Activation flags service.
     * @param stateMachineService               State machine service.
     */
    @Autowired
    public IdentityVerificationStatusService(
            IdentityVerificationService identityVerificationService,
            JsonSerializationService jsonSerializationService,
            PresenceCheckService presenceCheckService,
            IdentityVerificationFinishService identityVerificationFinishService,
            OnboardingServiceImpl onboardingService,
            IdentityVerificationOtpService identityVerificationOtpService,
            ActivationFlagService activationFlagService,
            StateMachineService stateMachineService) {
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckService = presenceCheckService;
        this.identityVerificationFinishService = identityVerificationFinishService;
        this.onboardingService = onboardingService;
        this.identityVerificationOtpService = identityVerificationOtpService;
        this.activationFlagService = activationFlagService;
        this.stateMachineService = stateMachineService;
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
            response.setIdentityVerificationStatus(FAILED);
            response.setIdentityVerificationPhase(null);
            return response;
        }

        // Check activation flags, the identity verification entity may need to be re-initialized after cleanup
        final List<String> flags = activationFlagService.listActivationFlags(ownerId);
        if (!flags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
            // Initialization is required because verification is not in progress for current identity verification
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            return response;
        }

// TODO new code
//        if (IdentityVerificationPhase.DOCUMENT_VERIFICATION.equals(idVerification.getPhase())
//                && IdentityVerificationStatus.ACCEPTED.equals(idVerification.getStatus())) {
//            logger.debug("Finished verification of documents for {}, {}", idVerification, ownerId);
//            continueWithClientEvaluation(ownerId, idVerification);
//        }
//
//        if (IdentityVerificationPhase.CLIENT_EVALUATION.equals(idVerification.getPhase())
//                && IdentityVerificationStatus.ACCEPTED.equals(idVerification.getStatus())) {
//            logger.debug("Finished evaluation of client for {}, {}", idVerification, ownerId);
//            continueWithPresenceCheck(ownerId, idVerification);
//        }

        StateMachine<OnboardingState, OnboardingEvent> state =
                stateMachineService.processStateMachineEvent(ownerId, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        idVerification = state.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        response.setIdentityVerificationStatus(idVerification.getStatus());
        response.setIdentityVerificationPhase(idVerification.getPhase());
        return response;
    }

//    private void continueWithClientEvaluation(final OwnerId ownerId, final IdentityVerificationEntity idVerification) {
//        idVerification.setPhase(IdentityVerificationPhase.CLIENT_EVALUATION);
//        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
//        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
//        logger.info("Switched to CLIENT_EVALUATION/IN_PROGRESS; {}, process ID: {}", ownerId, idVerification.getProcessId());
//    }

}
