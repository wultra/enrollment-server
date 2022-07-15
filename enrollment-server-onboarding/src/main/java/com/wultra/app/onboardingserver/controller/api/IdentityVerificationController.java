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
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.ConfigurationDataDto;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.DocumentMetadataResponseDto;
import com.wultra.app.enrollmentserver.model.DocumentMetadata;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.impl.service.*;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.util.PowerAuthUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationController.class);

    private final IdentityVerificationConfig identityVerificationConfig;

    private final DocumentProcessingService documentProcessingService;
    private final IdentityVerificationService identityVerificationService;
    private final IdentityVerificationStatusService identityVerificationStatusService;
    private final IdentityVerificationOtpService identityVerificationOtpService;
    private final PresenceCheckService presenceCheckService;
    private final OnboardingServiceImpl onboardingService;

    /**
     * Configuration data for client integration
     */
    private final ConfigurationDataDto integrationConfigDto;

    /**
     * Controller constructor.
     * @param identityVerificationConfig Configuration of identity verification.
     * @param onboardingConfig Configuration of onboarding.
     * @param documentProcessingService Document processing service.
     * @param identityVerificationService Identity verification service.
     * @param identityVerificationStatusService Identity verification status service.
     * @param identityVerificationOtpService Identity OTP verification service.
     * @param onboardingService Onboarding service.
     * @param presenceCheckService Presence check service.
     */
    @Autowired
    public IdentityVerificationController(
            IdentityVerificationConfig identityVerificationConfig,
            OnboardingConfig onboardingConfig,
            DocumentProcessingService documentProcessingService,
            IdentityVerificationService identityVerificationService,
            IdentityVerificationStatusService identityVerificationStatusService,
            IdentityVerificationOtpService identityVerificationOtpService,
            OnboardingServiceImpl onboardingService,
            PresenceCheckService presenceCheckService) {
        this.identityVerificationConfig = identityVerificationConfig;

        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.identityVerificationStatusService = identityVerificationStatusService;
        this.identityVerificationOtpService = identityVerificationOtpService;
        this.onboardingService = onboardingService;
        this.presenceCheckService = presenceCheckService;

        this.integrationConfigDto = new ConfigurationDataDto();
        integrationConfigDto.setOtpResendPeriod(onboardingConfig.getOtpResendPeriod().toString());
    }

    /**
     * Initialize identity verification.
     * @param request Initialize identity verification request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when encryption fails.
     * @throws IdentityVerificationException Thrown when identity verification initialization fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    @PostMapping("init")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/init", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response initializeIdentityVerification(@EncryptedRequestBody ObjectRequest<IdentityVerificationInitRequest> request,
                                                   @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                   @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, IdentityVerificationException, RemoteCommunicationException, OnboardingProcessException, PowerAuthEncryptionException, OnboardingProcessLimitException {

        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when initializing identity verification");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when initializing identity verification");
        }
        if (eciesContext == null) {
            throw new PowerAuthEncryptionException("ECIES encryption failed");
        }
        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when initializing identity verification");
            throw new PowerAuthAuthenticationException("Invalid request received when initializing identity verification");
        }

        // Initialize identity verification
        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        identityVerificationService.initializeIdentityVerification(ownerId, processId);
        return new Response();
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document submit response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws IdentityVerificationException Thrown when identity verification status fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     * @throws OnboardingOtpDeliveryException Thrown when OTP could not be sent when changing status.
     */
    @PostMapping("status")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(@EncryptedRequestBody ObjectRequest<IdentityVerificationStatusRequest> request,
                                                                                              @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                                              @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, IdentityVerificationException, RemoteCommunicationException, OnboardingProcessException, OnboardingOtpDeliveryException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when checking identity verification status");
            throw new PowerAuthTokenInvalidException("Unable to verify device registration when checking identity verification status");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when checking identity verification status");
            throw new PowerAuthEncryptionException("ECIES decryption failed when checking identity verification status");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when checking identity verification status");
            throw new PowerAuthEncryptionException("Invalid request received when checking identity verification status");
        }

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        // Check verification status
        final IdentityVerificationStatusResponse response =
                identityVerificationStatusService.checkIdentityVerificationStatus(request.getRequestObject(), ownerId);
        response.setConfig(integrationConfigDto);

        return new ObjectResponse<>(response);
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
    public ObjectResponse<DocumentSubmitResponse> submitDocuments(@EncryptedRequestBody ObjectRequest<DocumentSubmitRequest> request,
                                                                  @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                  @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when checking document verification status");
            throw new PowerAuthTokenInvalidException("Unable to verify device registration when checking document verification status");
        }
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthEncryptionException("ECIES encryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthEncryptionException("Invalid request received when submitting documents for verification");
        }

        // Extract user ID from onboarding process for current activation
        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        // Submit documents for verification
        final List<DocumentVerificationEntity> docVerificationEntities =
                identityVerificationService.submitDocuments(request.getRequestObject(), ownerId);

        final DocumentSubmitResponse response = new DocumentSubmitResponse();
        final List<DocumentMetadataResponseDto> respsMetadata =
                identityVerificationService.createDocsMetadata(docVerificationEntities);
        response.setDocuments(respsMetadata);

        return new ObjectResponse<>(response);
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
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when checking document verification status");
            throw new PowerAuthTokenInvalidException("Unable to verify device registration when checking document verification status");
        }
        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when uploading document for verification");
            throw new PowerAuthEncryptionException("ECIES encryption failed when uploading document for verification");
        }

        if (requestData == null) {
            logger.error("Invalid request received when uploading document for verification");
            throw new PowerAuthEncryptionException("Invalid request received when uploading document for verification");
        }

        // Extract user ID from finished onboarding process for current activation
        final OnboardingProcessEntity onboardingProcess = onboardingService.findExistingProcessWithVerificationInProgress(eciesContext.getActivationId());
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(onboardingProcess.getActivationId());
        ownerId.setUserId(onboardingProcess.getUserId());

        IdentityVerificationEntity idVerification = findIdentityVerification(ownerId);

        final DocumentMetadata uploadedDocument = documentProcessingService.uploadDocument(idVerification, requestData, ownerId);

        final DocumentUploadResponse response = new DocumentUploadResponse();
        response.setFilename(uploadedDocument.getFilename());
        response.setId(uploadedDocument.getId());

        return new ObjectResponse<>(response);
    }

    /**
     * Check status of document verification related to identity.
     * @param request Document status request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     */
    @PostMapping("document/status")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentStatusResponse> checkDocumentStatus(@EncryptedRequestBody ObjectRequest<DocumentStatusRequest> request,
                                                                      @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                      @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when checking document verification status");
            throw new PowerAuthTokenInvalidException("Unable to verify device registration when checking document verification status");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when checking document verification status");
            throw new PowerAuthEncryptionException("ECIES encryption failed when checking document verification status");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when checking document verification status");
            throw new PowerAuthEncryptionException("Invalid request received when checking document verification status");
        }

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        // Process upload document request
        final DocumentStatusResponse response = identityVerificationService.checkIdentityVerificationStatus(request.getRequestObject(), ownerId);
        return new ObjectResponse<>(response);
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
     */
    @PostMapping("document-verification/init-sdk")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/document-verification/init-sdk", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentVerificationSdkInitResponse> initVerificationSdk(
            @EncryptedRequestBody ObjectRequest<DocumentVerificationSdkInitRequest> request,
            @Parameter(hidden = true) EciesEncryptionContext eciesContext,
            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when initializing document verification SDK");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when initializing document verification SDK");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when initializing document verification SDK");
            throw new PowerAuthEncryptionException("ECIES encryption failed when initializing document verification SDK");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when initializing document verification SDK");
            throw new PowerAuthEncryptionException("Invalid request received when initializing document verification SDK");
        }

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        final Map<String, String> attributes = request.getRequestObject().getAttributes();
        final VerificationSdkInfo sdkVerificationInfo = identityVerificationService.initVerificationSdk(ownerId, attributes);

        final DocumentVerificationSdkInitResponse response = new DocumentVerificationSdkInitResponse();
        response.setAttributes(sdkVerificationInfo.getAttributes());
        return new ObjectResponse<>(response);
    }

    /**
     * Initialize presence check process.
     * @param request Presence check initialization request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws PresenceCheckLimitException Thrown when presence check limit is exceeded.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    @PostMapping("presence-check/init")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/presence-check/init", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<PresenceCheckInitResponse> initPresenceCheck(@EncryptedRequestBody ObjectRequest<PresenceCheckInitRequest> request,
                                                                       @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                       @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PresenceCheckException,
            PresenceCheckNotEnabledException, PowerAuthEncryptionException, OnboardingProcessException, PresenceCheckLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {

        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when initializing presence check");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when initializing presence check");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when initializing presence check");
            throw new PowerAuthEncryptionException("ECIES encryption failed when initializing presence check");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when initializing presence check");
            throw new PowerAuthEncryptionException("Invalid request received when initializing presence check");
        }

        if (!identityVerificationConfig.isPresenceCheckEnabled()) {
            throw new PresenceCheckNotEnabledException();
        }

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        final SessionInfo sessionInfo = presenceCheckService.init(ownerId, processId);

        final PresenceCheckInitResponse response = new PresenceCheckInitResponse();
        response.setSessionAttributes(sessionInfo.getSessionAttributes());
        return new ObjectResponse<>(response);
    }

    /**
     * Resend OTP code to the user.
     * @param request Presence check initialization request.
     * @param eciesContext ECIES context.
     * @return Send OTP response.
     * @throws IdentityVerificationException Thrown when identity verification is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    @PostMapping("otp/resend")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    public Response resendOtp(@EncryptedRequestBody ObjectRequest<IdentityVerificationOtpSendRequest> request,
                              @Parameter(hidden = true) EciesEncryptionContext eciesContext)
            throws IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException, OnboardingOtpDeliveryException {

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when sending OTP during identity verification");
            throw new PowerAuthEncryptionException("ECIES encryption failed when sending OTP during identity verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when sending OTP during identity verification");
            throw new PowerAuthEncryptionException("Invalid request received when sending OTP during identity verification");
        }

        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();
        onboardingService.verifyProcessId(ownerId, processId);

        IdentityVerificationEntity identityVerification = findIdentityVerification(ownerId);
        identityVerificationOtpService.resendOtp(ownerId, identityVerification);
        return new Response();
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

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when verifying OTP during identity verification");
            throw new PowerAuthEncryptionException("ECIES encryption failed when sending OTP during identity verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when verifying OTP during identity verification");
            throw new PowerAuthEncryptionException("Invalid request received when sending OTP during identity verification");
        }

        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        final String otpCode = request.getRequestObject().getOtpCode();
        return new ObjectResponse<>(identityVerificationOtpService.verifyOtpCode(processId, otpCode));
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
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/cleanup", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response cleanup(@EncryptedRequestBody ObjectRequest<IdentityVerificationCleanupRequest> request,
                            @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, PresenceCheckException, RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException, OnboardingProcessLimitException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when performing document cleanup");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when performing document cleanup");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when performing document cleanup");
            throw new PowerAuthEncryptionException("ECIES encryption failed when performing document cleanup");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when performing document cleanup");
            throw new PowerAuthEncryptionException("Invalid request received when performing document cleanup");
        }

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        // Process cleanup request
        identityVerificationService.cleanup(ownerId);
        if (identityVerificationConfig.isPresenceCheckEnabled()) {
            presenceCheckService.cleanup(ownerId);
        } else {
            logger.debug("Skipped presence check cleanup, not enabled");
        }

        return new Response();
    }

    @PostMapping("consent/text")
    @Operation(
            summary = "Obtain consent text",
            description = "Obtain a text of user consent in specified language."
    )
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    public ObjectResponse<OnboardingConsentTextResponse> fetchConsentText(
            final @EncryptedRequestBody ObjectRequest<OnboardingConsentTextRequest> request,
            final @Parameter(hidden = true, required = true) Locale locale,
            final @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws OnboardingProcessException, PowerAuthEncryptionException {

        if (eciesContext == null) {
            throw new PowerAuthEncryptionException("ECIES encryption failed");
        }
        if (request == null || request.getRequestObject() == null) {
            throw new PowerAuthEncryptionException("Invalid request received");
        }
        final OnboardingConsentTextRequest requestObject = request.getRequestObject();
        logger.debug("Returning consent for {}", requestObject);
        OnboardingConsentTextRequestValidator.validate(requestObject);

        final String processId = requestObject.getProcessId().toString();
        final OnboardingProcessEntity process = onboardingService.findProcess(processId);
        final String userId = process.getUserId();

        final OnboardingConsentTextResponse onboardingConsentTextResponse = onboardingService.fetchConsentText(requestObject, userId, locale);
        return new ObjectResponse<>(onboardingConsentTextResponse);
    }

    @PostMapping("consent/approve")
    @Operation(
            summary = "Store user consent",
            description = "Store user consent, whether approved or not."
    )
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/consent/approve", signatureType = PowerAuthSignatureTypes.POSSESSION)
    public Response approveConsent(
            final @EncryptedRequestBody ObjectRequest<OnboardingConsentApprovalRequest> request,
            final @Parameter(hidden = true) EciesEncryptionContext eciesContext,
            final @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthAuthenticationException, PowerAuthEncryptionException {

        if (apiAuthentication == null) {
            throw new PowerAuthAuthenticationException("Unable to authenticate");
        }
        if (eciesContext == null) {
            throw new PowerAuthEncryptionException("ECIES encryption failed");
        }
        if (request == null || request.getRequestObject() == null) {
            throw new PowerAuthEncryptionException("Invalid request received");
        }

        final OnboardingConsentApprovalRequest requestObject = request.getRequestObject();
        logger.debug("Approving consent for {}", requestObject);
        OnboardingConsentApprovalRequestValidator.validate(requestObject);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = requestObject.getProcessId().toString();
        onboardingService.verifyProcessId(ownerId, processId);

        onboardingService.approveConsent(requestObject, ownerId);
        return new Response();
    }

    /**
     * Extract owner identification from an ECIES context.
     * @param eciesContext ECIES context.
     * @return Owner identification.
     */
    private OwnerId extractOwnerId(EciesEncryptionContext eciesContext) throws OnboardingProcessException {
        final OnboardingProcessEntity onboardingProcess = onboardingService.findExistingProcessWithVerificationInProgress(eciesContext.getActivationId());
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(onboardingProcess.getActivationId());
        ownerId.setUserId(onboardingProcess.getUserId());
        return ownerId;
    }

    private IdentityVerificationEntity findIdentityVerification(OwnerId ownerId) throws IdentityVerificationNotFoundException {
        Optional<IdentityVerificationEntity> identityVerificationOptional = identityVerificationService.findBy(ownerId);

        if (identityVerificationOptional.isEmpty()) {
            logger.error("No identity verification entity found, {}", ownerId);
            throw new IdentityVerificationNotFoundException("Not existing identity verification");
        }
        return identityVerificationOptional.get();
    }

}
