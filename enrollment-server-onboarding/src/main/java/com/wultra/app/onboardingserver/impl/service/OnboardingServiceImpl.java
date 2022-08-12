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
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.errorhandling.TooManyProcessesException;
import com.wultra.app.onboardingserver.impl.util.DateUtil;
import com.wultra.app.onboardingserver.provider.*;
import io.getlime.core.rest.model.base.response.Response;
import lombok.SneakyThrows;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

/**
 * Service implementing specific behavior for the onboarding process. Shared behavior is inherited from {@link CommonOnboardingService}.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OnboardingServiceImpl extends CommonOnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingServiceImpl.class);

    private static final String IDENTIFICATION_DATA_DATE_FORMAT = "yyyy-MM-dd";

    private final OnboardingConfig onboardingConfig;
    private final IdentityVerificationConfig identityVerificationConfig;
    private final OtpServiceImpl otpService;

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
     */
    @Autowired
    public OnboardingServiceImpl(
            final OnboardingProcessRepository onboardingProcessRepository,
            final OnboardingConfig config,
            final IdentityVerificationConfig identityVerificationConfig,
            final OtpServiceImpl otpService,
            final OnboardingProvider onboardingProvider) {

        super(onboardingProcessRepository);
        this.onboardingConfig = config;
        this.identityVerificationConfig = identityVerificationConfig;
        this.otpService = otpService;
        this.onboardingProvider = onboardingProvider;
    }

    /**
     * Start an onboarding process.
     * @param request Onboarding start request.
     * @return Onboarding start response.
     * @throws OnboardingProcessException Thrown in case onboarding process fails.
     * @throws TooManyProcessesException Thrown in case too many onboarding processes are started.
     * @throws InvalidRequestObjectException Thrown in case request is invalid.
     */
    @Transactional
    public OnboardingStartResponse startOnboarding(OnboardingStartRequest request) throws OnboardingProcessException, OnboardingOtpDeliveryException, TooManyProcessesException, InvalidRequestObjectException {
        final Map<String, Object> identification = request.getIdentification();

        final String identificationData;
        try {
            identificationData = normalizedMapper.writeValueAsString(identification);
        } catch (JsonProcessingException ex) {
            logger.warn("Invalid identification data: {}", identification);
            throw new InvalidRequestObjectException();
        }

        final OnboardingProcessEntity process = onboardingProcessRepository.findExistingProcessByIdentificationData(identificationData, OnboardingStatus.ACTIVATION_IN_PROGRESS)
                .map(it -> resumeExistingProcess(it, identification))
                .orElseGet(() -> createNewProcess(identification, identificationData));

        // Check for brute force attacks
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, -24);
        Date timestampCheckStart = c.getTime();
        final String userId = process.getUserId();
        int existingProcessCount = onboardingProcessRepository.countProcessesAfterTimestamp(userId, timestampCheckStart);
        if (existingProcessCount > onboardingConfig.getMaxProcessCountPerDay()) {
            process.setStatus(OnboardingStatus.FAILED);
            process.setErrorDetail(OnboardingProcessEntity.ERROR_TOO_MANY_PROCESSES_PER_USER);
            onboardingProcessRepository.save(process);
            logger.warn("Maximum number of processes per day reached for user: {}", userId);
            throw new TooManyProcessesException();
        }

        // Create an OTP code
        String otpCode = otpService.createOtpCode(process, OtpType.ACTIVATION);
        // Send the OTP code
        try {
            final SendOtpCodeRequest sendOtpCodeRequest = SendOtpCodeRequest.builder()
                    .processId(process.getId())
                    .userId(userId)
                    .otpCode(otpCode)
                    .resend(false)
                    .locale(LocaleContextHolder.getLocale())
                    .otpType(SendOtpCodeRequest.OtpType.ACTIVATION)
                    .build();
            onboardingProvider.sendOtpCode(sendOtpCodeRequest);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code delivery failed, error: {}", e.getMessage(), e);
            throw new OnboardingOtpDeliveryException(e);
        }
        OnboardingStartResponse response = new OnboardingStartResponse();
        response.setProcessId(process.getId());
        response.setOnboardingStatus(process.getStatus());
        return response;
    }

    /**
     * Resend an OTP code.
     * @param request Resend OTP code request.
     * @return Resend OTP code response.
     * @throws OnboardingProcessException Thrown when OTP resend fails.
     */
    @Transactional
    public Response resendOtp(OnboardingOtpResendRequest request) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        String processId = request.getProcessId();
        OnboardingProcessEntity process = findProcess(processId);
        String userId = process.getUserId();
        // Create an OTP code
        String otpCode = otpService.createOtpCodeForResend(process, OtpType.ACTIVATION);
        // Resend the OTP code
        try {
            final SendOtpCodeRequest sendOtpCodeRequest = SendOtpCodeRequest.builder()
                    .processId(processId)
                    .userId(userId)
                    .otpCode(otpCode)
                    .locale(LocaleContextHolder.getLocale())
                    .resend(true)
                    .otpType(SendOtpCodeRequest.OtpType.ACTIVATION)
                    .build();
            onboardingProvider.sendOtpCode(sendOtpCodeRequest);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code resend failed, error: {}", e.getMessage(), e);
            throw new OnboardingOtpDeliveryException(e);
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
        String processId = request.getProcessId();
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (processOptional.isEmpty()) {
            logger.warn("Onboarding process not found, process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingProcessEntity process = processOptional.get();
        OnboardingStatusResponse response = new OnboardingStatusResponse();
        response.setProcessId(processId);

        // Check for expiration of onboarding process
        if (hasProcessExpired(process)) {
            // Trigger immediate processing of expired processes
            terminateInactiveProcesses();
            response.setOnboardingStatus(OnboardingStatus.FAILED);
            return response;
        }

        response.setOnboardingStatus(process.getStatus());
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
        String processId = request.getProcessId();
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (processOptional.isEmpty()) {
            logger.warn("Onboarding process not found, process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        otpService.cancelOtp(processOptional.get(), OtpType.ACTIVATION);
        otpService.cancelOtp(processOptional.get(), OtpType.USER_VERIFICATION);
        OnboardingProcessEntity process = processOptional.get();
        process.setStatus(OnboardingStatus.FAILED);
        process.setTimestampLastUpdated(new Date());
        process.setErrorDetail(OnboardingProcessEntity.ERROR_PROCESS_CANCELED);
        onboardingProcessRepository.save(process);
        return new Response();
    }

    /**
     * Verify process identifier.
     * @param ownerId Owner identification.
     * @param processId Process identifier from request.
     * @throws OnboardingProcessException Thrown in case process identifier is invalid.
     */
    public void verifyProcessId(OwnerId ownerId, String processId) throws OnboardingProcessException {
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findProcessByActivationId(ownerId.getActivationId());
        if (processOptional.isEmpty()) {
            logger.error("Onboarding process not found, {}", ownerId);
            throw new OnboardingProcessException();
        }
        String expectedProcessId = processOptional.get().getId();

        if (!expectedProcessId.equals(processId)) {
            logger.warn("Invalid process ID received in request: {}, {}", processId, ownerId);
            throw new OnboardingProcessException();
        }
    }

    /**
     * Find an existing onboarding process with verification in progress by activation identifier.
     * @param activationId Activation identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OnboardingProcessEntity findExistingProcessWithVerificationInProgress(String activationId) throws OnboardingProcessException {
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findExistingProcessForActivation(activationId, OnboardingStatus.VERIFICATION_IN_PROGRESS);
        if (processOptional.isEmpty()) {
            logger.warn("Onboarding process not found, activation ID: {}", activationId);
            throw new OnboardingProcessException();
        }
        return processOptional.get();
    }

    /**
     * Find an existing onboarding process by activation ID in any state.
     * @param activationId Activation identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Override
    public OnboardingProcessEntity findProcessByActivationId(String activationId) throws OnboardingProcessException {
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findProcessByActivationId(activationId);
        if (processOptional.isEmpty()) {
            logger.warn("Onboarding process not found, activation ID: {}", activationId);
            throw new OnboardingProcessException();
        }
        return processOptional.get();
    }

    /**
     * Check for inactive processes and terminate them.
     */
    @Transactional
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    @SchedulerLock(name = "terminateInactiveProcesses", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void terminateInactiveProcesses() {
        // Terminate processes with activations in progress
        final Duration activationExpiration = onboardingConfig.getActivationExpirationTime();
        final Date createdDateExpiredActivations = DateUtil.convertExpirationToCreatedDate(activationExpiration);
        onboardingProcessRepository.terminateExpiredProcessesByStatus(createdDateExpiredActivations, OnboardingStatus.ACTIVATION_IN_PROGRESS, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ACTIVATION);

        // Terminate processes with verifications in progress
        final Duration verificationExpiration = identityVerificationConfig.getVerificationExpirationTime();
        final Date createdDateExpiredVerifications = DateUtil.convertExpirationToCreatedDate(verificationExpiration);
        onboardingProcessRepository.terminateExpiredProcessesByStatus(createdDateExpiredVerifications, OnboardingStatus.VERIFICATION_IN_PROGRESS, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_IDENTITY_VERIFICATION);

        // Terminate OTP codes for all processes
        final Duration otpExpiration = onboardingConfig.getOtpExpirationTime();
        final Date createdDateExpiredOtp = DateUtil.convertExpirationToCreatedDate(otpExpiration);
        otpService.terminateExpiredOtps(createdDateExpiredOtp);

        // Terminate expired processes
        final Duration processExpiration = onboardingConfig.getProcessExpirationTime();
        final Date createdDateExpiredProcesses = DateUtil.convertExpirationToCreatedDate(processExpiration);
        onboardingProcessRepository.terminateExpiredProcesses(createdDateExpiredProcesses, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ONBOARDING);
    }

    /**
     * Provide consent text.
     *
     * @param request consent text request
     * @param userId user identification
     * @return consent response
     */
    public OnboardingConsentTextResponse fetchConsentText(
            final OnboardingConsentTextRequest request,
            final String userId) throws OnboardingProcessException {

        final ConsentTextRequest providerRequest = ConsentTextRequest.builder()
                .processId(request.getProcessId())
                .userId(userId)
                .consentType(request.getConsentType())
                .locale(LocaleContextHolder.getLocale())
                .build();

        try {
            final String consentText = onboardingProvider.fetchConsent(providerRequest);
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
     * @param userId user identification
     */
    public void approveConsent(final OnboardingConsentApprovalRequest request, final String userId) throws OnboardingProcessException {
        final ApproveConsentRequest providerRequest = ApproveConsentRequest.builder()
                .processId(request.getProcessId())
                .userId(userId)
                .consentType(request.getConsentType())
                .approved(request.getApproved())
                .build();

        try {
            final ApproveConsentResponse response = onboardingProvider.approveConsent(providerRequest);
            logger.debug("Got {} for processId={}", response, request.getProcessId());
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

    private String lookupUser(final String processId, final Map<String, Object> identification) throws OnboardingProcessException {
        try {
            final LookupUserRequest lookupUserRequest = LookupUserRequest.builder()
                    .identification(identification)
                    .processId(processId)
                    .build();
            return onboardingProvider.lookupUser(lookupUserRequest).getUserId();
        } catch (OnboardingProviderException e) {
            throw new OnboardingProcessException("User lookup failed, error: " + e.getMessage(), e);
        }
    }

    @SneakyThrows(OnboardingProcessException.class)
    private OnboardingProcessEntity createNewProcess(final Map<String, Object> identification, final String identificationData) {
        final OnboardingProcessEntity process = createNewProcess(identificationData);
        logger.debug("Created process ID: {}", process.getId());
        final String userId = lookupUser(process.getId(), identification);
        process.setUserId(userId);
        return process;
    }

    private OnboardingProcessEntity createNewProcess(final String identificationData) {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setIdentificationData(identificationData);
        process.setStatus(OnboardingStatus.ACTIVATION_IN_PROGRESS);
        process.setTimestampCreated(new Date());
        return onboardingProcessRepository.save(process);
    }

    @SneakyThrows(OnboardingProcessException.class)
    private OnboardingProcessEntity resumeExistingProcess(final OnboardingProcessEntity process, final Map<String, Object> identification) {
        logger.debug("Resuming process ID: {}", process.getId());
        process.setTimestampLastUpdated(new Date());
        final String userId = lookupUser(process.getId(), identification);
        if (!process.getUserId().equals(userId)) {
            throw new OnboardingProcessException(
                    String.format("Looked up user ID '%s' does not equal to user ID '%s' of process ID %s",
                            userId, process.getUserId(), process.getId()));
        }
        return process;
    }
}