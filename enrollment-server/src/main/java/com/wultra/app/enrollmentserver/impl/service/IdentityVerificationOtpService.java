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
import com.wultra.app.enrollmentserver.database.OnboardingOtpRepository;
import com.wultra.app.enrollmentserver.database.OnboardingProcessRepository;
import com.wultra.app.enrollmentserver.database.entity.OnboardingOtpEntity;
import com.wultra.app.enrollmentserver.database.entity.OnboardingProcessEntity;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingProviderException;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.provider.OnboardingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service implementing OTP delivery and verification during identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationOtpService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationOtpService.class);

    private final OnboardingProcessRepository onboardingProcessRepository;
    private final OnboardingOtpRepository onboardingOtpRepository;
    private final OtpService otpService;

    private OnboardingProvider onboardingProvider;

    /**
     * Service constructor.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param otpService OTP service.
     */
    public IdentityVerificationOtpService(OnboardingProcessRepository onboardingProcessRepository, OnboardingOtpRepository onboardingOtpRepository, OtpService otpService) {
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.onboardingOtpRepository = onboardingOtpRepository;
        this.otpService = otpService;
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
     * Send or resend an OTP code for a process during identity verification.
     * @param processId Process ID.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    public void sendOtpCode(String processId) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        if (onboardingProvider == null) {
            logger.error("Onboarding provider is not available. Implement an onboarding provider and make it accessible using autowiring.");
            throw new OnboardingProcessException();
        }
        final Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found: {}", processId);
            throw new OnboardingProcessException();
        }
        final OnboardingProcessEntity process = processOptional.get();
        final boolean isResend = otpService.isNextOtpResend(process, OtpType.USER_VERIFICATION);
        // Create an OTP code
        final String otpCode;
        if (isResend) {
            otpCode = otpService.createOtpCodeForResend(process, OtpType.USER_VERIFICATION);
        } else {
            otpCode = otpService.createOtpCode(process, OtpType.USER_VERIFICATION);
        }
        // Send the OTP code
        try {
            onboardingProvider.sendOtpCode(process.getUserId(), otpCode, isResend);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code delivery failed, error: {}", e.getMessage(), e);
            throw new OnboardingOtpDeliveryException();
        }
    }

    /**
     * Verify an OTP code.
     * @param processId Onboarding process identification.
     * @param otpCode OTP code.
     * @return OTP verification response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OtpVerifyResponse verifyOtpCode(String processId, String otpCode) throws OnboardingProcessException {
        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (!processOptional.isPresent()) {
            logger.warn("Onboarding process not found: {}", processId);
            throw new OnboardingProcessException();
        }
        final OnboardingProcessEntity process = processOptional.get();
        return otpService.verifyOtpCode(process.getId(), otpCode, OtpType.USER_VERIFICATION);
    }

    /**
     * Get whether user is verified using OTP code.
     * @param processId Onboarding process ID.
     * @return Whether user is verified using OTP code.
     */
    public boolean isUserVerifiedUsingOtp(String processId) {
        Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, OtpType.USER_VERIFICATION);
        if (!otpOptional.isPresent()) {
            return false;
        }
        OnboardingOtpEntity otp = otpOptional.get();
        return otp.getStatus() == OtpStatus.VERIFIED;
    }

}