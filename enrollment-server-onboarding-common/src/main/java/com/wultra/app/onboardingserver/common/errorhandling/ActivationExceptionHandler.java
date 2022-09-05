/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.common.errorhandling;

import com.wultra.app.enrollmentserver.api.model.onboarding.response.error.ActivationOtpErrorResponse;
import io.getlime.core.rest.model.base.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception handler for activation failures during onboarding.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ControllerAdvice
public class ActivationExceptionHandler {

    private final static Logger logger = LoggerFactory.getLogger(ActivationExceptionHandler.class);

    /**
     * Handle PowerAuthActivationOtpException exceptions.
     * @param ex Exception instance.
     * @return Error response.
     */
    @ExceptionHandler(value = PowerAuthActivationOtpException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleActivationOtpException(PowerAuthActivationOtpException ex) {
        logger.warn(ex.getMessage(), ex);
        return new ActivationOtpErrorResponse(ex.getDefaultCode(), ex.getDefaultError(), ex.getRemainingAttempts());
    }

    /**
     * Handle PowerAuthActivationOtpFailedException exceptions.
     * @param ex Exception instance.
     * @return Error response.
     */
    @ExceptionHandler(value = PowerAuthActivationOtpFailedException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleActivationOtpFailedException(PowerAuthActivationOtpFailedException ex) {
        logger.warn(ex.getMessage(), ex);
        return new ActivationOtpErrorResponse(ex.getDefaultCode(), ex.getDefaultError(), 0);
    }

}
