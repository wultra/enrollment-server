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
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.DocumentMetadata;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.validation.OnboardingConsentApprovalRequestValidator;
import com.wultra.app.onboardingserver.impl.service.validation.OnboardingConsentTextRequestValidator;
import com.wultra.app.onboardingserver.impl.util.PowerAuthUtil;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import com.wultra.security.powerauth.rest.api.model.entity.ActivationType;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import com.wultra.security.powerauth.rest.api.spring.exception.authentication.PowerAuthTokenInvalidException;
import com.wultra.security.powerauth.rest.api.spring.provider.CustomActivationProvider;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

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
@Slf4j
public class IdentityVerificationRestService {

    private final IdentityVerificationConfig identityVerificationConfig;

    private final DocumentProcessingService documentProcessingService;
    private final IdentityVerificationService identityVerificationService;
    private final IdentityVerificationStatusService identityVerificationStatusService;
    private final IdentityVerificationOtpService identityVerificationOtpService;
    private final PresenceCheckService presenceCheckService;

    private final StateMachineService stateMachineService;

    private final OnboardingServiceImpl onboardingService;

    private final DataExtractionService dataExtractionService;

    private final ActivationService activationService;

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
     * @param dataExtractionService             Data extraction service for uploaded documents.
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
            StateMachineService stateMachineService,
            DataExtractionService dataExtractionService,
            ActivationService activationService) {

        this.identityVerificationConfig = identityVerificationConfig;

        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.identityVerificationStatusService = identityVerificationStatusService;
        this.identityVerificationOtpService = identityVerificationOtpService;
        this.onboardingService = onboardingService;
        this.presenceCheckService = presenceCheckService;
        this.stateMachineService = stateMachineService;
        this.activationService = activationService;

        this.integrationConfigDto = new ConfigurationDataDto();
        integrationConfigDto.setOtpResendPeriod(onboardingConfig.getOtpResendPeriod().toString());
        integrationConfigDto.setOtpResendPeriodSeconds(onboardingConfig.getOtpResendPeriod().toSeconds());

        this.dataExtractionService = dataExtractionService;
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
     * @implNote This method performs a synchronization with the PowerAuth server.
     *           If the activation is confirmed externally, this method changes the process status to {@code VERIFICATION_IN_PROGRESS} as a side effect.
     */
    @Transactional
    public ResponseEntity<Response> initializeIdentityVerification(ObjectRequest<IdentityVerificationInitRequest> request,
                                                                   PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "initializing identity verification";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkRequestObject(request, operationDescription);

        // Initialize identity verification
        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        final OnboardingProcessEntity process = onboardingService.findProcessWithLock(processId);
        verifyProcess(process, processId, ownerId);
        synchronizeStateWithPowerAuth(process);

        StateMachine<OnboardingState, OnboardingEvent> stateMachine =
                stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.IDENTITY_VERIFICATION_INIT);

        return createResponseEntity(stateMachine);
    }

    private static void verifyProcess(final OnboardingProcessEntity process, final String processId, final OwnerId ownerId) throws OnboardingProcessException {
        final String expectedProcessId = process.getId();

        if (!expectedProcessId.equals(processId)) {
            throw new OnboardingProcessException(
                    String.format("Invalid process ID received in request: %s, %s", processId, ownerId));
        }
        if (!OnboardingStatus.NOT_YET_COMPLETED.contains(process.getStatus())) {
            throw new OnboardingProcessException(
                    String.format("Onboarding process is in state: %s, %s, %s", process.getStatus(), processId, ownerId));
        }
        if (!Objects.equals(process.getActivationId(), ownerId.getActivationId())) {
            throw new OnboardingProcessException(
                    String.format("Activation ID mismatch for process ID: %s, %s", processId, ownerId));
        }
    }

    /**
     * Synchronize the onboarding process state if needed.
     * <p>
     * If the process state is {@code ACTIVATION_IN_PROGRESS}, fetch the activation state and if {@code ACTIVE}, move the process to {@code VERIFICATION_IN_PROGRESS}.
     * Not needed for custom activation, where the state changes in {@link CustomActivationProvider#activationWasCommitted(Map, Map, String, String, String, ActivationType, Map)}.
     *
     * @param process Onboarding process.
     * @implNote using polling; callbacks would require more complex configuration and are not as reliable
     */
    private void synchronizeStateWithPowerAuth(final OnboardingProcessEntity process) {
        final String processId = process.getId();
        final String activationId = process.getActivationId();

        if (process.getStatus() == OnboardingStatus.ACTIVATION_IN_PROGRESS && activationId != null) {
            try {
                final ActivationStatus activationStatus = activationService.fetchActivationStatus(activationId);
                if (activationStatus == ActivationStatus.ACTIVE) {
                    logger.info("Activation activated externally, moving process ID: {} to VERIFICATION_IN_PROGRESS", processId);
                    process.setStatus(OnboardingStatus.VERIFICATION_IN_PROGRESS);
                    process.setTimestampLastUpdated(new Date());
                    onboardingService.updateProcess(process);
                }
            } catch (RemoteCommunicationException e) {
                logger.warn("Unable to check activation status for process ID: {}, activation ID: {}", processId, activationId, e);
            }
        } else {
            logger.debug("State synchronization skipped for process ID: {}, status: {}, activation ID: {}", processId, process.getStatus(), activationId);
        }
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
    public ObjectResponse<IdentityVerificationStatusResponse> checkIdentityVerificationStatus(ObjectRequest<IdentityVerificationStatusRequest> request,
                                                                                              PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException, OnboardingProcessException {

        final String operationDescription = "checking identity verification status";
        checkApiAuthentication(apiAuthentication, operationDescription);
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
     * Submit identity-related documents for verification V2.
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
    @Transactional
    public Response submitDocumentsV2(
            final ObjectRequest<DocumentSubmitV2Request> request,
            final EncryptionContext encryptionContext,
            final PowerAuthApiAuthentication apiAuthentication
    ) throws PowerAuthTokenInvalidException, PowerAuthEncryptionException, DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {
        final var operationDescription = "submitting documents for verification V2";

        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEncryptionContext(encryptionContext, operationDescription);
        checkRequestObject(request, operationDescription);

        return submitDocuments(request.getRequestObject(), encryptionContext);
    }

    /**
     * Submit identity-related documents for verification.
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
    @Transactional
    public Response submitDocuments(
            final ObjectRequest<DocumentSubmitRequest> request,
            final EncryptionContext encryptionContext,
            final PowerAuthApiAuthentication apiAuthentication
    ) throws PowerAuthEncryptionException, PowerAuthAuthenticationException, OnboardingProcessException, RemoteCommunicationException, IdentityVerificationLimitException, DocumentSubmitException, IdentityVerificationException, OnboardingProcessLimitException {
        final var operationDescription = "submitting documents for verification";

        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEncryptionContext(encryptionContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final var requestObject = request.getRequestObject();
        final var documentsByFilename = getDocumentsByFilename(encryptionContext.getActivationId(), requestObject);

        final var documentsV2 = buildDocumentsV2(requestObject, documentsByFilename);

        final var requestV2 = DocumentSubmitV2Request.builder()
                .processId(requestObject.getProcessId())
                .documents(documentsV2)
                .resubmit(requestObject.isResubmit())
                .build();

        return submitDocuments(requestV2, encryptionContext);
    }

    private Map<String, Document> getDocumentsByFilename(final String activationId, final DocumentSubmitRequest request) {
        final var documentsByFilename = new HashMap<String, Document>();

        if (request.getData() != null) {
            try {
                final var extractedDocuments = dataExtractionService.extractDocuments(request.getData())
                        .stream()
                        .collect(Collectors.toMap(Document::getFilename, document -> document));

                documentsByFilename.putAll(extractedDocuments);
            } catch (final DocumentVerificationException e) {
                logger.error("Unable to extract documents from {}, activationId: {}, error: {}", request, activationId, e.getMessage());
                logger.debug("Exception when extracting documents", e);
            }
        }

        return documentsByFilename;
    }

    private static List<DocumentSubmitV2Request.Document> buildDocumentsV2(final DocumentSubmitRequest request, final Map<String, Document> documentsByFilename) {
        final var documentsV2 = new ArrayList<DocumentSubmitV2Request.Document>();

        for (final var document : request.getDocuments()) {
            final var filename = document.getFilename();
            final var data = Optional.ofNullable(documentsByFilename.getOrDefault(filename, null))
                    .map(d -> Base64.getEncoder().encodeToString(d.getData()))
                    .orElse(null);

            final var documentV2 = DocumentSubmitV2Request.Document.builder()
                    .filename(filename)
                    .type(document.getType())
                    .side(document.getSide())
                    .originalDocumentId(document.getOriginalDocumentId())
                    .data(data)
                    .build();

            documentsV2.add(documentV2);
        }

        return documentsV2;
    }

    private Response submitDocuments(
            final DocumentSubmitV2Request request,
            final EncryptionContext encryptionContext
    ) throws DocumentSubmitException, OnboardingProcessException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {
        final OwnerId ownerId = extractOwnerId(encryptionContext);
        final String processId = request.processId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        identityVerificationService.submitDocuments(request, ownerId);

        return new Response();
    }

    /**
     * Upload a single document related to identity verification. This endpoint is used for upload of large documents.
     * @param requestData Binary request data.
     * @param encryptionContext Encryption context.
     * @return Document upload response.
     * @throws IdentityVerificationException Thrown when identity verification was not found.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws DocumentVerificationException Thrown when document is invalid.
     * @throws OnboardingProcessException Thrown when finished onboarding process is not found.
     */
    public ObjectResponse<DocumentUploadResponse> uploadDocument(byte[] requestData,
                                                                 EncryptionContext encryptionContext,
                                                                 PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, OnboardingProcessException {

        final String operationDescription = "uploading document for verification";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEncryptionContext(encryptionContext, operationDescription);
        checkRequest(requestData, operationDescription);

        // Extract user ID from onboarding process for current activation
        final OwnerId ownerId = extractOwnerId(encryptionContext);

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
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process identifier is invalid.
     */
    public ObjectResponse<DocumentStatusResponse> checkDocumentStatus(ObjectRequest<DocumentStatusRequest> request,
                                                                      PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "checking document verification status";
        checkApiAuthentication(apiAuthentication, operationDescription);
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
     * @param encryptionContext Encryption context.
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
            EncryptionContext encryptionContext,
            PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, DocumentVerificationException, PowerAuthEncryptionException, OnboardingProcessException, RemoteCommunicationException {

        final String operationDescription = "initializing document verification SDK";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEncryptionContext(encryptionContext, operationDescription);
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
     * @param encryptionContext Encryption context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @Transactional
    public ResponseEntity<ObjectResponse<PresenceCheckInitResponse>> initPresenceCheck(ObjectRequest<PresenceCheckInitRequest> request,
                                                      EncryptionContext encryptionContext,
                                                      PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "initializing presence check";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkEncryptionContext(encryptionContext, operationDescription);
        checkRequestObject(request, operationDescription);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.PRESENCE_CHECK_INIT);

        @SuppressWarnings("unchecked")
        final Class<ObjectResponse<PresenceCheckInitResponse>> presenceCheckInitResponseClass = (Class<ObjectResponse<PresenceCheckInitResponse>>) new ObjectResponse<PresenceCheckInitResponse>().getClass();
        return createResponseEntity(stateMachine, presenceCheckInitResponseClass);
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
    @Transactional
    public ResponseEntity<Response> submitPresenceCheck(ObjectRequest<PresenceCheckSubmitRequest> request,
                                                        PowerAuthApiAuthentication apiAuthentication)
            throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, OnboardingProcessException {

        final String operationDescription = "submitting presence check";
        checkApiAuthentication(apiAuthentication, operationDescription);
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
     * @param apiAuthentication PowerAuth authentication.
     * @return Send OTP response.
     * @throws IdentityVerificationException Thrown when identity verification is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     */
    @Transactional
    public ResponseEntity<Response> resendOtp(
            final ObjectRequest<IdentityVerificationOtpSendRequest> request,
            final PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, PowerAuthEncryptionException, OnboardingProcessException {

        checkRequestObject(request, "resending OTP during identity verification");

        // Extract user ID from onboarding process for current activation, lock onboarding process
        final OwnerId ownerId = extractOwnerId(apiAuthentication);
        final String processId = request.getRequestObject().getProcessId();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, {}", processId);
        onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineService.processStateMachineEvent(ownerId, processId, OnboardingEvent.OTP_VERIFICATION_RESEND);
        return createResponseEntity(stateMachine);
    }

    /**
     * Verify an OTP code received from the user.
     * @param request Presence check initialization request.
     * @param encryptionContext Encryption context.
     * @return Send OTP response.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Transactional
    public ObjectResponse<OtpVerifyResponse> verifyOtp(ObjectRequest<IdentityVerificationOtpVerifyRequest> request,
                                                       EncryptionContext encryptionContext)
            throws PowerAuthEncryptionException, OnboardingProcessException {

        checkEncryptionContext(encryptionContext, "verifying OTP during identity verification");
        checkRequestObject(request, "verifying OTP during identity verification");

        // Extract user ID from onboarding process for current activation
        final OwnerId ownerId = extractOwnerId(encryptionContext);
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
                            PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PowerAuthEncryptionException, DocumentVerificationException, PresenceCheckException, RemoteCommunicationException, OnboardingProcessException, IdentityVerificationException, OnboardingProcessLimitException {

        final String operationDescription = "performing document cleanup";
        checkApiAuthentication(apiAuthentication, operationDescription);
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
     * @param apiAuthentication PowerAuth authentication.
     * @return Consent text.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     */
    @Transactional
    public ObjectResponse<OnboardingConsentTextResponse> fetchConsentText(
            final ObjectRequest<OnboardingConsentTextRequest> request,
            final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthEncryptionException, PowerAuthTokenInvalidException {

        checkApiAuthentication(apiAuthentication, "obtaining user consent text");
        checkRequestObject(request, "obtaining user consent text");

        final OnboardingConsentTextRequest requestObject = request.getRequestObject();
        logger.debug("Returning consent for {}", requestObject);
        OnboardingConsentTextRequestValidator.validate(requestObject);

        final OwnerId ownerId = extractOwnerId(apiAuthentication);
        final String processId = requestObject.getProcessId();

        logger.debug("Onboarding process will not be locked, {}", processId);
        onboardingService.verifyProcessId(ownerId, processId, OnboardingStatus.NOT_YET_COMPLETED);

        return new ObjectResponse<>(onboardingService.fetchConsentText(requestObject));
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
    @Transactional
    public Response approveConsent(
            final ObjectRequest<OnboardingConsentApprovalRequest> request,
            final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException, PowerAuthAuthenticationException, PowerAuthEncryptionException {

        final String operationDescription = "approving user consent";
        checkApiAuthentication(apiAuthentication, operationDescription);
        checkRequestObject(request, operationDescription);

        final OnboardingConsentApprovalRequest requestObject = request.getRequestObject();
        logger.debug("Approving consent for {}", requestObject);
        OnboardingConsentApprovalRequestValidator.validate(requestObject);

        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = requestObject.getProcessId();

        logger.debug("Onboarding process will not be locked, {}", processId);
        onboardingService.verifyProcessId(ownerId, processId, OnboardingStatus.NOT_YET_COMPLETED);

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
            throw new PowerAuthTokenInvalidException("Unable to verify device registration when " + description);
        }
    }

    /**
     * Checks if the request was correctly decrypted
     * @param encryptionContext ECIES encryption context
     * @param description Additional description
     * @throws PowerAuthEncryptionException When the ECIES encryption context does not exist
     */
    private void checkEncryptionContext(@Nullable EncryptionContext encryptionContext, String description) throws PowerAuthEncryptionException {
        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES encryption failed when " + description);
        }
    }

    private void checkRequest(@Nullable Object request, String description) throws PowerAuthEncryptionException {
        if (request == null) {
            throw new PowerAuthEncryptionException("Invalid request received when " + description);
        }
    }

    private void checkRequestObject(@Nullable ObjectRequest<?> request, String description) throws PowerAuthEncryptionException {
        if (request == null || request.getRequestObject() == null) {
            throw new PowerAuthEncryptionException("Invalid request received when " + description);
        }
    }

    /**
     * Extract owner identification from an Encryption context. The onboarding process is not locked.
     *
     * @param encryptionContext Encryption context.
     * @return Owner identification.
     */
    private OwnerId extractOwnerId(EncryptionContext encryptionContext) throws OnboardingProcessException {
        return extractOwnerId(encryptionContext.getActivationId());
    }

    /**
     * Extract owner identification from PowerAuth authentication. The onboarding process is not locked.
     *
     * @param apiAuthentication PowerAuth authentication.
     * @return Owner identification.
     */
    private OwnerId extractOwnerId(final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException {
        return extractOwnerId(apiAuthentication.getActivationContext().getActivationId());
    }

    /**
     * Extract owner identification from activation ID. The onboarding process is not locked.
     *
     * @param activationId Activation ID.
     * @return Owner identification.
     */
    private OwnerId extractOwnerId(final String activationId) throws OnboardingProcessException {
        final OnboardingProcessEntity onboardingProcess = onboardingService.findExistingProcessWithVerificationInProgress(activationId);
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(onboardingProcess.getActivationId());
        ownerId.setUserId(onboardingProcess.getUserId());
        return ownerId;
    }

    private ResponseEntity<Response> createResponseEntity(final StateMachine<OnboardingState, OnboardingEvent> stateMachine) {
        return createResponseEntity(stateMachine, Response.class);
    }

    private <T> ResponseEntity<T> createResponseEntity(final StateMachine<OnboardingState, OnboardingEvent> stateMachine, Class<T> responseClass) {
        final T response = stateMachine.getExtendedState().get(ExtendedStateVariable.RESPONSE_OBJECT, responseClass);
        final HttpStatus status = stateMachine.getExtendedState().get(ExtendedStateVariable.RESPONSE_STATUS, HttpStatus.class);
        Assert.state(response != null && status != null, "Missing one of important values to generate response entity, response=%s, status=%s".formatted(response, status));
        return new ResponseEntity<>(response, status);
    }

}
