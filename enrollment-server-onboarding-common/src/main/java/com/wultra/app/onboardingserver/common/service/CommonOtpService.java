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
    private final CommonOnboardingConfig commonOnboardingConfig;

    /**
     * Service constructor.
     *
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param commonOnboardingConfig Common onboarding configuration.
     */
    public CommonOtpService(
            final OnboardingOtpRepository onboardingOtpRepository,
            final OnboardingProcessRepository onboardingProcessRepository,
            final CommonOnboardingConfig commonOnboardingConfig) {

        this.onboardingOtpRepository = onboardingOtpRepository;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.commonOnboardingConfig = commonOnboardingConfig;
    }

    @Override
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
        int maxFailedAttempts = commonOnboardingConfig.getOtpMaxFailedAttempts();
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
     * Mark the given onboarding process entity as failed with the given error detail.
     * Does nothing for already failed onboarding process entity.
     *
     * @param entity onboarding process entity to update
     * @param errorDetail error detail
     * @return updated onboarding process entity
     */
    protected OnboardingProcessEntity failProcess(OnboardingProcessEntity entity, String errorDetail) {
        if (OnboardingStatus.FAILED == entity.getStatus()) {
            logger.debug("Not failing already failed onboarding entity ID: {}", entity.getId());
            return entity;
        }
        entity.setStatus(OnboardingStatus.FAILED);
        entity.setTimestampLastUpdated(new Date());
        entity.setErrorDetail(errorDetail);
        return onboardingProcessRepository.save(entity);
    }
}
