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
package com.wultra.app.enrollmentserver.controller.api;

import com.wultra.app.enrollmentserver.model.request.*;
import com.wultra.app.enrollmentserver.model.response.OnboardingStartResponse;
import com.wultra.app.enrollmentserver.model.response.OnboardingStatusResponse;
import com.wultra.app.enrollmentserver.model.response.VerifyOtpResponse;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.encryptor.ecies.model.EciesScope;
import io.getlime.security.powerauth.rest.api.base.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.base.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller publishing REST services for the onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server.onboarding-process.enabled",
        havingValue = "true"
)
@RestController
@RequestMapping(value = "v1/api/onboarding")
public class OnboardingController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingController.class);

    /**
     * Controller constructor.
     */
    @Autowired
    public OnboardingController() {
    }

    /**
     * Start an onboarding process.
     *
     * @param request Start onboarding process request.
     * @param eciesContext ECIES context.
     * @return Start onboarding process response.
     * @throws PowerAuthAuthenticationException Thrown when request is invalid.
     */
    @RequestMapping(value = "start", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public ObjectResponse<OnboardingStartResponse> startOnboarding(@EncryptedRequestBody ObjectRequest<OnboardingStartRequest> request,
                                                                   @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthAuthenticationException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when submitting documents for verification");
        }

        // TODO
        return null;
    }

    /**
     * Resend an onboarding OTP code.
     *
     * @param request Resend an OTP code request.
     * @param eciesContext ECIES context.
     * @return Response.
     * @throws PowerAuthAuthenticationException Thrown when request encryption fails.
     */
    @RequestMapping(value = "otp/resend", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public Response resendOtp(@EncryptedRequestBody ObjectRequest<ResendOtpRequest> request,
                              @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthAuthenticationException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when submitting documents for verification");
        }

        // TODO
        return null;
    }

    /**
     * Verify an onboarding OTP code.
     *
     * @param request Verify OTP code request.
     * @param eciesContext ECIES context.
     * @return Verify OTP code response.
     * @throws PowerAuthAuthenticationException Thrown when request encryption fails.
     */
    @RequestMapping(value = "otp/verify", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public ObjectResponse<VerifyOtpResponse> verifyOtp(@EncryptedRequestBody ObjectRequest<VerifyOtpRequest> request,
                                                       @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthAuthenticationException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when submitting documents for verification");
        }

        // TODO
        return null;
    }

    /**
     * Get onboarding process status.
     *
     * @param request Onboarding status request.
     * @param eciesContext ECIES context.
     * @return Onboarding status response.
     * @throws PowerAuthAuthenticationException Thrown when request encryption fails.
     */
    @RequestMapping(value = "status", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public ObjectResponse<OnboardingStatusResponse> getStatus(@EncryptedRequestBody ObjectRequest<OnboardingStatusRequest> request,
                                                              @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthAuthenticationException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when submitting documents for verification");
        }

        // TODO
        return null;
    }

    /**
     * Perform cleanup related to an onboarding process.
     *
     * @param request Onboarding cleanup request.
     * @param eciesContext ECIES context.
     * @return Onboarding cleanup response.
     * @throws PowerAuthAuthenticationException Thrown when request encryption fails.
     */
    @RequestMapping(value = "cleanup", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public Response performCleanup(@EncryptedRequestBody ObjectRequest<OnboardingCleanupRequest> request,
                                   @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthAuthenticationException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when submitting documents for verification");
        }

        // TODO
        return null;
    }

}