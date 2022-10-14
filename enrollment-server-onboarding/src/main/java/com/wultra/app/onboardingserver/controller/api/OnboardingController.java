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
package com.wultra.app.onboardingserver.controller.api;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.OnboardingCleanupRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.OnboardingOtpResendRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.OnboardingStartRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.OnboardingStatusRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.OnboardingStartResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.OnboardingStatusResponse;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.TooManyProcessesException;
import com.wultra.app.onboardingserver.impl.service.OnboardingServiceImpl;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.encryptor.ecies.model.EciesScope;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller publishing REST services for the onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server-onboarding.onboarding-process.enabled",
        havingValue = "true"
)
@RestController
@RequestMapping(value = "api/onboarding")
public class OnboardingController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingController.class);

    private final OnboardingServiceImpl onboardingService;

    /**
     * Controller constructor.
     * @param onboardingService Onboarding service.
     */
    @Autowired
    public OnboardingController(OnboardingServiceImpl onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * Start an onboarding process.
     *
     * @param request Start onboarding process request.
     * @param eciesContext ECIES context.
     * @return Start onboarding process response.
     * @throws PowerAuthEncryptionException Thrown when request is invalid.
     * @throws OnboardingProcessException Thrown in case onboarding process fails.
     * @throws OnboardingOtpDeliveryException Thrown in case onboarding OTP delivery fails.
     * @throws TooManyProcessesException Thrown in case too many onboarding processes are started.
     * @throws InvalidRequestObjectException Thrown in case request is invalid.
     */
    @PostMapping("start")
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public ObjectResponse<OnboardingStartResponse> startOnboarding(@EncryptedRequestBody ObjectRequest<OnboardingStartRequest> request,
                                                                   @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws OnboardingProcessException, OnboardingOtpDeliveryException, PowerAuthEncryptionException, TooManyProcessesException, InvalidRequestObjectException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed during onboarding");
            throw new PowerAuthEncryptionException("ECIES decryption failed during onboarding");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received during onboarding");
            throw new PowerAuthEncryptionException("Invalid request received during onboarding");
        }

        OnboardingStartResponse response = onboardingService.startOnboarding(request.getRequestObject());
        return new ObjectResponse<>(response);
    }

    /**
     * Resend an onboarding OTP code.
     *
     * @param request Resend an OTP code request.
     * @param eciesContext ECIES context.
     * @return Response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process fails.
     * @throws OnboardingOtpDeliveryException Thrown when onboarding OTP delivery fails.
     */
    @PostMapping("otp/resend")
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public Response resendOtp(@EncryptedRequestBody ObjectRequest<OnboardingOtpResendRequest> request,
                              @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthEncryptionException, OnboardingProcessException, OnboardingOtpDeliveryException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed during onboarding");
            throw new PowerAuthEncryptionException("ECIES decryption failed while resending OTP code");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received during onboarding");
            throw new PowerAuthEncryptionException("Invalid request received while resending OTP code");
        }

        return onboardingService.resendOtp(request.getRequestObject());
    }

    /**
     * Get onboarding process status.
     *
     * @param request Onboarding status request.
     * @param eciesContext ECIES context.
     * @return Onboarding status response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @PostMapping("status")
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public ObjectResponse<OnboardingStatusResponse> getStatus(@EncryptedRequestBody ObjectRequest<OnboardingStatusRequest> request,
                                                              @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthEncryptionException, OnboardingProcessException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed during onboarding");
            throw new PowerAuthEncryptionException("ECIES decryption failed while getting status");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received during onboarding");
            throw new PowerAuthEncryptionException("Invalid request received while getting status");
        }

        logger.debug("Onboarding process will not be locked, {}", request.getRequestObject().getProcessId());
        OnboardingStatusResponse response = onboardingService.getStatus(request.getRequestObject());
        return new ObjectResponse<>(response);
    }

    /**
     * Perform cleanup related to an onboarding process.
     *
     * @param request Onboarding cleanup request.
     * @param eciesContext ECIES context.
     * @return Onboarding cleanup response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @PostMapping("cleanup")
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public Response performCleanup(@EncryptedRequestBody ObjectRequest<OnboardingCleanupRequest> request,
                                   @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthEncryptionException, OnboardingProcessException {
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed during onboarding");
            throw new PowerAuthEncryptionException("ECIES decryption failed during onboarding");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received during onboarding");
            throw new PowerAuthEncryptionException("Invalid request received during onboarding");
        }

        return onboardingService.performCleanup(request.getRequestObject());
    }
}
