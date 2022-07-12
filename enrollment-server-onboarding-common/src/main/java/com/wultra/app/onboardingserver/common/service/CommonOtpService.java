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
package com.wultra.app.onboardingserver.common.service;

import com.wultra.app.enrollmentserver.api.model.onboarding.response.OtpVerifyResponse;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.onboardingserver.common.api.OtpService;
import com.wultra.app.onboardingserver.common.configuration.CommonOnboardingConfig;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;

/**
 * Implementation of {@link OtpService} which is shared both for enrollment and onboarding.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public class CommonOtpService implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(CommonOtpService.class);

    protected final OnboardingOtpRepository onboardingOtpRepository;
    protected final OnboardingProcessRepository onboardingProcessRepository;
    protected final CommonOnboardingConfig commonOnboardingConfig;
    protected final CommonProcessLimitService processLimitService;

    /**
     * Service constructor.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param commonOnboardingConfig Common onboarding configuration.
     * @param commonProcessLimitService Common onboarding process limit service.
     */
    public CommonOtpService(
            final OnboardingOtpRepository onboardingOtpRepository,
            final OnboardingProcessRepository onboardingProcessRepository,
            final CommonOnboardingConfig commonOnboardingConfig,
            final CommonProcessLimitService commonProcessLimitService) {

        this.onboardingOtpRepository = onboardingOtpRepository;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.commonOnboardingConfig = commonOnboardingConfig;
        this.processLimitService = commonProcessLimitService;
    }

    @Override
    public OtpVerifyResponse verifyOtpCode(String processId, String otpCode, OtpType otpType) throws OnboardingProcessException {
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (processOptional.isEmpty()) {
            logger.warn("Onboarding process not found: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingProcessEntity process = processOptional.get();

        Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, otpType);
        if (otpOptional.isEmpty()) {
            logger.warn("Onboarding OTP not found for process ID: {}", processId);
            throw new OnboardingProcessException();
        }
        OnboardingOtpEntity otp = otpOptional.get();

        // Verify OTP code
        Date now = new Date();
        boolean expired = false;
        boolean verified = false;
        int failedAttempts = onboardingOtpRepository.getFailedAttemptsByProcess(processId, otpType);
        int maxFailedAttempts = commonOnboardingConfig.getOtpMaxFailedAttempts();
        if (OtpStatus.ACTIVE != otp.getStatus()) {
            logger.warn("Unexpected not active {}, process ID: {}", otp, processId);
        } else if (failedAttempts >= maxFailedAttempts) {
            logger.warn("Unexpected OTP code verification when already exhausted max failed attempts, process ID: {}", processId);
            process = processLimitService.failProcess(process, OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);
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
            handleFailedOtpVerification(process, otp, otpType);
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
     * Handle failed OTP verification.
     * @param process Onboarding process entity.
     * @param otp OTP entity.
     * @param otpType OTP type.
     */
    private void handleFailedOtpVerification(OnboardingProcessEntity process, OnboardingOtpEntity otp, OtpType otpType) {
        final Date now = new Date();
        int failedAttempts = onboardingOtpRepository.getFailedAttemptsByProcess(process.getId(), otpType);
        final int maxFailedAttempts = commonOnboardingConfig.getOtpMaxFailedAttempts();
        otp.setFailedAttempts(otp.getFailedAttempts() + 1);
        otp.setTimestampLastUpdated(now);
        otp = onboardingOtpRepository.save(otp);
        failedAttempts++;
        if (failedAttempts >= maxFailedAttempts) {
            otp.setStatus(OtpStatus.FAILED);
            otp.setErrorDetail(OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);
            onboardingOtpRepository.save(otp);

            // Onboarding process is failed, update it
            process = processLimitService.failProcess(process, OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);
        } else {
            // Increase error score for process based on OTP type
            if (otpType == OtpType.ACTIVATION) {
                process = processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_ACTIVATION_OTP_FAILED);
            } else if (otpType == OtpType.USER_VERIFICATION) {
                process = processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_USER_VERIFICATION_OTP_FAILED);
            }
            // Check onboarding process error limit
            process = processLimitService.checkOnboardingProcessErrorLimits(process);

            // Update OTP status in case process has just failed
            if (process.getStatus() == OnboardingStatus.FAILED) {
                otp.setStatus(OtpStatus.FAILED);
                otp.setErrorDetail(process.getErrorDetail());
                onboardingOtpRepository.save(otp);
            }
        }

        onboardingProcessRepository.save(process);
    }

}
