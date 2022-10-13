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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.*;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.*;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.ConfigurationDataDto;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.DocumentMetadataResponseDto;
import com.wultra.app.enrollmentserver.model.DocumentMetadata;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.validation.OnboardingConsentApprovalRequestValidator;
import com.wultra.app.onboardingserver.impl.service.validation.OnboardingConsentTextRequestValidator;
import com.wultra.app.onboardingserver.impl.util.PowerAuthUtil;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ErrorResponse;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.spring.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.getlime.security.powerauth.rest.api.spring.exception.authentication.PowerAuthTokenInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Service implementing REST API methods for identity document verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server-onboarding.identity-verification.enabled",
        havingValue = "true"
)
@Service
public class IdentityVerificationRestService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationRestService.class);

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
    public IdentityVerificationRestService(
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
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @Transactional
    public ResponseEntity<Response> initializeIdentityVerification(ObjectRequest<IdentityVerificationInitRequest> request,
                                                                   EciesEncryptionContext eciesContext,
                                                                   PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "initializing identity verification";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        // Initialize identity verification
        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

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
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(ObjectRequest<IdentityVerificationStatusRequest> request,
                                                                                              EciesEncryptionContext eciesContext,
                                                                                              PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException, OnboardingProcessException {

        final String operationDescription = "checking identity verification status";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        logger.debug("Onboarding process will not be locked, {}", ownerId);
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
    @Transactional
    public ObjectResponse<DocumentSubmitResponse> submitDocuments(ObjectRequest<DocumentSubmitRequest> request,
                                                                  EciesEncryptionContext eciesContext,
                                                                  PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {

        final String operationDescription = "submitting documents for verification";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        // Extract user ID from onboarding process for current activation
        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

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
    public ObjectResponse<DocumentUploadResponse> uploadDocument(byte[] requestData,
                                                                 EciesEncryptionContext eciesContext,
                                                                 PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, OnboardingProcessException {

        final String operationDescription = "uploading document for verification";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequest(requestData, operationDescription);

        // Extract user ID from onboarding process for current activation
        final OwnerId ownerId = extractOwnerId(eciesContext);

        logger.debug("Onboarding process will not be locked, {}", ownerId);
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
    public ObjectResponse<DocumentStatusResponse> checkDocumentStatus(ObjectRequest<DocumentStatusRequest> request,
                                                                      EciesEncryptionContext eciesContext,
                                                                      PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "checking document verification status";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will not be locked, {}", processId);
        onboardingService.verifyProcessId(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

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
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    public ObjectResponse<DocumentVerificationSdkInitResponse> initVerificationSdk(
            ObjectRequest<DocumentVerificationSdkInitRequest> request,
            EciesEncryptionContext eciesContext,
            PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PowerAuthEncryptionException, OnboardingProcessException, RemoteCommunicationException {

        final String operationDescription = "initializing document verification SDK";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will not be locked, {}", processId);
        onboardingService.verifyProcessId(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        final Map<String, String> attributes = request.getRequestObject().getAttributes();
        final VerificationSdkInfo sdkVerificationInfo = identityVerificationService.initVerificationSdk(ownerId, attributes);

        final DocumentVerificationSdkInitResponse response = new DocumentVerificationSdkInitResponse();
        response.setAttributes(sdkVerificationInfo.getAttributes());
        return new ObjectResponse<>(response);
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
    @Transactional
    public ResponseEntity<Response> initPresenceCheck(ObjectRequest<PresenceCheckInitRequest> request,
                                                      EciesEncryptionContext eciesContext,
                                                      PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "initializing presence check";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.PRESENCE_CHECK_INIT);
        return createResponseEntity(stateMachine);
    }

    /**
     * Submit presence check process.
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
    @Transactional
    public ResponseEntity<Response> submitPresenceCheck(ObjectRequest<PresenceCheckSubmitRequest> request,
                                                        EciesEncryptionContext eciesContext,
                                                        PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "submitting presence check";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.PRESENCE_CHECK_SUBMITTED);
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
    @Transactional
    public ResponseEntity<Response> resendOtp(ObjectRequest<IdentityVerificationOtpSendRequest> request,
                                              EciesEncryptionContext eciesContext)
            throws IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        checkEciesContext(eciesContext, "resending OTP during identity verification");
        checkRequestObject(request, "resending OTP during identity verification");

        // Extract user ID from onboarding process for current activation, lock onboarding process
        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

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
    @Transactional
    public ObjectResponse<OtpVerifyResponse> verifyOtp(ObjectRequest<IdentityVerificationOtpVerifyRequest> request,
                                                       EciesEncryptionContext eciesContext)
            throws PowerAuthEncryptionException, OnboardingProcessException {

        checkEciesContext(eciesContext, "verifying OTP during identity verification");
        checkRequestObject(request, "verifying OTP during identity verification");

        // Extract user ID from onboarding process for current activation
        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        final String otpCode = request.getRequestObject().getOtpCode();
        final OtpVerifyResponse otpVerifyResponse = identityVerificationOtpService.verifyOtpCode(processId, ownerId, otpCode);

        try {
            stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.EVENT_NEXT_STATE);
        } catch (IdentityVerificationException e) {
            throw new OnboardingProcessException("Unable to move state machine for " + ownerId, e);
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
    @Transactional
    public Response cleanup(ObjectRequest<IdentityVerificationCleanupRequest> request,
                            EciesEncryptionContext eciesContext,
                            PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, PresenceCheckException, RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException, OnboardingProcessLimitException {

        final String operationDescription = "performing document cleanup";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        // Process cleanup request
        identityVerificationService.cleanup(ownerId);
        if (identityVerificationConfig.isPresenceCheckEnabled()) {
            presenceCheckService.cleanup(ownerId);
        } else {
            logger.debug("Skipped presence check cleanup, not enabled");
        }

        return new Response();
    }

    /**
     * Obtain consent text.
     * @param request Obtain consent text request.
     * @param eciesContext ECIES context.
     * @return Consent text.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     */
    public ObjectResponse<OnboardingConsentTextResponse> fetchConsentText(
            final ObjectRequest<OnboardingConsentTextRequest> request,
            final EciesEncryptionContext eciesContext) throws OnboardingProcessException, PowerAuthEncryptionException {

        checkEciesContext(eciesContext, "obtaining user consent text");
        checkRequestObject(request, "obtaining user consent text");

        final OnboardingConsentTextRequest requestObject = request.getRequestObject();
        logger.debug("Returning consent for {}", requestObject);
        OnboardingConsentTextRequestValidator.validate(requestObject);

        final OwnerId ownerId = extractOwnerId(eciesContext);
        final String processId = requestObject.getProcessId();

        logger.debug("Onboarding process will not be locked, {}", processId);
        onboardingService.verifyProcessId(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        return new ObjectResponse<>(onboardingService.fetchConsentText(requestObject));
    }

    /**
     * Approve or reject consent.
     * @param request Approve consent request
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     */
    public Response approveConsent(
            final ObjectRequest<OnboardingConsentApprovalRequest> request,
            final EciesEncryptionContext eciesContext,
            final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthAuthenticationException, PowerAuthEncryptionException {

        final String operationDescription = "approving user consent";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEciesContext(eciesContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OnboardingConsentApprovalRequest requestObject = request.getRequestObject();
        logger.debug("Approving consent for {}", requestObject);
        OnboardingConsentApprovalRequestValidator.validate(requestObject);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = requestObject.getProcessId();

        logger.debug("Onboarding process will not be locked, {}", processId);
        onboardingService.verifyProcessId(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        onboardingService.approveConsent(requestObject);
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
     * Extract owner identification from an ECIES context. The onboarding process is not locked.
     *
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
