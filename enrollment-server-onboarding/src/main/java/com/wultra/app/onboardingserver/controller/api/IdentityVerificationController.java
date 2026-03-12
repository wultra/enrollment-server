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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.*;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationRestService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationTargetActivationService;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuth;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthActivation;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import com.wultra.security.powerauth.rest.api.spring.exception.authentication.PowerAuthTokenInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.wultra.app.onboardingserver.controller.api.LoggingUtils.extractActivation;
import static com.wultra.app.onboardingserver.controller.api.LoggingUtils.extractRequest;

/**
 * Controller publishing REST services for identity document verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server-onboarding.identity-verification.enabled",
        havingValue = "true"
)
@RestController
@RequestMapping(value = "api/identity")
@AllArgsConstructor
@Slf4j
public class IdentityVerificationController {

    private final IdentityVerificationRestService identityVerificationRestService;

    private final IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    /**
     * Initialize identity verification.
     * @param request Initialize identity verification request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when encryption fails.
     * @throws IdentityVerificationException Thrown when identity verification initialization fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("init")
    @PowerAuth(resourceId = "/api/identity/init", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ResponseEntity<Response> initializeIdentityVerification(@RequestBody ObjectRequest<IdentityVerificationInitRequest> request,
                                                                   @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        logger.info("action: initializeIdentityVerification, state: initiated, processId: {}", extractRequest(request).map(IdentityVerificationInitRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.initializeIdentityVerification(request, apiAuthentication);
        logger.info("action: initializeIdentityVerification, state: succeeded");
        return response;
    }

    /**
     * Check status of identity verification.
     *
     * @param request Document submit request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document submit response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("status")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(@RequestBody ObjectRequest<IdentityVerificationStatusRequest> request,
                                                                                              @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException, OnboardingProcessException {

        logger.info("action: checkIdentityVerificationStatus, state: initiated, activationId: {}", extractActivation(apiAuthentication).map(PowerAuthActivation::getActivationId).orElse(null));
        final var response = identityVerificationRestService.checkIdentityVerificationStatus(request, apiAuthentication);
        logger.info("action: checkIdentityVerificationStatus, state: succeeded, phase: {}, status: {}",
                response.getResponseObject().getIdentityVerificationPhase(), response.getResponseObject().getIdentityVerificationStatus());
        return response;
    }

    /**
     * Submit identity-related documents for verification.
     *
     * @deprecated
     * Use {@code /api/v2/identity/document/submit} instead.
     *
     * @param request Document submit request.
     * @param encryptionContext Encryption context.
     * @return Document submit response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws DocumentSubmitException Thrown when document submission fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     * @throws IdentityVerificationLimitException Thrown in case document upload limit is reached.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown in case identity verification is invalid.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    @Deprecated(since = "2.0.0")
    @PostMapping("document/submit")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public Response submitDocuments(@EncryptedRequestBody ObjectRequest<DocumentSubmitRequest> request,
                                                                  @Parameter(hidden = true) EncryptionContext encryptionContext,
                                                                  @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {

        logger.info("action: submitDocuments, state: initiated, processId: {}", extractRequest(request).map(DocumentSubmitRequest::getProcessId).orElse(null));
        final Response response = identityVerificationRestService.submitDocuments(request, encryptionContext, apiAuthentication);
        logger.info("action: submitDocuments, state: succeeded");
        return response;
    }

    /**
     * Check status of document verification related to identity.
     * @param request Document status request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     */
    @PostMapping("document/status")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ObjectResponse<DocumentStatusResponse> checkDocumentStatus(@RequestBody ObjectRequest<DocumentStatusRequest> request,
                                                                      @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        logger.info("action: checkDocumentStatus, state: initiated, processId: {}", extractRequest(request).map(DocumentStatusRequest::getProcessId).orElse(null));
        final var result = identityVerificationRestService.checkDocumentStatus(request, apiAuthentication);
        logger.info("action: checkDocumentStatus, state: succeeded");
        return result;
    }

    /**
     * Initialize document verification SDK for an integration.
     * @param request Presence check initialization request.
     * @param encryptionContext Encryption context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Verification SDK initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws DocumentVerificationException Thrown when SKD initialization fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    @PostMapping("document/init-sdk")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/document/init-sdk", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ObjectResponse<DocumentVerificationSdkInitResponse> initVerificationSdk(
            @EncryptedRequestBody ObjectRequest<DocumentVerificationSdkInitRequest> request,
            @Parameter(hidden = true) EncryptionContext encryptionContext,
            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PowerAuthEncryptionException, OnboardingProcessException, RemoteCommunicationException {

        logger.info("action: initVerificationSdk, state: initiated, processId: {}", extractRequest(request).map(DocumentVerificationSdkInitRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.initVerificationSdk(request, encryptionContext, apiAuthentication);
        logger.info("action: initVerificationSdk, state: succeeded");
        return response;
    }

    /**
     * Initialize presence check process.
     *
     * @param request Presence check initialization request.
     * @param encryptionContext Encryption context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("presence-check/init")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/presence-check/init", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ResponseEntity<ObjectResponse<PresenceCheckInitResponse>> initPresenceCheck(@EncryptedRequestBody ObjectRequest<PresenceCheckInitRequest> request,
                                                      @Parameter(hidden = true) EncryptionContext encryptionContext,
                                                      @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        logger.info("action: initPresenceCheck, state: initiated, processId: {}", extractRequest(request).map(PresenceCheckInitRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.initPresenceCheck(request, encryptionContext, apiAuthentication);
        logger.info("action: initPresenceCheck, state: succeeded");
        return response;
    }

    /**
     * Submit presence check process.
     *
     * @param request Presence check initialization request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("presence-check/submit")
    @PowerAuth(resourceId = "/api/identity/presence-check/submit", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public ResponseEntity<Response> submitPresenceCheck(@RequestBody ObjectRequest<PresenceCheckSubmitRequest> request,
                                                        @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        logger.info("action: submitPresenceCheck, state: initiated, processId: {}", extractRequest(request).map(PresenceCheckSubmitRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.submitPresenceCheck(request, apiAuthentication);
        logger.info("action: submitPresenceCheck, state: succeeded");
        return response;
    }

    /**
     * Resend OTP code to the user.
     * @param request Presence check initialization request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Send OTP response.
     * @throws IdentityVerificationException Thrown when identity verification is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     */
    @PostMapping("otp/resend")
    @PowerAuth(resourceId = "/api/identity/otp/resend", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public ResponseEntity<Response> resendOtp(
            final @RequestBody ObjectRequest<IdentityVerificationOtpSendRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        logger.info("action: resendOtp, state: initiated, processId: {}", extractRequest(request).map(IdentityVerificationOtpSendRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.resendOtp(request, apiAuthentication);
        logger.info("action: resendOtp, state: succeeded");
        return response;
    }

    /**
     * Verify an OTP code received from the user.
     * @param request Presence check initialization request.
     * @param encryptionContext Encryption context.
     * @return Send OTP response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @PostMapping("otp/verify")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    public ObjectResponse<OtpVerifyResponse> verifyOtp(@EncryptedRequestBody ObjectRequest<IdentityVerificationOtpVerifyRequest> request,
                                                       @Parameter(hidden = true) EncryptionContext encryptionContext)
            throws PowerAuthEncryptionException, OnboardingProcessException {

        logger.info("action: verifyOtp, state: initiated, processId: {}", extractRequest(request).map(IdentityVerificationOtpVerifyRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.verifyOtp(request, encryptionContext);
        logger.info("action: verifyOtp, state: succeeded");
        return response;
    }

    /**
     * Cleanup documents related to identity verification.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when PowerAuth signature verification fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws DocumentVerificationException Thrown when document cleanup fails
     * @throws PresenceCheckException Thrown when presence check cleanup fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     * @throws IdentityVerificationException Thrown when identity verification reset fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    @PostMapping("cleanup")
    @PowerAuth(resourceId = "/api/identity/cleanup", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public Response cleanup(@RequestBody ObjectRequest<IdentityVerificationCleanupRequest> request,
                            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, PresenceCheckException, RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException, OnboardingProcessLimitException {

        logger.info("action: cleanup, state: initiated, processId: {}", extractRequest(request).map(IdentityVerificationCleanupRequest::getProcessId).orElse(null));
        final Response response = identityVerificationRestService.cleanup(request, apiAuthentication);
        logger.info("action: cleanup, state: succeeded");
        return response;
    }

    /**
     * Obtain consent text.
     * @param request Obtain consent text request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Consent text.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     */
    @PostMapping("consent/text")
    @Operation(
            summary = "Obtain consent text",
            description = "Obtain a text of user consent in specified language."
    )
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ObjectResponse<OnboardingConsentTextResponse> fetchConsentText(
            final @RequestBody ObjectRequest<OnboardingConsentTextRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthEncryptionException, PowerAuthTokenInvalidException {

        logger.info("action: fetchConsentText, state: initiated, processId: {}", extractRequest(request).map(OnboardingConsentTextRequest::getProcessId).orElse(null));
        final var response = identityVerificationRestService.fetchConsentText(request, apiAuthentication);
        logger.info("action: fetchConsentText, state: succeeded");
        return response;
    }

    /**
     * Approve or reject consent.
     * @param request Approve consent request
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     */
    @PostMapping("consent/approve")
    @Operation(
            summary = "Store user consent",
            description = "Store user consent, whether approved or not."
    )
    @PowerAuth(resourceId = "/api/identity/consent/approve", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public Response approveConsent(
            final @RequestBody ObjectRequest<OnboardingConsentApprovalRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthAuthenticationException, PowerAuthEncryptionException {

        logger.info("action: approveConsent, state: initiated, processId: {}", extractRequest(request).map(OnboardingConsentApprovalRequest::getProcessId).orElse(null));
        final Response response = identityVerificationRestService.approveConsent(request, apiAuthentication);
        logger.info("action: approveConsent, state: succeeded");
        return response;
    }

    /**
     * Create a target activation.
     *
     * @param request Request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     * @throws OnboardingProcessException Thrown when the onboarding process is not found.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    @PostMapping("activation")
    @Operation(
            summary = "Create a target activation",
            description = "Create a target activation. The identity verification status has to be `ACTIVATION_FINISH`."
    )
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuthToken(authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public ObjectResponse<CreateTargetActivationResponse> createTargetActivation(
            final @EncryptedRequestBody @Valid ObjectRequest<CreateTargetActivationRequest> request,
            final @Parameter(hidden = true) @NotNull PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, RemoteCommunicationException {

        logger.info("action: createTargetActivation, state: initiated, processId: {}", request.getRequestObject().processId());
        final CreateTargetActivationResponse response = identityVerificationTargetActivationService.createTargetActivation(request.getRequestObject(), apiAuthentication);
        logger.info("action: createTargetActivation, state: succeeded");

        return new ObjectResponse<>(response);
    }
}
