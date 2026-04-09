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
package com.wultra.app.onboardingserver.impl.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.*;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.OnboardingConsentTextResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.OnboardingStartResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.OnboardingStatusResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.ConfigurationDataDto;
import com.wultra.app.enrollmentserver.model.enumeration.ActivationType;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessConfigurationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntityWrapper;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.errorhandling.TooManyProcessesException;
import com.wultra.app.onboardingserver.impl.util.DateUtil;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.ApproveConsentRequest;
import com.wultra.app.onboardingserver.provider.model.request.ConsentTextRequest;
import com.wultra.app.onboardingserver.provider.model.request.SendOtpCodeRequest;
import com.wultra.app.onboardingserver.provider.model.response.ApproveConsentResponse;
import com.wultra.app.onboardingserver.provider.model.response.LookupUserResponse;
import com.wultra.core.http.common.request.RequestContext;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.client.model.response.InitActivationResponse;
import com.wultra.security.powerauth.crypto.lib.generator.IdentifierGenerator;
import com.wultra.security.powerauth.crypto.lib.model.exception.CryptoProviderException;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * Service implementing specific behavior for the onboarding process. Shared behavior is inherited from {@link CommonOnboardingService}.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@Slf4j
public class OnboardingServiceImpl extends CommonOnboardingService {

    private static final String IDENTIFICATION_DATA_DATE_FORMAT = "yyyy-MM-dd";

    private final OnboardingProcessConfigurationRepository onboardingProcessConfigurationRepository;

    private final OnboardingConfig onboardingConfig;
    private final IdentityVerificationConfig identityVerificationConfig;
    private final OtpServiceImpl otpService;

    private final ActivationService activationService;

    private final LookupUserService lookupUserService;

    /**
     * Configuration data for client integration
     */
    private final ConfigurationDataDto integrationConfigDto;

    // Special instance of ObjectMapper for normalized serialization of identification data
    private final ObjectMapper normalizedMapper = JsonMapper
            .builder()
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build()
            .setDateFormat(new SimpleDateFormat(IDENTIFICATION_DATA_DATE_FORMAT))
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    private final OnboardingProvider onboardingProvider;

    private final IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    /**
     * Service constructor.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param config Onboarding configuration.
     * @param identityVerificationConfig Identity verification config.
     * @param otpService OTP service.
     * @param auditService audit service.
     */
    @Autowired
    public OnboardingServiceImpl(
            final OnboardingProcessRepository onboardingProcessRepository,
            final OnboardingProcessConfigurationRepository onboardingProcessConfigurationRepository,
            final OnboardingConfig config,
            final IdentityVerificationConfig identityVerificationConfig,
            final OtpServiceImpl otpService,
            final ActivationService activationService,
            final OnboardingProvider onboardingProvider,
            final LookupUserService lookupUserService,
            final AuditService auditService) {

        super(onboardingProcessRepository, auditService);
        this.onboardingConfig = config;
        this.identityVerificationConfig = identityVerificationConfig;
        this.otpService = otpService;
        this.activationService = activationService;
        this.onboardingProvider = onboardingProvider;
        this.lookupUserService = lookupUserService;
        this.integrationConfigDto = new ConfigurationDataDto();
        this.onboardingProcessConfigurationRepository = onboardingProcessConfigurationRepository;
        integrationConfigDto.setOtpResendPeriod(onboardingConfig.getOtpResendPeriod().toString());
        integrationConfigDto.setOtpResendPeriodSeconds(onboardingConfig.getOtpResendPeriod().toSeconds());
    }

    /**
     * Start an onboarding process.
     * @param request Onboarding start request.
     * @param requestContext Request context.
     * @return Onboarding start response.
     * @throws OnboardingProcessException Thrown in case onboarding process fails.
     * @throws TooManyProcessesException Thrown in case too many onboarding processes are started.
     * @throws InvalidRequestObjectException Thrown in case request is invalid.
     */
    @Transactional
    public OnboardingStartResponse startOnboarding(
            final OnboardingStartRequest request,
            final RequestContext requestContext,
            final EncryptionContext encryptionContext) throws OnboardingProcessException, OnboardingOtpDeliveryException, TooManyProcessesException, InvalidRequestObjectException, RemoteCommunicationException {

        final Map<String, Object> identification = request.identification();
        final String identificationData = parseIdentificationData(identification);
        final Map<String, Object> fdsData = request.fdsData();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock");
        final OnboardingProcessEntity process = onboardingProcessRepository.findByIdentificationDataAndStatusWithLock(identificationData, OnboardingStatus.ACTIVATION_IN_PROGRESS)
                .map(it -> resumeExistingProcess(it, identification, fdsData, requestContext))
                .orElseGet(() -> createNewProcessAndLookupUser(request, identificationData, requestContext));

        // Check for brute force attacks
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -24);
        final Date timestampCheckStart = c.getTime();
        final String userId = process.getUserId();
        final int existingProcessCount = onboardingProcessRepository.countByUserIdAndTimestamp(userId, timestampCheckStart);
        if (existingProcessCount > onboardingConfig.getMaxProcessCountPerDay()) {
            process.setStatus(OnboardingStatus.FAILED);
            process.setErrorDetail(OnboardingProcessEntity.ERROR_TOO_MANY_PROCESSES_PER_USER);
            process.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            final Date now = new Date();
            process.setTimestampLastUpdated(now);
            process.setTimestampFailed(now);
            onboardingProcessRepository.save(process);
            auditService.audit(process, "Maximum number of processes per day reached for user: {}", userId);
            throw new TooManyProcessesException("Maximum number of processes per day reached for user: " + userId);
        }

        final Optional<String> otp = createAndSendOtp(process, userId);

        final ActivationService.InitActivationContext initActivationContext = ActivationService.InitActivationContext.builder()
                .applicationKey(encryptionContext.getApplicationKey())
                .userId(userId)
                .otp(otp.orElse(null))
                .build();

        final var activationType = process.getProcessConfiguration().getConfiguration().activationType();
        final InitActivationResponse initActivationResponse = initActivation(initActivationContext, activationType);
        process.setActivationId(initActivationResponse.getActivationId());
        onboardingProcessRepository.save(process);

        return OnboardingStartResponse.builder()
                .processId(process.getId())
                .onboardingStatus(process.getStatus())
                .config(integrationConfigDto)
                .activationCode(initActivationResponse.getActivationCode())
                .activationType(convert(activationType))
                .build();
    }

    private Optional<String> createAndSendOtp(final OnboardingProcessEntity process, final String userId) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        if (isActivationOtpDisabled(process)) {
            logger.info("Activation OTP is disabled for process type: {}", process.getProcessConfiguration().getProcessType());
            return Optional.empty();
        }

        final String otpCode = otpService.createOtpCode(process, OtpType.ACTIVATION);
        if (userId == null) {
            logger.info("User ID is null, OTP is not sent");
            return Optional.empty();
        } else {
            logger.debug("Sending OTP for user ID: {}", userId);
            sendOtp(process, otpCode);
            return Optional.of(otpCode);
        }
    }

    private static boolean isActivationOtpDisabled(final OnboardingProcessEntity process) {
        return !process.getProcessConfiguration().getConfiguration().otpForIdentification();
    }

    private static ActivationType convert(final OnboardingProcessConfigurationValue.ActivationType source) {
        return switch(source) {
            case CODE -> ActivationType.CODE;
            case IDENTITY -> ActivationType.IDENTITY;
        };
    }

    private InitActivationResponse initActivation(
            final ActivationService.InitActivationContext request,
            final OnboardingProcessConfigurationValue.ActivationType activationType) throws RemoteCommunicationException {

        if (activationType == OnboardingProcessConfigurationValue.ActivationType.IDENTITY) {
            logger.info("ActivationCode is not generated for activationType=IDENTITY");
            return new InitActivationResponse();
        } else if (activationType == OnboardingProcessConfigurationValue.ActivationType.CODE && request.userId() != null) {
            return activationService.initActivation(request);
        } else {
            logger.info("User ID is null, generating fake activationCode");
            final InitActivationResponse response = new InitActivationResponse();
            response.setActivationCode(generateActivationCode());
            return response;
        }
    }

    private String generateActivationCode() {
        try {
            return identifierGenerator.generateActivationCode();
        } catch (CryptoProviderException e) {
            logger.error("Failed to generate fake activation code: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Resend an OTP code.
     * @param request Resend OTP code request.
     * @return Resend OTP code response.
     * @throws OnboardingProcessException Thrown when OTP resend fails.
     */
    @Transactional
    public Response resendOtp(final OnboardingOtpResendRequest request) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        final String processId = request.getProcessId();
        final OnboardingProcessEntity process = findProcessWithLock(processId);
        if (isActivationOtpDisabled(process)) {
            logger.warn("OTP is disabled for process type: {}", process.getProcessConfiguration().getProcessType());
            return new Response();
        }

        final String userId = process.getUserId();

        final String otpCode = otpService.createOtpCodeForResend(process, OtpType.ACTIVATION);

        if (userId == null) {
            logger.debug("User ID is not present, OTP is not resent");
        } else {
            logger.debug("Resending OTP for user ID: {}", userId);
            resendOtp(process, otpCode);
        }

        return new Response();
    }

    /**
     * Get onboarding process status.
     * @param request Onboarding status request.
     * @return Onboarding status response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(OnboardingStatusRequest request) throws OnboardingProcessException {
        final String processId = request.getProcessId();
        final OnboardingProcessEntity process = findProcess(request.getProcessId());
        OnboardingStatusResponse response = new OnboardingStatusResponse();
        response.setProcessId(processId);

        // Check for expiration of onboarding process
        if (hasProcessExpired(process)) {
            response.setOnboardingStatus(OnboardingStatus.FAILED);
            return response;
        }

        response.setOnboardingStatus(process.getStatus());
        response.setConfig(integrationConfigDto);
        return response;
    }

    /**
     * Perform cleanup of an onboarding process.
     * @param request Onboarding process cleanup request.
     * @return Onboarding process cleanup response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Transactional
    public Response performCleanup(OnboardingCleanupRequest request) throws OnboardingProcessException {
        final String processId = request.getProcessId();
        logger.info("Cleaning up process ID: {}", processId);

        final OnboardingProcessEntity process = findProcessWithLock(processId);

        otpService.cancelOtp(process, OtpType.ACTIVATION);
        otpService.cancelOtp(process, OtpType.USER_VERIFICATION);

        removeActivation(process);

        process.setStatus(OnboardingStatus.FAILED);
        process.setErrorDetail(OnboardingProcessEntity.ERROR_PROCESS_CANCELED);
        process.setErrorOrigin(ErrorOrigin.USER_REQUEST);
        process.setTimestampLastUpdated(new Date());
        process.setTimestampFailed(new Date());
        process.setActivationRemoved(true);
        onboardingProcessRepository.save(process);

        auditService.audit(process, "Process cleaned up for user: {}", process.getUserId());
        return new Response();
    }

    /**
     * Verify process identifier and lock the process until the end of the transaction.
     * @param ownerId Owner identification.
     * @param processId Process identifier from request.
     * @param onboardingStatus Expected onboarding process status.
     * @throws OnboardingProcessException Thrown in case process identifier is invalid.
     */
    public OnboardingProcessEntity verifyProcessIdAndLock(OwnerId ownerId, String processId, OnboardingStatus onboardingStatus) throws OnboardingProcessException {
        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, process ID: {}", processId);
        final OnboardingProcessEntity process = onboardingProcessRepository.findByActivationIdAndStatusWithLock(ownerId.getActivationId(), onboardingStatus)
                .orElseThrow(() -> new OnboardingProcessException("Onboarding process not found, activation ID: " + ownerId.getActivationId()));
        final String expectedProcessId = process.getId();

        if (!expectedProcessId.equals(processId)) {
            throw new OnboardingProcessException(
                    String.format("Invalid process ID received in request: %s, %s", processId, ownerId));
        }
        return process;
    }

    /**
     * Check if verify the presence with OTP is enabled for the given process ID.
     *
     * @param processId Process ID.
     * @return {@code true} if verify the presence with OTP feature is enabled in the process configuration, {@code false} otherwise
     * @throws OnboardingProcessException if the onboarding process cannot be found or its configuration cannot be read.
     */
    @Transactional(readOnly = true)
    public boolean isVerifyPresenceWithOtpEnabled(final String processId) throws OnboardingProcessException {
        return findProcess(processId)
                .getProcessConfiguration()
                .getConfiguration()
                .verifyPresenceWithOtp();
    }

    /**
     * Verify process identifier.
     * @param ownerId Owner identification.
     * @param processId Process identifier from request.
     * @throws OnboardingProcessException Thrown in case process identifier is invalid.
     */
    public void verifyProcessId(OwnerId ownerId, String processId, OnboardingStatus onboardingStatus) throws OnboardingProcessException {
        final OnboardingProcessEntity process = onboardingProcessRepository.findByActivationIdAndStatus(ownerId.getActivationId(), onboardingStatus)
                .orElseThrow(() -> new OnboardingProcessException("Onboarding process not found, activation ID: " + ownerId.getActivationId()));
        final String expectedProcessId = process.getId();

        if (!expectedProcessId.equals(processId)) {
            throw new OnboardingProcessException(
                    String.format("Invalid process ID received in request: %s, %s", processId, ownerId));
        }
    }

    /**
     * Verify process identifier.
     * @param ownerId Owner identification.
     * @param processId Process identifier from request.
     * @param onboardingStatuses Onboarding process statuses.
     * @throws OnboardingProcessException Thrown in case process identifier is invalid.
     */
    public void verifyProcessId(OwnerId ownerId, String processId, Collection<OnboardingStatus> onboardingStatuses) throws OnboardingProcessException {
        final OnboardingProcessEntity process = onboardingProcessRepository.findByActivationIdAndStatuses(ownerId.getActivationId(), onboardingStatuses)
                .orElseThrow(() -> new OnboardingProcessException("Onboarding process not found, activation ID: " + ownerId.getActivationId()));
        final String expectedProcessId = process.getId();

        if (!expectedProcessId.equals(processId)) {
            throw new OnboardingProcessException(
                    String.format("Invalid process ID received in request: %s, %s", processId, ownerId));
        }
    }

    /**
     * Find an existing onboarding process with verification in progress by activation identifier.
     * @param activationId Activation identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OnboardingProcessEntity findExistingProcessWithVerificationInProgress(String activationId) throws OnboardingProcessException {
        return onboardingProcessRepository.findByActivationIdAndStatus(activationId, OnboardingStatus.VERIFICATION_IN_PROGRESS)
                .orElseThrow(() -> new OnboardingProcessException("Onboarding process not found, activation ID: " + activationId));
    }

    /**
     * Find an existing onboarding process by activation ID in any state.
     * @param activationId Activation identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OnboardingProcessEntity findProcessByActivationId(String activationId) throws OnboardingProcessException {
        return onboardingProcessRepository.findByActivationId(activationId).orElseThrow(() ->
                new OnboardingProcessException("Onboarding process not found, activation ID: " + activationId));
    }

    /**
     * Provide consent text.
     *
     * @param request consent text request
     * @return consent response
     */
    public OnboardingConsentTextResponse fetchConsentText(final OnboardingConsentTextRequest request) throws OnboardingProcessException {
        final OnboardingProcessEntity process = findProcess(request.getProcessId());
        final String userId = process.getUserId();
        final ConsentTextRequest providerRequest = ConsentTextRequest.builder()
                .processId(request.getProcessId())
                .processType(process.getProcessConfiguration().getProcessType())
                .userId(userId)
                .consentType(request.getConsentType())
                .locale(LocaleContextHolder.getLocale())
                .build();

        try {
            final String consentText = onboardingProvider.fetchConsent(providerRequest);
            auditService.auditOnboardingProviderDebug(process, "Fetched consent text for user: {}", userId);
            final OnboardingConsentTextResponse response = new OnboardingConsentTextResponse();
            response.setConsentText(consentText);
            return response;
        } catch (OnboardingProviderException e) {
            throw new OnboardingProcessException("An error when fetching consent text.", e);
        }
    }

    /**
     * Record dis/approval of consent
     *
     * @param request approval request
     */
    public void approveConsent(final OnboardingConsentApprovalRequest request) throws OnboardingProcessException {
        final OnboardingProcessEntity process = findProcess(request.getProcessId());
        final String userId = process.getUserId();
        final ApproveConsentRequest providerRequest = ApproveConsentRequest.builder()
                .processId(request.getProcessId())
                .processType(process.getProcessConfiguration().getProcessType())
                .userId(userId)
                .consentType(request.getConsentType())
                .approved(request.isApproved())
                .build();

        try {
            final ApproveConsentResponse response = onboardingProvider.approveConsent(providerRequest);
            logger.debug("Got {} for processId={}", response, request.getProcessId());
            if (response.isErrorOccurred()) {
                final String errorDetail = response.getErrorDetail();
                auditService.auditOnboardingProvider(process, "Consent text approval failed for user: {}, error: {}", userId, errorDetail);
                throw new OnboardingProcessException("Consent text approval failed for process: %s, user: %s, error: %s"
                        .formatted(process.getId(), userId, errorDetail));
            }

            process.setConsentAccepted(true);

            auditService.auditOnboardingProvider(process, "Approve consent text for user: {}", userId);
        } catch (OnboardingProviderException e) {
            throw new OnboardingProcessException("An error when approving consent.", e);
        }
    }

    /**
     * Check whether onboarding process has expired.
     * @param onboardingProcess Onboarding process entity.
     * @return Whether onboarding process has expired.
     */
    public boolean hasProcessExpired(OnboardingProcessEntity onboardingProcess) {

        // Check expiration for onboarding process with activation in progress
        if (onboardingProcess.getStatus() == OnboardingStatus.ACTIVATION_IN_PROGRESS) {
            final Duration activationExpiration = onboardingConfig.getActivationExpirationTime();
            final Date createdDateExpirationActivation = DateUtil.convertExpirationToCreatedDate(activationExpiration);
            if (onboardingProcess.getTimestampCreated().before(createdDateExpirationActivation)) {
                return true;
            }
        }

        // Check expiration for onboarding process with identity verification in progress
        if (onboardingProcess.getStatus() == OnboardingStatus.VERIFICATION_IN_PROGRESS) {
            final Duration verificationExpiration = identityVerificationConfig.getVerificationExpirationTime();
            final Date createdDateExpirationVerification = DateUtil.convertExpirationToCreatedDate(verificationExpiration);
            if (onboardingProcess.getTimestampCreated().before(createdDateExpirationVerification)) {
                return true;
            }
        }

        // Check expiration for onboarding process due to process timeout
        final Duration processExpiration = onboardingConfig.getProcessExpirationTime();
        final Date createdDateExpirationProcess = DateUtil.convertExpirationToCreatedDate(processExpiration);
        return onboardingProcess.getTimestampCreated().before(createdDateExpirationProcess);
    }

    @SneakyThrows(OnboardingProcessException.class)
    private OnboardingProcessEntity createNewProcessAndLookupUser(
            final OnboardingStartRequest request,
            final String identificationData,
            final RequestContext requestContext) {

        final OnboardingProcessEntity process = createNewProcess(request, identificationData, requestContext);
        logger.debug("Created process ID: {}", process.getId());

        final Optional<LookupUserResponse> lookupUserResponse = lookupUserService.lookupUser(process, request.identification());
        final String userId = lookupUserResponse.map(LookupUserResponse::getUserId).orElse(null);
        process.setUserId(userId);
        storeConsent(process, lookupUserResponse.map(LookupUserResponse::isConsentNotRequired).orElse(false));
        auditService.audit(process, "Process started for user: {}", process.getUserId());
        return process;
    }

    /**
     * Copy consent result from lookup user response to process if configured to use consent.
     *
     * @param process process to store consent for
     * @param consentAccepted value from lookup user response
     */
    private static void storeConsent(final OnboardingProcessEntity process, final boolean consentAccepted) {
        if (isConsentConfigured(process)) {
            logger.debug("Consent configured for processId: {}, storing user lookup response value: {}", process.getId(), consentAccepted);
            process.setConsentAccepted(consentAccepted);
        } else {
            logger.debug("Consent not configured for processId: {}, ignoring user lookup response", process.getId());
        }
    }

    private static boolean isConsentConfigured(final OnboardingProcessEntity process) {
        return process.getProcessConfiguration().getConfiguration().consentRequired();
    }

    private OnboardingProcessEntity createNewProcess(final OnboardingStartRequest request, final String identificationData, final RequestContext requestContext) throws OnboardingProcessException {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setIdentificationData(identificationData);
        process.setStatus(OnboardingStatus.ACTIVATION_IN_PROGRESS);
        process.setTimestampCreated(new Date());
        process.setProcessConfiguration(fetchProcessConfiguration(request.processType()));
        process.setConsentAccepted(false);
        setProcessCustomData(process, request.fdsData(), requestContext);
        return onboardingProcessRepository.save(process);
    }

    private OnboardingProcessConfigurationEntity fetchProcessConfiguration(final String source) throws OnboardingProcessException {
        final String processType = resolveProcessType(source);
        return onboardingProcessConfigurationRepository.findByProcessType(processType)
                .orElseThrow(() -> new OnboardingProcessException("No configuration found for process type: " + processType));
    }

    private String resolveProcessType(final String processType) throws OnboardingProcessException {
        if (StringUtils.isNotBlank(processType)) {
            return processType;
        }

        logger.debug("Process type missing, looking for default value");
        final String defaultProcessType = onboardingConfig.getDefaultProcessType();
        if (StringUtils.isNotBlank(defaultProcessType)) {
            return defaultProcessType;
        } else {
            throw new OnboardingProcessException("Default process type is not configured.");
        }
    }

    private static void setProcessCustomData(final OnboardingProcessEntity process, final Map<String, Object> fdsData, final RequestContext requestContext) {
        final OnboardingProcessEntityWrapper processWrapper = new OnboardingProcessEntityWrapper(process);
        processWrapper.setLocale(LocaleContextHolder.getLocale());
        processWrapper.setIpAddress(requestContext.getIpAddress());
        processWrapper.setUserAgent(requestContext.getUserAgent());
        processWrapper.setFdsData(fdsData);
    }

    @SneakyThrows(OnboardingProcessException.class)
    private OnboardingProcessEntity resumeExistingProcess(
            final OnboardingProcessEntity process,
            final Map<String, Object> identification,
            final Map<String, Object> fdsData,
            final RequestContext requestContext) {

        logger.debug("Resuming process ID: {}", process.getId());
        process.setTimestampLastUpdated(new Date());
        setProcessCustomData(process, fdsData, requestContext);
        final String userId = lookupUserService.lookupUser(process, identification)
                .map(LookupUserResponse::getUserId)
                .orElse(null);
        if (!process.getUserId().equals(userId)) {
            throw new OnboardingProcessException(
                    String.format("Looked up user ID '%s' does not equal to user ID '%s' of process ID %s",
                            userId, process.getUserId(), process.getId()));
        }
        auditService.audit(process, "Process resumed for user: {}", userId);
        return process;
    }

    private void removeActivation(final OnboardingProcessEntity process) throws OnboardingProcessException {
        final String activationId = process.getActivationId();
        if (activationId != null) {
            try {
                logger.info("Removing activation ID: {} of process ID: {}", activationId, process.getId());
                activationService.removeActivation(activationId);
                auditService.auditActivation(process, activationId, "Remove activation for user: {}", process.getUserId());
            } catch (RemoteCommunicationException e) {
                throw new OnboardingProcessException(
                        String.format("Unable to remove activation ID: %s of process ID: %s", activationId, process.getId()), e);
            }
        }
    }

    private String parseIdentificationData(final Map<String, Object> identification) throws InvalidRequestObjectException {
        try {
            return normalizedMapper.writeValueAsString(identification);
        } catch (JsonProcessingException ex) {
            throw new InvalidRequestObjectException("Invalid identification data: " + identification, ex);
        }
    }

    private void sendOtp(final OnboardingProcessEntity process, final String otpCode) throws OnboardingOtpDeliveryException {
        final SendOtpCodeRequest sendOtpCodeRequest = SendOtpCodeRequest.builder()
                .processId(process.getId())
                .processType(process.getProcessConfiguration().getProcessType())
                .userId(process.getUserId())
                .otpCode(otpCode)
                .resend(false)
                .locale(LocaleContextHolder.getLocale())
                .otpType(SendOtpCodeRequest.OtpType.ACTIVATION)
                .build();
        try {
            onboardingProvider.sendOtpCode(sendOtpCodeRequest);
        } catch (OnboardingProviderException e) {
            throw new OnboardingOtpDeliveryException("OTP code delivery failed, error: " + e.getMessage(), e);
        }

        auditService.auditOnboardingProvider(process, "Sent activation OTP for user: {}", process.getUserId());
    }

    private void resendOtp(final OnboardingProcessEntity process, final String otpCode) throws OnboardingOtpDeliveryException {
        final String userId = process.getUserId();
        final SendOtpCodeRequest sendOtpCodeRequest = SendOtpCodeRequest.builder()
                .processId(process.getId())
                .processType(process.getProcessConfiguration().getProcessType())
                .userId(userId)
                .otpCode(otpCode)
                .locale(LocaleContextHolder.getLocale())
                .resend(true)
                .otpType(SendOtpCodeRequest.OtpType.ACTIVATION)
                .build();
        try {
            onboardingProvider.sendOtpCode(sendOtpCodeRequest);
        } catch (OnboardingProviderException e) {
            throw new OnboardingOtpDeliveryException("OTP code resend failed, error: " + e.getMessage(), e);
        }

        auditService.auditOnboardingProvider(process, "Resent activation OTP for user: {}", userId);
    }
}
