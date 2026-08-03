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
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.TooManyProcessesException;
import com.wultra.app.onboardingserver.impl.service.OnboardingServiceImpl;
import com.wultra.core.http.common.request.RequestContext;
import com.wultra.core.http.common.request.RequestContextConverter;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuth;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.*;

/**
 * Controller publishing REST services for the onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@RestController
@RequestMapping(value = "api/onboarding")
@AllArgsConstructor
@Slf4j
public class OnboardingController {

    private final OnboardingServiceImpl onboardingService;

    /**
     * Start an onboarding process.
     * <p>
     * The request is always encrypted using application-scope ECIES encryption.
     * For a standard onboarding process, no PowerAuth signature is required and a new activation is initialized according to the process
     * configuration.
     * A process configured with {@code existingActivation=true} is a re-KYC process.
     * It requires a valid possession signature from an active activation. The process uses the activation and user ID from
     * the verified signature, does not create a new activation, and returns {@code activationType=ACTIVATION_ALREADY_EXIST}.
     *
     * @param request Start the onboarding process request.
     * @param encryptionContext Encryption context.
     * @param apiAuthentication PowerAuth authentication context; required only for a process configured with
     *                          {@code existingActivation=true}.
     * @param servletRequest HttpServletRequest.
     * @return Start onboarding process response.
     * @throws PowerAuthEncryptionException Thrown when request is invalid.
     * @throws OnboardingProcessException Thrown in case onboarding process fails.
     * @throws OnboardingOtpDeliveryException Thrown in case onboarding OTP delivery fails.
     * @throws TooManyProcessesException Thrown in case too many onboarding processes are started.
     * @throws InvalidRequestObjectException Thrown in case request is invalid.
     */
    @PostMapping("start")
    @Operation(
            summary = "Start an onboarding process",
            description = """
                    Starts an application-encrypted onboarding process.

                    A standard process initializes a new activation according to its configuration. A process
                    configured with `existingActivation=true` starts re-KYC for the active activation identified
                    by the required `POSSESSION` PowerAuth signature. In this mode no new activation is created,
                    and the response contains `activationType=ACTIVATION_ALREADY_EXIST`.
                    """
    )
    @PowerAuthEncryption(scope = EncryptionScope.APPLICATION_SCOPE)
    @PowerAuth(resourceId = "/api/onboarding/start", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public ObjectResponse<OnboardingStartResponse> startOnboarding(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<OnboardingStartRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext,
            @Parameter(hidden = true) final PowerAuthApiAuthentication apiAuthentication,
            final HttpServletRequest servletRequest) throws OnboardingProcessException, OnboardingOtpDeliveryException, PowerAuthEncryptionException, TooManyProcessesException, InvalidRequestObjectException, RemoteCommunicationException {

        final OnboardingStartRequest requestObject = request.getRequestObject();
        logger.info("Start initiated", action("start"), stateInitiated(), kv("processType", requestObject.processType()));
        // Check if the request was correctly decrypted
        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES decryption failed during onboarding");
        }

        final RequestContext requestContext = RequestContextConverter.convert(servletRequest);

        final OnboardingStartResponse response = onboardingService.startOnboarding(OnboardingServiceImpl.StartOnboardingContext.builder()
                .request(requestObject)
                .requestContext(requestContext)
                .encryptionContext(encryptionContext)
                .apiAuthentication(apiAuthentication)
                .build());
        logger.info("Start succeeded", action("start"), stateSucceeded());
        return new ObjectResponse<>(response);
    }

    /**
     * Resend an onboarding OTP code.
     *
     * @param request Resend an OTP code request.
     * @param encryptionContext Encryption context.
     * @return Response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process fails.
     * @throws OnboardingOtpDeliveryException Thrown when onboarding OTP delivery fails.
     */
    @PostMapping("otp/resend")
    @PowerAuthEncryption(scope = EncryptionScope.APPLICATION_SCOPE)
    public Response resendOtp(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<OnboardingOtpResendRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext)
            throws PowerAuthEncryptionException, OnboardingProcessException, OnboardingOtpDeliveryException {

        final OnboardingOtpResendRequest requestObject = request.getRequestObject();
        logger.info("Resend otp initiated", action("resendOtp"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        // Check if the request was correctly decrypted
        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES decryption failed while resending OTP code");
        }

        final Response response = onboardingService.resendOtp(requestObject);
        logger.info("Resend otp succeeded", action("resendOtp"), stateSucceeded());
        return response;
    }

    /**
     * Get onboarding process status.
     *
     * @param request Onboarding status request.
     * @param encryptionContext Encryption context.
     * @return Onboarding status response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @PostMapping("status")
    @PowerAuthEncryption(scope = EncryptionScope.APPLICATION_SCOPE)
    public ObjectResponse<OnboardingStatusResponse> getStatus(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<OnboardingStatusRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext) throws PowerAuthEncryptionException, OnboardingProcessException {

        final OnboardingStatusRequest requestObject = request.getRequestObject();
        logger.info("Status initiated", action("status"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        // Check if the request was correctly decrypted
        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES decryption failed while getting status");
        }

        logger.debug("Onboarding process will not be locked, {}", requestObject.getProcessId(), kv("processId", requestObject.getProcessId()));
        final OnboardingStatusResponse response = onboardingService.getStatus(requestObject);
        logger.info("Status succeeded", action("status"), stateSucceeded());
        return new ObjectResponse<>(response);
    }

    /**
     * Perform cleanup related to an onboarding process.
     *
     * @param request Onboarding cleanup request.
     * @param encryptionContext Encryption context.
     * @return Onboarding cleanup response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @PostMapping("cleanup")
    @PowerAuthEncryption(scope = EncryptionScope.APPLICATION_SCOPE)
    public Response performCleanup(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<OnboardingCleanupRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext) throws PowerAuthEncryptionException, OnboardingProcessException {

        final OnboardingCleanupRequest requestObject = request.getRequestObject();
        logger.info("Cleanup initiated", action("cleanup"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        // Check if the request was correctly decrypted
        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES decryption failed during cleanup");
        }

        final Response response = onboardingService.performCleanup(requestObject);
        logger.info("Cleanup succeeded", action("cleanup"), stateSucceeded());
        return response;
    }
}
