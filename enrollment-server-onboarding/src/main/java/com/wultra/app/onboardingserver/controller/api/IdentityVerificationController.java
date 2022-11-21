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
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationRestService;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.encryptor.ecies.model.EciesScope;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuth;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.spring.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.getlime.security.powerauth.rest.api.spring.exception.authentication.PowerAuthTokenInvalidException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class IdentityVerificationController {

    private final IdentityVerificationRestService identityVerificationRestService;

    /**
     * Controller constructor.
     * @param identityVerificationRestService  Identity verification entry service.
     */
    @Autowired
    public IdentityVerificationController(IdentityVerificationRestService identityVerificationRestService) {
        this.identityVerificationRestService = identityVerificationRestService;
    }

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
    @PowerAuth(resourceId = "/api/identity/init", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ResponseEntity<Response> initializeIdentityVerification(@RequestBody ObjectRequest<IdentityVerificationInitRequest> request,
                                                                   @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        return identityVerificationRestService.initializeIdentityVerification(request, apiAuthentication);
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
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(@RequestBody ObjectRequest<IdentityVerificationStatusRequest> request,
                                                                                              @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException, OnboardingProcessException {

       return identityVerificationRestService.checkIdentityVerificationStatus(request, apiAuthentication);
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param eciesContext ECIES context.
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
    @PostMapping("document/submit")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response submitDocuments(@EncryptedRequestBody ObjectRequest<DocumentSubmitRequest> request,
                                                                  @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                  @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {

        return identityVerificationRestService.submitDocuments(request, eciesContext, apiAuthentication);
    }

    /**
     * Upload a single document related to identity verification. This endpoint is used for upload of large documents.
     * @param requestData Binary request data.
     * @param eciesContext ECIES context.
     * @return Document upload response.
     * @throws IdentityVerificationException Thrown when identity verification was not found.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws DocumentVerificationException Thrown when document is invalid.
     * @throws OnboardingProcessException Thrown when finished onboarding process is not found.
     */
    @PostMapping("document/upload")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentUploadResponse> uploadDocument(@EncryptedRequestBody byte[] requestData,
                                                                 @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                 @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, OnboardingProcessException {

        return identityVerificationRestService.uploadDocument(requestData, eciesContext, apiAuthentication);
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
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentStatusResponse> checkDocumentStatus(@RequestBody ObjectRequest<DocumentStatusRequest> request,
                                                                      @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        return identityVerificationRestService.checkDocumentStatus(request, apiAuthentication);
    }

    /**
     * Initialize document verification SDK for an integration.
     * @param request Presence check initialization request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Verification SDK initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws DocumentVerificationException Thrown when SKD initialization fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    @PostMapping("document/init-sdk")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/document/init-sdk", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentVerificationSdkInitResponse> initVerificationSdk(
            @EncryptedRequestBody ObjectRequest<DocumentVerificationSdkInitRequest> request,
            @Parameter(hidden = true) EciesEncryptionContext eciesContext,
            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PowerAuthEncryptionException, OnboardingProcessException, RemoteCommunicationException {

        return identityVerificationRestService.initVerificationSdk(request, eciesContext, apiAuthentication);
    }

    /**
     * Initialize presence check process.
     *
     * @param request Presence check initialization request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("presence-check/init")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/presence-check/init", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ResponseEntity<Response> initPresenceCheck(@EncryptedRequestBody ObjectRequest<PresenceCheckInitRequest> request,
                                                      @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                      @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        return identityVerificationRestService.initPresenceCheck(request, eciesContext, apiAuthentication);
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
    @PowerAuth(resourceId = "/api/identity/presence-check/submit", signatureType = PowerAuthSignatureTypes.POSSESSION)
    public ResponseEntity<Response> submitPresenceCheck(@RequestBody ObjectRequest<PresenceCheckSubmitRequest> request,
                                                        @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        return identityVerificationRestService.submitPresenceCheck(request, apiAuthentication);
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
    @PowerAuth(resourceId = "/api/identity/otp/resend", signatureType = PowerAuthSignatureTypes.POSSESSION)
    public ResponseEntity<Response> resendOtp(
            final @RequestBody ObjectRequest<IdentityVerificationOtpSendRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        return identityVerificationRestService.resendOtp(request, apiAuthentication);
    }

    /**
     * Verify an OTP code received from the user.
     * @param request Presence check initialization request.
     * @param eciesContext ECIES context.
     * @return Send OTP response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @PostMapping("otp/verify")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    public ObjectResponse<OtpVerifyResponse> verifyOtp(@EncryptedRequestBody ObjectRequest<IdentityVerificationOtpVerifyRequest> request,
                                                       @Parameter(hidden = true) EciesEncryptionContext eciesContext)
            throws PowerAuthEncryptionException, OnboardingProcessException {

        return identityVerificationRestService.verifyOtp(request, eciesContext);
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
    @PowerAuth(resourceId = "/api/identity/cleanup", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response cleanup(@RequestBody ObjectRequest<IdentityVerificationCleanupRequest> request,
                            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, PresenceCheckException, RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException, OnboardingProcessLimitException {

        return identityVerificationRestService.cleanup(request, apiAuthentication);
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
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<OnboardingConsentTextResponse> fetchConsentText(
            final @RequestBody ObjectRequest<OnboardingConsentTextRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthEncryptionException, PowerAuthTokenInvalidException {

        return identityVerificationRestService.fetchConsentText(request, apiAuthentication);
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
    @PowerAuth(resourceId = "/api/identity/consent/approve", signatureType = PowerAuthSignatureTypes.POSSESSION)
    public Response approveConsent(
            final @RequestBody ObjectRequest<OnboardingConsentApprovalRequest> request,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthAuthenticationException, PowerAuthEncryptionException {

        return identityVerificationRestService.approveConsent(request, apiAuthentication);
    }

}
