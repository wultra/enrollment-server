/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine.interceptor;

import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.statemachine.EnrollmentStateProvider;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import io.getlime.core.rest.model.base.response.ErrorResponse;
import io.getlime.core.rest.model.base.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom interceptor to handle state machine errors
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class CustomStateMachineInterceptor extends StateMachineInterceptorAdapter<OnboardingState, OnboardingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(CustomStateMachineInterceptor.class);

    private final EnrollmentStateProvider enrollmentStateProvider;

    @Autowired
    public CustomStateMachineInterceptor(EnrollmentStateProvider enrollmentStateProvider) {
        this.enrollmentStateProvider = enrollmentStateProvider;
    }

    @Override
    public Exception stateMachineError(StateMachine<OnboardingState, OnboardingEvent> stateMachine, Exception e) {
        HttpStatus status;
        Response response;

        if (e instanceof OnboardingProcessException) {
            logger.warn("Onboarding process failed: {}", e.getMessage());
            logger.debug("Onboarding process failed", e);
            response = new ErrorResponse("ONBOARDING_FAILED", "Onboarding process failed.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof OnboardingOtpDeliveryException) {
            logger.warn("Onboarding process failed: {}", e.getMessage());
            logger.debug("Onboarding process failed", e);
            response = new ErrorResponse("ONBOARDING_OTP_FAILED", "Onboarding OTP delivery failed.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof PresenceCheckNotEnabledException) {
            logger.warn("Presence check transition with a not enabled presence check service: {}", e.getMessage());
            logger.debug("Presence check transition with a not enabled presence check service", e);
            response = new ErrorResponse("PRESENCE_CHECK_NOT_ENABLED", "Presence check is not enabled.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof PresenceCheckException) {
            logger.warn("Presence check failed: {}", e.getMessage());
            logger.debug("Presence check failed", e);
            response = new ErrorResponse("PRESENCE_CHECK_FAILED", "Presence check failed.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof PresenceCheckLimitException) {
            logger.warn("Presence check limit reached: {}", e.getMessage());
            logger.debug("Presence check limit reached", e);
            response = new ErrorResponse("PRESENCE_CHECK_LIMIT_REACHED", "Presence check limit reached.");
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if (e instanceof DocumentVerificationException) {
            logger.warn("Document verification failed: {}", e.getMessage());
            logger.debug("Document verification failed", e);
            response = new ErrorResponse("DOCUMENT_VERIFICATION_FAILED", "Document verification failed.");
            status = HttpStatus.BAD_REQUEST;
        } else {
            logger.error("Error occurred in a state machine.", e);
            response = new ErrorResponse("ERROR_GENERIC", "Unknown error occurred while processing a request.");
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        final Map<Object, Object> variables = stateMachine.getExtendedState().getVariables();
        variables.put(ExtendedStateVariable.RESPONSE_OBJECT, response);
        variables.put(ExtendedStateVariable.RESPONSE_STATUS, status);

        return e;
    }

    @Override
    public StateContext<OnboardingState, OnboardingEvent> postTransition(StateContext<OnboardingState, OnboardingEvent> context) {
        IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        OnboardingState targetState = context.getTarget().getId();
        if (targetState.isChoiceState()) {
            logger.debug("Transition to a choice state {} for {}", targetState, identityVerification);
            return context;
        }

        if (OnboardingState.UNEXPECTED_STATE == targetState) {
            logger.debug("Transition to unexpected state for {}", identityVerification);
            return context;
        }

        OnboardingState expectedState;
        try {
            expectedState = enrollmentStateProvider.findByPhaseAndStatus(identityVerification.getPhase(), identityVerification.getStatus());
        } catch (IdentityVerificationException e) {
            logger.error("Failed post transition check: {}, {}", e.getMessage(), identityVerification);
            return context;
        }

        if (expectedState != targetState) {
            logger.error("Unexpected targetState={} when expectedState={}, {}", targetState, expectedState, identityVerification);
        } else {
            logger.debug("Transition to targetState={}, {}", targetState, identityVerification);
        }

        return context;
    }

}
