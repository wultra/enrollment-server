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
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
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
import com.wultra.app.onboardingserver.provider.model.request.LookupUserRequest;
import com.wultra.app.onboardingserver.provider.model.request.SendOtpCodeRequest;
import com.wultra.app.onboardingserver.provider.model.response.ApproveConsentResponse;
import com.wultra.app.onboardingserver.provider.model.response.LookupUserResponse;
import com.wultra.core.http.common.request.RequestContext;
import io.getlime.core.rest.model.base.response.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Service implementing specific behavior for the onboarding process. Shared behavior is inherited from {@link CommonOnboardingService}.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@Slf4j
public class OnboardingServiceImpl extends CommonOnboardingService {

    private static final String IDENTIFICATION_DATA_DATE_FORMAT = "yyyy-MM-dd";

    private final OnboardingConfig onboardingConfig;
    private final IdentityVerificationConfig identityVerificationConfig;
    private final OtpServiceImpl otpService;

    private final ActivationService activationService;

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
            final OnboardingConfig config,
            final IdentityVerificationConfig identityVerificationConfig,
            final OtpServiceImpl otpService,
            final ActivationService activationService,
            final OnboardingProvider onboardingProvider,
            final AuditService auditService) {

        super(onboardingProcessRepository, auditService);
        this.onboardingConfig = config;
        this.identityVerificationConfig = identityVerificationConfig;
        this.otpService = otpService;
        this.activationService = activationService;
        this.onboardingProvider = onboardingProvider;
        this.integrationConfigDto = ConfigurationDataDto.builder()
                .otpResendPeriod(onboardingConfig.getOtpResendPeriod().toString())
                .otpResendPeriodSeconds(onboardingConfig.getOtpResendPeriod().toSeconds()).build();
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
            final RequestContext requestContext) throws OnboardingProcessException, OnboardingOtpDeliveryException, TooManyProcessesException, InvalidRequestObjectException {

        final Map<String, Object> identification = request.getIdentification();
        final String identificationData = parseIdentificationData(identification);
        final Map<String, Object> fdsData = request.getFdsData();

        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock");
        final OnboardingProcessEntity process = onboardingProcessRepository.findByIdentificationDataAndStatusWithLock(identificationData, OnboardingStatus.ACTIVATION_IN_PROGRESS)
                .map(it -> resumeExistingProcess(it, identification, fdsData, requestContext))
                .orElseGet(() -> createNewProcess(identification, identificationData, fdsData, requestContext));

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

        final String otpCode = otpService.createOtpCode(process, OtpType.ACTIVATION);
        if (userId == null) {
            logger.debug("User ID is null, OTP is not sent");
        } else {
            logger.debug("Sending OTP for user ID: {}", userId);
            sendOtp(process, otpCode);
        }

        OnboardingStartResponse response = new OnboardingStartResponse();
        response.setProcessId(process.getId());
        response.setOnboardingStatus(process.getStatus());
        response.setConfig(integrationConfigDto);
        return response;
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
    @Transactional
    public OnboardingStatusResponse getStatus(OnboardingStatusRequest request) throws OnboardingProcessException {
        final String processId = request.getProcessId();
        final OnboardingProcessEntity process = onboardingProcessRepository.findById(processId).orElseThrow(() ->
                new OnboardingProcessException("Onboarding process not found, process ID: " + processId));
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
    public void verifyProcessIdAndLock(OwnerId ownerId, String processId, OnboardingStatus onboardingStatus) throws OnboardingProcessException {
        logger.debug("Onboarding process will be locked using PESSIMISTIC_WRITE lock, process ID: {}", processId);
        final OnboardingProcessEntity process = onboardingProcessRepository.findByActivationIdAndStatusWithLock(ownerId.getActivationId(), onboardingStatus)
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

    private String lookupUser(final OnboardingProcessEntity process, final Map<String, Object> identification) {
        try {
            final LookupUserRequest lookupUserRequest = LookupUserRequest.builder()
                    .identification(identification)
                    .processId(process.getId())
                    .build();
            final LookupUserResponse response = onboardingProvider.lookupUser(lookupUserRequest);
            auditService.auditOnboardingProvider(process, "Looked up user: {}", response.getUserId());
            if (response.isErrorOccurred()) {
                logger.warn("Business logic error occurred during user lookup, process ID: {}, error detail: {}", process.getId(), response.getErrorDetail());
                process.setErrorOrigin(ErrorOrigin.USER_REQUEST);
                process.setErrorDetail(OnboardingProcessEntity.ERROR_USER_LOOKUP);
                process.setTimestampLastUpdated(new Date());
                onboardingProcessRepository.save(process);
                auditService.auditOnboardingProvider(process, "Error to look up user: {}, {}", response.getUserId(), response.getErrorDetail());
            }
            return response.getUserId();
        } catch (OnboardingProviderException e) {
            logger.info("User lookup failed, using null user ID, error: {}", e.getMessage());
            logger.debug("User lookup failed, using null user ID", e);
            return null;
        }
    }

    private OnboardingProcessEntity createNewProcess(
            final Map<String, Object> identification,
            final String identificationData,
            final Map<String, Object> fdsData,
            final RequestContext requestContext) {

        final OnboardingProcessEntity process = createNewProcess(identificationData, fdsData, requestContext);
        logger.debug("Created process ID: {}", process.getId());
        final String userId = lookupUser(process, identification);
        process.setUserId(userId);
        auditService.audit(process, "Process started for user: {}", userId);
        return process;
    }

    private OnboardingProcessEntity createNewProcess(final String identificationData, final Map<String, Object> fdsData, final RequestContext requestContext) {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setIdentificationData(identificationData);
        process.setStatus(OnboardingStatus.ACTIVATION_IN_PROGRESS);
        process.setTimestampCreated(new Date());
        setProcessCustomData(process, fdsData, requestContext);
        return onboardingProcessRepository.save(process);
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
        final String userId = lookupUser(process, identification);
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
                auditService.auditActivation(process, "Remove activation for user: {}", process.getUserId());
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
