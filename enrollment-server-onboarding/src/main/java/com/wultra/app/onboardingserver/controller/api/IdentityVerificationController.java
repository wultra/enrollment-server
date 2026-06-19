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
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.DocumentMetadataResponseDto;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wultra.app.onboardingserver.controller.api.LoggingUtils.extractActivationId;
import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.*;

/**
 * Controller publishing REST services for identity document verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
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
     * @throws IdentityVerificationException Thrown when identity verification initialization fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("init")
    @PowerAuth(resourceId = "/api/identity/init", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ResponseEntity<Response> initializeIdentityVerification(
            final @Valid @RequestBody ObjectRequest<IdentityVerificationInitRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, IdentityVerificationException, OnboardingProcessException {

        final IdentityVerificationInitRequest requestObject = request.getRequestObject();
        logger.info("Initialize identity verification initiated", action("initializeIdentityVerification"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.initializeIdentityVerification(requestObject, apiAuthentication);
        logger.info("Initialize identity verification succeeded", action("initializeIdentityVerification"), stateSucceeded());
        return response;
    }

    /**
     * Check status of identity verification.
     *
     * @param request Document submit request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document submit response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("status")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(
            final @Valid @RequestBody ObjectRequest<IdentityVerificationStatusRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, RemoteCommunicationException, OnboardingProcessException {

        logger.info("Check identity verification status initiated", action("checkIdentityVerificationStatus"), stateInitiated(), kv("activationId", extractActivationId(apiAuthentication)));
        final var response = identityVerificationRestService.checkIdentityVerificationStatus(request.getRequestObject(), apiAuthentication);
        logger.info("Check identity verification status succeeded", action("checkIdentityVerificationStatus"), stateSucceeded(), kv("phase", response.getResponseObject().getIdentityVerificationPhase()), kv("status", response.getResponseObject().getIdentityVerificationStatus()));
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
    public Response submitDocuments(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<DocumentSubmitRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext,
            @Parameter(hidden = true) final PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {

        final DocumentSubmitRequest requestObject = request.getRequestObject();
        logger.info("Submit documents initiated", action("submitDocuments"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final Response response = identityVerificationRestService.submitDocuments(requestObject, encryptionContext, apiAuthentication);
        logger.info("Submit documents succeeded", action("submitDocuments"), stateSucceeded());
        return response;
    }

    /**
     * Check status of document verification related to identity.
     * @param request Document status request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     */
    @PostMapping("document/status")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public ObjectResponse<DocumentStatusResponse> checkDocumentStatus(
            final @Valid @RequestBody ObjectRequest<DocumentStatusRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, OnboardingProcessException {

        final DocumentStatusRequest requestObject = request.getRequestObject();
        logger.info("Check document status initiated", action("checkDocumentStatus"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var result = identityVerificationRestService.checkDocumentStatus(requestObject, apiAuthentication);

        logger.info("Check document status succeeded", action("checkDocumentStatus"), stateSucceeded(), kv("statuses", collectDocumentStatuses(result)));
        return result;
    }

    private static Map<String, DocumentStatus> collectDocumentStatuses(final ObjectResponse<DocumentStatusResponse> source) {
        final List<DocumentMetadataResponseDto> documents = source.getResponseObject().getDocuments();
        if (documents == null) {
            return Map.of();
        }

        return documents.stream()
                .collect(Collectors.toMap(DocumentMetadataResponseDto::getId, DocumentMetadataResponseDto::getStatus));
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
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<DocumentVerificationSdkInitRequest> request,
            @Parameter(hidden = true) EncryptionContext encryptionContext,
            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PowerAuthEncryptionException, OnboardingProcessException, RemoteCommunicationException {

        final DocumentVerificationSdkInitRequest requestObject = request.getRequestObject();
        logger.info("Init verification sdk initiated", action("initVerificationSdk"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.initVerificationSdk(requestObject, encryptionContext, apiAuthentication);
        logger.info("Init verification sdk succeeded", action("initVerificationSdk"), stateSucceeded());
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
    public ResponseEntity<ObjectResponse<PresenceCheckInitResponse>> initPresenceCheck(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<PresenceCheckInitRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext,
            @Parameter(hidden = true) final PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final PresenceCheckInitRequest requestObject = request.getRequestObject();
        logger.info("Init presence check initiated", action("initPresenceCheck"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.initPresenceCheck(requestObject, encryptionContext, apiAuthentication);
        logger.info("Init presence check succeeded", action("initPresenceCheck"), stateSucceeded());
        return response;
    }

    /**
     * Submit presence check process.
     *
     * @param request Presence check initialization request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("presence-check/submit")
    @PowerAuth(resourceId = "/api/identity/presence-check/submit", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public ResponseEntity<Response> submitPresenceCheck(
            final @Valid @RequestBody ObjectRequest<PresenceCheckSubmitRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, PowerAuthAuthenticationException, OnboardingProcessException {

        final PresenceCheckSubmitRequest requestObject = request.getRequestObject();
        logger.info("Submit presence check initiated", action("submitPresenceCheck"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.submitPresenceCheck(requestObject, apiAuthentication);
        logger.info("Submit presence check succeeded", action("submitPresenceCheck"), stateSucceeded());
        return response;
    }

    /**
     * Resend OTP code to the user.
     * @param request Presence check initialization request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Send OTP response.
     * @throws IdentityVerificationException Thrown when identity verification is not found.
     * @throws PowerAuthTokenInvalidException When the API authentication object does not exist
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     */
    @PostMapping("otp/resend")
    @PowerAuth(resourceId = "/api/identity/otp/resend", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public ResponseEntity<Response> resendOtp(
            final @Valid @RequestBody ObjectRequest<IdentityVerificationOtpSendRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, OnboardingProcessException, PowerAuthTokenInvalidException {

        final IdentityVerificationOtpSendRequest requestObject = request.getRequestObject();
        logger.info("Resend otp initiated", action("resendOtp"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.resendOtp(requestObject, apiAuthentication);
        logger.info("Resend otp succeeded", action("resendOtp"), stateSucceeded());
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
    public ObjectResponse<OtpVerifyResponse> verifyOtp(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<IdentityVerificationOtpVerifyRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext)
            throws PowerAuthEncryptionException, OnboardingProcessException {

        final IdentityVerificationOtpVerifyRequest requestObject = request.getRequestObject();
        logger.info("Verify otp initiated", action("verifyOtp"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.verifyOtp(requestObject, encryptionContext);
        logger.info("Verify otp succeeded", action("verifyOtp"), stateSucceeded());
        return response;
    }

    /**
     * Cleanup documents related to identity verification.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when PowerAuth signature verification fails.
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
    public Response cleanup(
            final @Valid @RequestBody ObjectRequest<IdentityVerificationCleanupRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PresenceCheckException, RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException, OnboardingProcessLimitException {

        final IdentityVerificationCleanupRequest requestObject = request.getRequestObject();
        logger.info("Cleanup initiated", action("cleanup"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final Response response = identityVerificationRestService.cleanup(requestObject, apiAuthentication);
        logger.info("Cleanup succeeded", action("cleanup"), stateSucceeded());
        return response;
    }

    /**
     * Obtain consent text.
     * @param request Obtain consent text request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Consent text.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthTokenInvalidException When the API authentication object does not exist
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
            final @Valid @RequestBody ObjectRequest<OnboardingConsentTextRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthTokenInvalidException {

        final OnboardingConsentTextRequest requestObject = request.getRequestObject();
        logger.info("Fetch consent text initiated", action("fetchConsentText"), stateInitiated(), kv("processId", requestObject.getProcessId()));
        final var response = identityVerificationRestService.fetchConsentText(requestObject, apiAuthentication);
        logger.info("Fetch consent text succeeded", action("fetchConsentText"), stateSucceeded());
        return response;
    }

    /**
     * Approve or reject consent.
     * @param request Approve consent request
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     */
    @PostMapping("consent/approve")
    @Operation(
            summary = "Store user consent",
            description = "Store user consent, whether approved or not."
    )
    @PowerAuth(resourceId = "/api/identity/consent/approve", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public Response approveConsent(
            final @Valid @RequestBody ObjectRequest<OnboardingConsentApprovalRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthAuthenticationException {

        final OnboardingConsentApprovalRequest requestObject = request.getRequestObject();
        logger.info("Approve consent initiated", action("approveConsent"), stateInitiated(), kv("processId", requestObject.getProcessId()), kv("approved", requestObject.isApproved()));
        final Response response = identityVerificationRestService.approveConsent(requestObject, apiAuthentication);
        logger.info("Approve consent succeeded", action("approveConsent"), stateSucceeded());
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
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<CreateTargetActivationRequest> request,
            @Parameter(hidden = true) @NotNull final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, RemoteCommunicationException {

        final CreateTargetActivationRequest requestObject = request.getRequestObject();
        logger.info("Create target activation initiated", action("createTargetActivation"), stateInitiated(), kv("processId", requestObject.processId()));
        final CreateTargetActivationResponse response = identityVerificationTargetActivationService.createTargetActivation(requestObject, apiAuthentication);
        logger.info("Create target activation succeeded", action("createTargetActivation"), stateSucceeded());

        return new ObjectResponse<>(response);
    }
}
