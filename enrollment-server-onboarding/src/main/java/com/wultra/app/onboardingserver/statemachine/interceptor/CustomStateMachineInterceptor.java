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

import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckNotEnabledException;
import com.wultra.app.onboardingserver.statemachine.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import io.getlime.core.rest.model.base.response.ErrorResponse;
import io.getlime.core.rest.model.base.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.stereotype.Component;

/**
 * Custom interceptor to handle state machine errors
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class CustomStateMachineInterceptor extends StateMachineInterceptorAdapter<EnrollmentState, EnrollmentEvent> {

    private static final Logger logger = LoggerFactory.getLogger(CustomStateMachineInterceptor.class);

    @Override
    public Exception stateMachineError(StateMachine<EnrollmentState, EnrollmentEvent> stateMachine, Exception e) {
        HttpStatus status;
        Response response;

        if (e instanceof OnboardingProcessException) {
            logger.warn("Onboarding process failed", e);
            response = new ErrorResponse("ONBOARDING_FAILED", "Onboarding process failed.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof OnboardingOtpDeliveryException) {
            logger.warn("Onboarding process failed", e);
            response = new ErrorResponse("ONBOARDING_OTP_FAILED", "Onboarding OTP delivery failed.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof PresenceCheckNotEnabledException) {
            logger.warn("Presence check transition with a not enabled presence check service", e);
            response = new ErrorResponse("PRESENCE_CHECK_NOT_ENABLED", "Presence check is not enabled.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof PresenceCheckException) {
            logger.warn("Presence check failed", e);
            response = new ErrorResponse("PRESENCE_CHECK_FAILED", "Presence check failed.");
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof DocumentVerificationException) {
            logger.warn("Document verification failed", e);
            response = new ErrorResponse("DOCUMENT_VERIFICATION_FAILED", "Document verification failed.");
            status = HttpStatus.BAD_REQUEST;
        } else {
            logger.error("Error occurred in a state machine.", e);
            response = new ErrorResponse("ERROR_GENERIC", "Unknown error occurred while processing a request.");
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        stateMachine.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_OBJECT, response);
        stateMachine.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_STATUS, status);

        return e;
    }

}
