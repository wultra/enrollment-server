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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.configuration.OnboardingConfig;
import com.wultra.app.enrollmentserver.database.OnboardingOtpRepository;
import com.wultra.app.enrollmentserver.database.OnboardingProcessRepository;
import com.wultra.app.enrollmentserver.database.entity.OnboardingOtp;
import com.wultra.app.enrollmentserver.database.entity.OnboardingProcess;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingProviderException;
import com.wultra.app.enrollmentserver.impl.service.internal.IdGeneratorService;
import com.wultra.app.enrollmentserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.enrollmentserver.impl.service.internal.OtpGeneratorService;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.request.*;
import com.wultra.app.enrollmentserver.model.response.OnboardingStartResponse;
import com.wultra.app.enrollmentserver.model.response.OnboardingStatusResponse;
import com.wultra.app.enrollmentserver.model.response.OtpVerifyResponse;
import com.wultra.app.enrollmentserver.provider.OnboardingProvider;
import io.getlime.core.rest.model.base.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

/**
 * Service implementing the onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    private final OnboardingProcessRepository onboardingProcessRepository;
    private final OnboardingOtpRepository onboardingOtpRepository;
    private final JsonSerializationService serializer;
    private final IdGeneratorService idGenerator;
    private final OtpGeneratorService otpGeneratorService;
    private final OnboardingConfig config;

    private OnboardingProvider onboardingProvider;

    /**
     * Service constructor.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingOtpRepository Onboarding OTP code repository.
     * @param serializer JSON serialization service.
     * @param idGenerator ID generator service.
     * @param otpGeneratorService OTP generator service.
     * @param config Onboarding configuration.
     */
    public OnboardingService(OnboardingProcessRepository onboardingProcessRepository, OnboardingOtpRepository onboardingOtpRepository, JsonSerializationService serializer, IdGeneratorService idGenerator, OtpGeneratorService otpGeneratorService, OnboardingConfig config) {
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.onboardingOtpRepository = onboardingOtpRepository;
        this.serializer = serializer;
        this.idGenerator = idGenerator;
        this.otpGeneratorService = otpGeneratorService;
        this.config = config;
    }

    /**
     * Set onboarding provider via setter injection.
     * @param onboardingProvider Onboarding provider.
     */
    @Autowired(required = false)
    public void setOnboardingProvider(OnboardingProvider onboardingProvider) {
        this.onboardingProvider = onboardingProvider;
    }

    /**
     * Start an onboarding process.
     * @param request Onboarding start request.
     * @return Onboarding start response.
     * @throws OnboardingProcessException Thrown in case onboarding process fails.
     */
    @Transactional
    public OnboardingStartResponse startOnboarding(OnboardingStartRequest request) throws OnboardingProcessException {
        if (onboardingProvider == null) {
            logger.error("Onboarding provider is not available. Implement an onboarding provider and make it accessible using autowiring.");
            throw new OnboardingProcessException();
        }
        Map<String, Object> identification = request.getIdentification();
        String identificationData = serializer.serialize(identification);

        // Lookup user using identification attributes
        String userId;
        try {
            userId = onboardingProvider.lookupUser(identification);
        } catch (OnboardingProviderException e) {
            logger.warn("User look failed, error: {}", e.getMessage(), e);
            throw new OnboardingProcessException();
        }

        // Check for brute force attacks
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.HOUR, -24);
        Date timestampCheckStart = c.getTime();
        int existingProcessCount = onboardingProcessRepository.countProcessesAfterTimestamp(userId, timestampCheckStart);
        if (existingProcessCount >= config.getMaxProcessCountPerDay()) {
            logger.warn("Maximum number of processes per day reached for user: " + userId);
            throw new OnboardingProcessException();
        }

        Optional<OnboardingProcess> processOptional = onboardingProcessRepository.findExistingProcess(userId);
        OnboardingProcess process;
        String processId;
        if (processOptional.isPresent()) {
            // Resume an existing process
            process = processOptional.get();
            processId = process.getId();
            // Use latest identification data
            process.setIdentificationData(identificationData);
            process.setTimestampLastUpdated(new Date());
        } else {
            // Create an onboarding process
            processId = idGenerator.generateProcessId();
            process = new OnboardingProcess();
            process.setIdentificationData(identificationData);
            process.setId(processId);
            process.setStatus(OnboardingStatus.IN_PROGRESS);
            process.setUserId(userId);
            process.setTimestampCreated(new Date());
        }
        process = onboardingProcessRepository.save(process);
        // Create an OTP code
        String otpCode = createOtpCode(processId);
        // Send the OTP code
        try {
            onboardingProvider.sendOtpCode(userId, otpCode, false);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code delivery failed, error: {}", e.getMessage(), e);
            throw new OnboardingProcessException();
        }
        OnboardingStartResponse response = new OnboardingStartResponse();
        response.setProcessId(processId);
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
    public Response resendOtp(OtpResendRequest request) throws OnboardingProcessException {
        String processId = request.getProcessId();
        OnboardingProcess process = findProcess(processId);
        // Do not allow spamming by OTP codes
        Date lastDate = onboardingOtpRepository.getNewestOtpCreatedTimestamp(processId);
        int resendPeriod = config.getResendPeriod();
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(lastDate);
        c.add(Calendar.SECOND, resendPeriod);
        Date allowedDate = c.getTime();
        if (allowedDate.after(new Date())) {
            logger.warn("Resend OTP functionality is not available yet for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        String userId = process.getUserId();
        Optional<OnboardingOtp> otpOptional = onboardingOtpRepository.findFirstByProcessIdOrderByTimestampCreatedDesc(processId);
        if (!otpOptional.isPresent()) {
            logger.warn("Onboarding OTP not found for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingOtp existingOtp = otpOptional.get();
        existingOtp.setStatus(OtpStatus.FAILED);
        existingOtp.setTimestampLastUpdated(new Date());
        onboardingOtpRepository.save(existingOtp);
        // Create an OTP code
        String otpCode = createOtpCode(processId);
        // Resend the OTP code
        try {
            onboardingProvider.sendOtpCode(userId, otpCode, true);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code resend failed, error: {}", e.getMessage(), e);
            throw new OnboardingProcessException();
        }
        return new Response();
    }

    /**
     * Verify an OTP code.
     * @param processId Process identifier.
     * @param otpCode OTP code sent by the user.
     * @return Verify OTP code response.
     * @throws OnboardingProcessException Thrown when process or OTP code is not found.
     */
    @Transactional
    public OtpVerifyResponse verifyOtp(String processId, String otpCode) throws OnboardingProcessException {
        OnboardingProcess process = findProcess(processId);
        Optional<OnboardingOtp> otpOptional = onboardingOtpRepository.findFirstByProcessIdOrderByTimestampCreatedDesc(processId);
        if (!otpOptional.isPresent()) {
            logger.warn("Onboarding OTP not found for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingOtp otp = otpOptional.get();
        // Verify OTP code
        boolean verified = false;
        int remainingAttempts = 0;
        int failedAttempts = onboardingOtpRepository.getFailedAttemptsByProcess(processId);
        int maxFailedAttempts = config.getMaxFailedAttempts();
        if (otp.getStatus() == OtpStatus.ACTIVE && failedAttempts < maxFailedAttempts) {
            if (otp.getOtpCode().equals(otpCode)) {
                verified = true;
                otp.setStatus(OtpStatus.VERIFIED);
                otp.setTimestampVerified(new Date());
            } else {
                otp.setFailedAttempts(otp.getFailedAttempts() + 1);
                failedAttempts++;
                if (failedAttempts >= maxFailedAttempts) {
                    otp.setStatus(OtpStatus.FAILED);
                    otp.setErrorDetail("maxFailedAttempts");
                    // Onboarding process is failed, update it
                    process.setStatus(OnboardingStatus.FAILED);
                    process.setTimestampLastUpdated(new Date());
                    process.setErrorDetail("maxFailedAttempts");
                    process = onboardingProcessRepository.save(process);
                }
            }
            otp.setTimestampLastUpdated(new Date());
            onboardingOtpRepository.save(otp);
        }

        OtpVerifyResponse response = new OtpVerifyResponse();
        response.setProcessId(processId);
        response.setOnboardingStatus(process.getStatus());
        response.setVerified(verified);
        response.setRemainingAttempts(remainingAttempts);
        return response;
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
        Optional<OnboardingProcess> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found, process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingProcess process = processOptional.get();
        OnboardingStatusResponse response = new OnboardingStatusResponse();
        response.setProcessId(processId);
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
        Optional<OnboardingProcess> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found, process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        Optional<OnboardingOtp> otpOptional = onboardingOtpRepository.findFirstByProcessIdOrderByTimestampCreatedDesc(processId);
        // Fail current OTP, if it is present
        if (otpOptional.isPresent()) {
            OnboardingOtp otp = otpOptional.get();
            if (otp.getStatus() != OtpStatus.FAILED) {
                otp.setStatus(OtpStatus.FAILED);
                otp.setTimestampLastUpdated(new Date());
                otp.setErrorDetail("canceled");
                onboardingOtpRepository.save(otp);
            }
        }
        OnboardingProcess process = processOptional.get();
        process.setStatus(OnboardingStatus.FAILED);
        process.setTimestampLastUpdated(new Date());
        process.setErrorDetail("canceled");
        onboardingProcessRepository.save(process);
        return new Response();
    }

    /**
     * Find an onboarding process.
     * @param processId Process identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OnboardingProcess findProcess(String processId) throws OnboardingProcessException {
        Optional<OnboardingProcess> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found, process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        return processOptional.get();
    }

    /**
     * Update a process entity in database.
     * @param process Onboarding process entity.
     * @return Updated onboarding process entity.
     */
    public OnboardingProcess updateProcess(OnboardingProcess process) {
        return onboardingProcessRepository.save(process);
    }

    /**
     * Check for inactive processes and terminate them.
     */
    @Transactional
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    public void terminateInactiveProcesses() {
        int expirationSeconds = config.getProcessExpirationTime();
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.SECOND, -expirationSeconds);
        Date expirationDate = c.getTime();
        onboardingProcessRepository.terminateOldProcesses(expirationDate);
        onboardingOtpRepository.terminateOldOtps(expirationDate);
    }

    /**
     * Create an OTP code.
     * @param processId Process ID.
     * @return Generated OTP code.
     */
    private String createOtpCode(String processId) throws OnboardingProcessException {
        OnboardingOtp otp = new OnboardingOtp();
        String otpId = idGenerator.generateOtpId();
        int otpLength = config.getOtpLength();
        String otpCode = otpGeneratorService.generateOtpCode(otpLength);
        otp.setId(otpId);
        otp.setProcessId(processId);
        otp.setOtpCode(otpCode);
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setTimestampCreated(new Date());
        otp.setFailedAttempts(0);
        onboardingOtpRepository.save(otp);
        return otpCode;
    }

}