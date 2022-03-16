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

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
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
        final Date otpLastCreatedDate = onboardingOtpRepository.getNewestOtpCreatedTimestamp(processId, otpType);
        final Duration resendPeriod = onboardingConfig.getOtpResendPeriod();
        if (isFromNowCloserThan(otpLastCreatedDate, resendPeriod)) {
            logger.warn("Resend OTP functionality is not available yet (due to resend period) for process ID: {}", processId);
            throw new OnboardingOtpDeliveryException();
        }
        final Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, otpType);
        if (!otpOptional.isPresent()) {
            logger.warn("Onboarding OTP not found for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        final OnboardingOtpEntity existingOtp = otpOptional.get();
        if (!OtpStatus.FAILED.equals(existingOtp.getStatus())) {
            existingOtp.setStatus(OtpStatus.FAILED);
            existingOtp.setTimestampLastUpdated(new Date());
            onboardingOtpRepository.save(existingOtp);
            logger.info("Marked previous {} as {} to allow new send of the OTP code", existingOtp, OtpStatus.FAILED);
        }
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
        Date now = new Date();
        boolean expired = false;
        boolean verified = false;
        int failedAttempts = onboardingOtpRepository.getFailedAttemptsByProcess(processId, otpType);
        int maxFailedAttempts = onboardingConfig.getOtpMaxFailedAttempts();
        if (OtpStatus.ACTIVE != otp.getStatus()) {
            logger.warn("Unexpected not active {}, process ID: {}", otp, processId);
        } else if (failedAttempts >= maxFailedAttempts) {
            logger.warn("Unexpected OTP code verification when already exhausted max failed attempts, process ID: {}", processId);
            process = failProcess(process, OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);
        } else if (otp.hasExpired()) {
            logger.info("Expired OTP code received, process ID: {}", processId);
            expired = true;
            otp.setStatus(OtpStatus.FAILED);
            otp.setErrorDetail(OnboardingOtpEntity.ERROR_EXPIRED);
            otp.setTimestampLastUpdated(now);
            onboardingOtpRepository.save(otp);
        } else if (otp.getOtpCode().equals(otpCode)) {
            verified = true;
            otp.setStatus(OtpStatus.VERIFIED);
            otp.setTimestampVerified(now);
            otp.setTimestampLastUpdated(now);
            onboardingOtpRepository.save(otp);
        } else {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            failedAttempts++;
            if (failedAttempts >= maxFailedAttempts) {
                otp.setStatus(OtpStatus.FAILED);
                otp.setErrorDetail(OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);

                // Onboarding process is failed, update it
                process = failProcess(process, OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);
            }
            otp.setTimestampLastUpdated(now);
            onboardingOtpRepository.save(otp);
        }

        OtpVerifyResponse response = new OtpVerifyResponse();
        response.setProcessId(processId);
        response.setOnboardingStatus(process.getStatus());
        response.setExpired(expired);
        response.setVerified(verified);
        response.setRemainingAttempts(maxFailedAttempts - failedAttempts);
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
                otp.setErrorDetail(OnboardingOtpEntity.ERROR_CANCELED);
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
        int otpLength = onboardingConfig.getOtpLength();
        String otpCode = otpGeneratorService.generateOtpCode(otpLength);

        // prepare timestamp created and expiration
        Calendar calendar = Calendar.getInstance();
        Date timestampCreated = calendar.getTime();
        calendar.add(Calendar.SECOND, (int) onboardingConfig.getOtpExpirationTime().getSeconds());
        Date timestampExpiration = calendar.getTime();

        OnboardingOtpEntity otp = new OnboardingOtpEntity();
        otp.setProcess(process);
        otp.setOtpCode(otpCode);
        otp.setType(otpType);
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setTimestampCreated(timestampCreated);
        otp.setTimestampExpiration(timestampExpiration);
        otp.setFailedAttempts(0);
        onboardingOtpRepository.save(otp);
        return otpCode;
    }

    private OnboardingProcessEntity failProcess(OnboardingProcessEntity entity, String errorDetail) {
        if (OnboardingStatus.FAILED == entity.getStatus()) {
            logger.debug("Not failing already failed onboarding entity");
            return entity;
        }
        entity.setStatus(OnboardingStatus.FAILED);
        entity.setTimestampLastUpdated(new Date());
        entity.setErrorDetail(errorDetail);
        return onboardingProcessRepository.save(entity);
    }

    /**
     * Checks whether a date is less than a specified duration closer to the current time
     * @param date Date value
     * @param duration Minimum duration before now
     * @return true when the date is before or after the current time shorter duration than the specified one
     */
    private boolean isFromNowCloserThan(Date date, Duration duration) {
        return Math.abs(System.currentTimeMillis() - date.getTime()) < (duration.getSeconds() * 1_000);
    }

}
