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

import com.wultra.app.enrollmentserver.api.model.response.OtpVerifyResponse;
import com.wultra.app.enrollmentserver.configuration.OnboardingConfig;
import com.wultra.app.enrollmentserver.database.OnboardingOtpRepository;
import com.wultra.app.enrollmentserver.database.OnboardingProcessRepository;
import com.wultra.app.enrollmentserver.database.entity.OnboardingOtpEntity;
import com.wultra.app.enrollmentserver.database.entity.OnboardingProcessEntity;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.impl.service.internal.OtpGeneratorService;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

/**
 * Service implementing OTP delivery and verification during onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OtpService {

    private final static Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final OtpGeneratorService otpGeneratorService;
    private final OnboardingOtpRepository onboardingOtpRepository;
    private final OnboardingProcessRepository onboardingProcessRepository;
    private final OnboardingConfig onboardingConfig;

    /**
     * Service constructor.
     * @param otpGeneratorService OTP generator service.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingConfig Onboarding configuration.
     */
    @Autowired
    public OtpService(OtpGeneratorService otpGeneratorService, OnboardingOtpRepository onboardingOtpRepository, OnboardingProcessRepository onboardingProcessRepository, OnboardingConfig onboardingConfig) {
        this.otpGeneratorService = otpGeneratorService;
        this.onboardingOtpRepository = onboardingOtpRepository;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.onboardingConfig = onboardingConfig;
    }

    /**
     * Create an OTP code for onboarding process.
     * @param process Onboarding process.
     * @param otpType OTP type.
     * @return OTP code.
     * @throws OnboardingProcessException Thrown in case OTP code could not be generated.
     */
    public String createOtpCode(OnboardingProcessEntity process, OtpType otpType) throws OnboardingProcessException {
        return generateOtpCode(process, otpType);
    }

    /**
     * Get whether the next generated OTP is going to be a resend OTP for given OTP type.
     * @param process Onboarding process.
     * @param otpType OTP type.
     * @return Whether the next generated OTP is going to be a resend OTP for given OTP type.
     */
    public boolean isNextOtpResend(OnboardingProcessEntity process, OtpType otpType) {
        return onboardingOtpRepository.getOtpCount(process.getId(), otpType) > 0;
    }

    /**
     * Create an OTP code for onboarding process for resend.
     * @param process Onboarding process.
     * @param otpType OTP type.
     * @return OTP code.
     * @throws OnboardingOtpDeliveryException Thrown in case OTP code could not be created yet due to a time limit.
     * @throws OnboardingProcessException Thrown in case a previous OTP code is not found or OTP code could not be generated.
     */
    public String createOtpCodeForResend(OnboardingProcessEntity process, OtpType otpType) throws OnboardingOtpDeliveryException, OnboardingProcessException {
        final String processId = process.getId();
        // Do not allow spamming by OTP codes
        Date lastDate = onboardingOtpRepository.getNewestOtpCreatedTimestamp(processId, otpType);
        int resendPeriod = onboardingConfig.getResendPeriod();
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(lastDate);
        c.add(Calendar.SECOND, resendPeriod);
        Date allowedDate = c.getTime();
        if (allowedDate.after(new Date())) {
            logger.warn("Resend OTP functionality is not available yet for process ID: {}", processId);
            throw new OnboardingOtpDeliveryException();
        }
        Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, otpType);
        if (!otpOptional.isPresent()) {
            logger.warn("Onboarding OTP not found for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingOtpEntity existingOtp = otpOptional.get();
        existingOtp.setStatus(OtpStatus.FAILED);
        existingOtp.setTimestampLastUpdated(new Date());
        onboardingOtpRepository.save(existingOtp);
        // Generate an OTP code
        return generateOtpCode(process, otpType);
    }

    /**
     * Verify an OTP code.
     * @param processId Process identifier.
     * @param otpCode OTP code sent by the user.
     * @param otpType OTP type.
     * @return Verify OTP code response.
     * @throws OnboardingProcessException Thrown when process or OTP code is not found.
     */
    public OtpVerifyResponse verifyOtpCode(String processId, String otpCode, OtpType otpType) throws OnboardingProcessException {
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingProcessEntity process = processOptional.get();
        Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, otpType);
        if (!otpOptional.isPresent()) {
            logger.warn("Onboarding OTP not found for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingOtpEntity otp = otpOptional.get();
        // Verify OTP code
        boolean verified = false;
        int remainingAttempts = 0;
        int failedAttempts = onboardingOtpRepository.getFailedAttemptsByProcess(processId, otpType);
        int maxFailedAttempts = onboardingConfig.getMaxFailedAttempts();
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
     * Cancel an OTP for an onboarding process.
     * @param process Onboarding process.
     */
    public void cancelOtp(OnboardingProcessEntity process, OtpType otpType) {
        String processId = process.getId();
        Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, otpType);
        // Fail current OTP, if it is present
        if (otpOptional.isPresent()) {
            OnboardingOtpEntity otp = otpOptional.get();
            if (otp.getStatus() != OtpStatus.FAILED) {
                otp.setStatus(OtpStatus.FAILED);
                otp.setTimestampLastUpdated(new Date());
                otp.setErrorDetail("canceled");
                onboardingOtpRepository.save(otp);
            }
        }
    }

    /**
     * Terminate OTPs created before specified date.
     * @param createdDateOtp OTP created date.
     */
    public void terminateOldOtps(Date createdDateOtp) {
        onboardingOtpRepository.terminateOldOtps(createdDateOtp);
    }

    /**
     * Generate an OTP code for an onboarding process.
     * @param process Onboarding process.
     * @param otpType OTP type.
     * @return OTP code.
     * @throws OnboardingProcessException Thrown in case OTP code could not be generated.
     */
    private String generateOtpCode(OnboardingProcessEntity process, OtpType otpType) throws OnboardingProcessException {
        OnboardingOtpEntity otp = new OnboardingOtpEntity();
        int otpLength = onboardingConfig.getOtpLength();
        String otpCode = otpGeneratorService.generateOtpCode(otpLength);
        otp.setProcess(process);
        otp.setOtpCode(otpCode);
        otp.setType(otpType);
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setTimestampCreated(new Date());
        otp.setFailedAttempts(0);
        onboardingOtpRepository.save(otp);
        return otpCode;
    }

}
