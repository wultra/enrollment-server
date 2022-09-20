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
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.impl.service.*;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.util.PowerAuthUtil;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ErrorResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

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

    private final StateMachineService stateMachineService;

    private final OnboardingServiceImpl onboardingService;

    /**
     * Configuration data for client integration
     */
    private final ConfigurationDataDto integrationConfigDto;

    /**
     * Controller constructor.
     *
     * @param identityVerificationConfig        Configuration of identity verification.
     * @param onboardingConfig                  Configuration of onboarding.
     * @param documentProcessingService         Document processing service.
     * @param identityVerificationService       Identity verification service.
     * @param identityVerificationStatusService Identity verification status service.
     * @param identityVerificationOtpService    Identity OTP verification service.
     * @param onboardingService                 Onboarding service.
     * @param presenceCheckService              Presence check service.
     * @param stateMachineService               State machine service.
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
            PresenceCheckService presenceCheckService,
            StateMachineService stateMachineService) {
        this.identityVerificationConfig = identityVerificationConfig;

        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.identityVerificationStatusService = identityVerificationStatusService;
        this.identityVerificationOtpService = identityVerificationOtpService;
        this.onboardingService = onboardingService;
        this.presenceCheckService = presenceCheckService;
        this.stateMachineService = stateMachineService;

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
     */
    @PostMapping("init")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/init", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ResponseEntity<Response> initializeIdentityVerification(@EncryptedRequestBody ObjectRequest<IdentityVerificationInitRequest> request,
                                                                   @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                   @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, IdentityVerificationException, PowerAuthEncryptionException {

        checkApiAuthentication(apiAuthentication, "initializing identity verification");
        checkEciesContext(eciesContext, "initializing identity verification");
        checkRequestObject(request, "initializing identity verification");

        // Initialize identity verification
        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        StateMachine<OnboardingState, OnboardingEvent> stateMachine =
                stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.IDENTITY_VERIFICATION_INIT);

        return createResponseEntity(stateMachine);
    }

    /**
     * Check status of identity verification.
     *
     * @param request Document submit request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document submit response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @PostMapping("status")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(@EncryptedRequestBody ObjectRequest<IdentityVerificationStatusRequest> request,
                                                                                              @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                                              @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException, OnboardingProcessException {

        final String operationDescription = "checking identity verification status";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

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

        final String operationDescription = "submitting documents for verification";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

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

        checkApiAuthentication(apiAuthentication, "uploading document for verification");
        checkEciesContext(eciesContext, "uploading document for verification");
        checkRequest(requestData, "uploading document for verification");

        // Extract user ID from finished onboarding process for current activation
        final OnboardingProcessEntity onboardingProcess = onboardingService.findExistingProcessWithVerificationInProgress(eciesContext.getActivationId());
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(onboardingProcess.getActivationId());
        ownerId.setUserId(onboardingProcess.getUserId());

        IdentityVerificationEntity idVerification = identityVerificationService.findBy(ownerId);

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

        final String operationDescription = "checking document verification status";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        final DocumentStatusResponse response = identityVerificationService.fetchDocumentStatusResponse(request.getRequestObject(), ownerId);
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

        checkApiAuthentication(apiAuthentication, "initializing document verification SDK");
        checkEciesContext(eciesContext, "initializing document verification SDK");
        checkRequestObject(request, "initializing document verification SDK");

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
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     */
    @PostMapping("presence-check/init")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/presence-check/init", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ResponseEntity<Response> initPresenceCheck(@EncryptedRequestBody ObjectRequest<PresenceCheckInitRequest> request,
                                                      @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                      @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException {

        checkApiAuthentication(apiAuthentication, "initializing presence check");
        checkEciesContext(eciesContext, "initializing presence check");
        checkRequestObject(request, "initializing presence check");

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.PRESENCE_CHECK_INIT);
        return createResponseEntity(stateMachine);
    }

    /**
     * Resend OTP code to the user.
     * @param request Presence check initialization request.
     * @param eciesContext ECIES context.
     * @return Send OTP response.
     * @throws IdentityVerificationException Thrown when identity verification is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     */
    @PostMapping("otp/resend")
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    public ResponseEntity<Response> resendOtp(@EncryptedRequestBody ObjectRequest<IdentityVerificationOtpSendRequest> request,
                                              @Parameter(hidden = true) EciesEncryptionContext eciesContext)
            throws IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        checkEciesContext(eciesContext, "resending OTP during identity verification");
        checkRequestObject(request, "resending OTP during identity verification");

        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.OTP_VERIFICATION_RESEND);
        return createResponseEntity(stateMachine);
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

        checkEciesContext(eciesContext, "verifying OTP during identity verification");
        checkRequestObject(request, "verifying OTP during identity verification");

        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        onboardingService.verifyProcessId(ownerId, processId);

        final String otpCode = request.getRequestObject().getOtpCode();
        final OtpVerifyResponse otpVerifyResponse = identityVerificationOtpService.verifyOtpCode(processId, ownerId, otpCode);

        try {
            stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.EVENT_NEXT_STATE);
        } catch (IdentityVerificationException e) {
            throw new OnboardingProcessException("Unable to move state machine for process ID: " + processId, e);
        }

        return new ObjectResponse<>(otpVerifyResponse);
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

        checkApiAuthentication(apiAuthentication, "performing document cleanup");
        checkEciesContext(eciesContext, "performing document cleanup");
        checkRequestObject(request, "performing document cleanup");

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
            final @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws OnboardingProcessException, PowerAuthEncryptionException {

        checkEciesContext(eciesContext, "obtaining user consent text");
        checkRequestObject(request, "obtaining user consent text");

        final OnboardingConsentTextRequest requestObject = request.getRequestObject();
        logger.debug("Returning consent for {}", requestObject);
        OnboardingConsentTextRequestValidator.validate(requestObject);

        final String processId = requestObject.getProcessId().toString();
        final OnboardingProcessEntity process = onboardingService.findProcess(processId);
        final String userId = process.getUserId();

        return new ObjectResponse<>(onboardingService.fetchConsentText(requestObject, userId));
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

        checkApiAuthentication(apiAuthentication, "approving user consent");
        checkEciesContext(eciesContext, "approving user consent");
        checkRequestObject(request, "approving user consent");

        final OnboardingConsentApprovalRequest requestObject = request.getRequestObject();
        logger.debug("Approving consent for {}", requestObject);
        OnboardingConsentApprovalRequestValidator.validate(requestObject);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = requestObject.getProcessId().toString();
        onboardingService.verifyProcessId(ownerId, processId);

        onboardingService.approveConsent(requestObject, ownerId.getUserId());
        return new Response();
    }

    /**
     * Checks if the API authentication object is present
     * @param apiAuthentication API authentication object value
     * @param description Additional description
     * @throws PowerAuthTokenInvalidException When the API authentication object does not exist
     */
    private void checkApiAuthentication(@Nullable PowerAuthApiAuthentication apiAuthentication, String description) throws PowerAuthTokenInvalidException {
        if (apiAuthentication == null) {
            String errorMessage = String.format("Unable to verify device registration when %s", description);
            logger.error(errorMessage);
            throw new PowerAuthTokenInvalidException(errorMessage);
        }
    }

    /**
     * Checks if the request was correctly decrypted
     * @param eciesContext ECIES encryption context
     * @param description Additional description
     * @throws PowerAuthEncryptionException When the ECIES encryption context does not exist
     */
    private void checkEciesContext(@Nullable EciesEncryptionContext eciesContext, String description) throws PowerAuthEncryptionException {
        if (eciesContext == null) {
            String errorMessage = String.format("ECIES encryption failed when %s", description);
            logger.error(errorMessage);
            throw new PowerAuthEncryptionException(errorMessage);
        }
    }

    private void checkRequest(@Nullable Object request, String description) throws PowerAuthEncryptionException {
        if (request == null) {
            String errorMessage = String.format("Invalid request received when %s", description);
            logger.error(errorMessage);
            throw new PowerAuthEncryptionException(errorMessage);
        }
    }

    private void checkRequestObject(@Nullable ObjectRequest<?> request, String description) throws PowerAuthEncryptionException {
        if (request == null || request.getRequestObject() == null) {
            String errorMessage = String.format("Invalid request received when %s", description);
            logger.error(errorMessage);
            throw new PowerAuthEncryptionException(errorMessage);
        }
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

    private ResponseEntity<Response> createResponseEntity(StateMachine<OnboardingState, OnboardingEvent> stateMachine) {
        Response response = stateMachine.getExtendedState().get(ExtendedStateVariable.RESPONSE_OBJECT, Response.class);
        HttpStatus status = stateMachine.getExtendedState().get(ExtendedStateVariable.RESPONSE_STATUS, HttpStatus.class);
        if (response == null || status == null) {
            logger.warn("Missing one of important values to generate response entity, response={}, status={}", response, status);
            response = new ErrorResponse("UNEXPECTED_ERROR", "Unexpected error occurred.");
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(response, status);
    }

}
