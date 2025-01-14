/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.errorhandling;

import com.wultra.security.powerauth.lib.mtoken.model.enumeration.ErrorCode;
import io.getlime.core.rest.model.base.response.ErrorResponse;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthApplicationConfigurationException;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Exception handler for RESTful API issues.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ControllerAdvice
@Slf4j
public class DefaultExceptionHandler {

    /**
     * Default exception handler, for unexpected errors.
     * @param t Throwable.
     * @return Response with error details.
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorResponse handleDefaultException(Throwable t) {
        logger.error("Error occurred when processing the request.", t);
        return new ErrorResponse("ERROR_GENERIC", "Unknown error occurred while processing request.");
    }

    /**
     * Handling of remote communication exception.
     *
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(RemoteCommunicationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public @ResponseBody ErrorResponse handleRemoteExceptionException(RemoteCommunicationException ex) {
        logger.warn("Communication with remote system failed", ex);
        return new ErrorResponse("REMOTE_COMMUNICATION_ERROR", "Communication with remote system failed.");
    }

    /**
     * Exception handler for invalid request exception.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(InvalidRequestObjectException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleInvalidRequestException(InvalidRequestObjectException ex) {
        logger.warn("Error occurred when processing request object.", ex);
        return new ErrorResponse(ErrorCode.INVALID_REQUEST, "Invalid request object.");
    }

    /**
     * Exception handler for invalid request exception.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(PushRegistrationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handlePushRegistrationException(PushRegistrationFailedException ex) {
        logger.warn("Error occurred when registering to push server.", ex);
        return new ErrorResponse(ErrorCode.PUSH_REGISTRATION_FAILED, "Push registration failed in Mobile Token API component.");
    }

    /**
     * Handling of unauthorized exception.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(PowerAuthAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public @ResponseBody ErrorResponse handleUnauthorizedException(PowerAuthAuthenticationException ex) {
        logger.warn("Unable to verify device registration - authentication failed.", ex);
        return new ErrorResponse(ErrorCode.POWERAUTH_AUTH_FAIL, "Unable to verify device registration.");
    }

    /**
     * Handling of mtoken exceptions.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(MobileTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleMobileTokenException(MobileTokenException ex) {
        logger.warn("Mobile token operation failed: {}", ex.getMessage());
        return new ErrorResponse(ex.getCode(), ex.getMessage());
    }

    /**
     * Handling of mtoken auth exceptions.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(MobileTokenAuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public @ResponseBody ErrorResponse handleMobileTokenAuthException(MobileTokenAuthException ex) {
        logger.warn("Mobile token operation failed due to authorization error: {}", ex.getMessage());
        return new ErrorResponse(ex.getCode(), ex.getMessage());
    }

    /**
     * Handling of mtoken configuration exceptions.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(MobileTokenConfigurationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorResponse handleMobileTokenConfigurationException(MobileTokenConfigurationException ex) {
        logger.warn("Mobile token back-end is incorrectly configured: {}", ex.getMessage());
        return new ErrorResponse(ex.getCode(), ex.getMessage());
    }

    /**
     * Handling of activation code exceptions.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(ActivationCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleActivationCodeException(ActivationCodeException ex) {
        logger.warn("Unable to fetch activation code", ex);
        return new ErrorResponse("ACTIVATION_CODE_FAILED", "Unable to fetch activation code.");
    }

    /**
     * Handling of application configuration exceptions.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(PowerAuthApplicationConfigurationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleActivationCodeException(PowerAuthApplicationConfigurationException ex) {
        logger.warn("Unable to fetch application configuration", ex);
        return new ErrorResponse("APPLICATION_CONFIGURATION_ERROR", "Unable to fetch application configuration.");
    }

    /**
     * Handling of inbox exceptions.
     * @param ex Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(InboxException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleInboxException(InboxException ex) {
        logger.warn("Unable to process inbox request", ex);
        return new ErrorResponse("INBOX_FAILED", "Unable to process inbox request.");
    }

    /**
     * Exception handler for no resource found.
     *
     * @param e Exception.
     * @return Response with error details.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ErrorResponse handleNoResourceFoundException(final NoResourceFoundException e) {
        logger.warn("Error occurred when calling an API: {}", e.getMessage());
        logger.debug("Exception detail: ", e);
        return new ErrorResponse("ERROR_NOT_FOUND", "Resource not found.");
    }
}
