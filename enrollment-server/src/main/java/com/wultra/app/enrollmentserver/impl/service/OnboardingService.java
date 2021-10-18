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
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

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

    private OnboardingProvider onboardingProvider;

    /**
     * Service constructor.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingOtpRepository Onboarding OTP code repository.
     * @param serializer JSON serialization service.
     * @param idGenerator ID generator service.
     * @param otpGeneratorService OTP generator service.
     */
    public OnboardingService(OnboardingProcessRepository onboardingProcessRepository, OnboardingOtpRepository onboardingOtpRepository, JsonSerializationService serializer, IdGeneratorService idGenerator, OtpGeneratorService otpGeneratorService) {
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.onboardingOtpRepository = onboardingOtpRepository;
        this.serializer = serializer;
        this.idGenerator = idGenerator;
        this.otpGeneratorService = otpGeneratorService;
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
        // TODO - check for brute force attacks
        Map<String, Object> identification = request.getIdentification();
        String identificationData = serializer.serialize(identification);
        String processId = idGenerator.generateProcessId();

        // Lookup user using identification attributes
        String userId;
        try {
            userId = onboardingProvider.lookupUser(identification);
        } catch (OnboardingProviderException e) {
            logger.warn("User look failed, error: {}", e.getMessage(), e);
            throw new OnboardingProcessException();
        }
        // TODO - check for existing processes for given user, join process if it is IN_PROGRESS

        // Create an onboarding process
        OnboardingProcess process = new OnboardingProcess();
        process.setId(processId);
        process.setIdentificationData(identificationData);
        process.setStatus(OnboardingStatus.IN_PROGRESS);
        process.setUserId(userId);
        process.setTimestampCreated(new Date());
        onboardingProcessRepository.save(process);
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
        // TODO - check for spamming attempts
        String processId = request.getProcessId();
        OnboardingProcess process = findProcess(processId);
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
        // TODO - failed attempts must take into account all OTPs within process!
        if (otp.getStatus() == OtpStatus.ACTIVE && otp.getFailedAttempts() < 3) {
            // TODO - configuration of max failed attempts
            // TODO - timeout check
            if (otp.getOtpCode().equals(otpCode)) {
                verified = true;
                otp.setStatus(OtpStatus.VERIFIED);
                otp.setTimestampVerified(new Date());
                finishOnboarding(processId);
            } else {
                otp.setFailedAttempts(otp.getFailedAttempts() + 1);
                if (otp.getFailedAttempts() == 3) {
                    otp.setStatus(OtpStatus.FAILED);
                    // Onboarding process is failed
                    process.setStatus(OnboardingStatus.FAILED);
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
                onboardingOtpRepository.save(otp);
            }
        }
        OnboardingProcess process = processOptional.get();
        process.setStatus(OnboardingStatus.FAILED);
        process.setTimestampLastUpdated(new Date());
        onboardingProcessRepository.save(process);
        return new Response();
    }

    // TODO - a service which checks for inactive processes and terminates them

    private void finishOnboarding(String processId) throws OnboardingProcessException {
        // TODO - this needs to be done in activation provider
        OnboardingProcess process = findProcess(processId);
        process.setStatus(OnboardingStatus.FINISHED);
        process.setActivationId(null); // TODO
        onboardingProcessRepository.save(process);
    }

    /**
     * Find an onboarding process.
     * @param processId Process identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    private OnboardingProcess findProcess(String processId) throws OnboardingProcessException {
        Optional<OnboardingProcess> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found, process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        return processOptional.get();
    }

    /**
     * Create an OTP code.
     * @param processId Process ID.
     * @return Generated OTP code.
     */
    private String createOtpCode(String processId) {
        // TODO - add OTP configuration
        OnboardingOtp otp = new OnboardingOtp();
        String otpId = idGenerator.generateOtpId();
        String otpCode = otpGeneratorService.generateOtpCode();
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