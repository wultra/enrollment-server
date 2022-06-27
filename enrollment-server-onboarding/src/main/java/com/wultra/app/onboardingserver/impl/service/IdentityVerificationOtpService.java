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

import com.wultra.app.enrollmentserver.api.model.response.OtpVerifyResponse;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.SendOtpCodeRequest;
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
    private final OtpServiceImpl otpService;

    private OnboardingProvider onboardingProvider;

    /**
     * Service constructor.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param otpService OTP service.
     */
    public IdentityVerificationOtpService(OnboardingProcessRepository onboardingProcessRepository, OnboardingOtpRepository onboardingOtpRepository, OtpServiceImpl otpService) {
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
     * Resends an OTP code for a process during identity verification.
     * @param ownerId Owner identification.
     * @param identityVerification Identity verification entity.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    public void resendOtp(OwnerId ownerId, IdentityVerificationEntity identityVerification) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        checkPreconditions(ownerId, identityVerification);
        sendOtpCode(identityVerification.getProcessId(), true);
    }

    /**
     * Sends an OTP code for a process during identity verification.
     * @param ownerId Owner identification.
     * @param identityVerification Identity verification entity.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    public void sendOtp(OwnerId ownerId, IdentityVerificationEntity identityVerification) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        checkPreconditions(ownerId, identityVerification);
        sendOtpCode(identityVerification.getProcessId(), false);
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

    /**
     * Sends or resends an OTP code for a process during identity verification.
     * @param processId Process ID.
     * @param isResend Whether the OTP code is being resent.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    private void sendOtpCode(String processId, boolean isResend) throws OnboardingProcessException, OnboardingOtpDeliveryException {
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
        // Create an OTP code
        final String otpCode;
        if (isResend) {
            otpCode = otpService.createOtpCodeForResend(process, OtpType.USER_VERIFICATION);
        } else {
            otpCode = otpService.createOtpCode(process, OtpType.USER_VERIFICATION);
        }
        // Send the OTP code
        try {
            final SendOtpCodeRequest request = SendOtpCodeRequest.builder()
                    .userId(process.getUserId())
                    .otpCode(otpCode)
                    .resend(isResend)
                    .build();
            onboardingProvider.sendOtpCode(request);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code delivery failed, error: {}", e.getMessage(), e);
            throw new OnboardingOtpDeliveryException();
        }
    }

    private void checkPreconditions(OwnerId ownerId, IdentityVerificationEntity identityVerification) throws OnboardingProcessException {
        if (!IdentityVerificationPhase.OTP_VERIFICATION.equals(identityVerification.getPhase())) {
            logger.warn("Invalid identity verification phase {}, but expected {}, {}",
                    identityVerification.getPhase(), IdentityVerificationPhase.OTP_VERIFICATION, ownerId);
            throw new OnboardingProcessException("Unexpected state of identity verification");
        }
        if (!IdentityVerificationStatus.OTP_VERIFICATION_PENDING.equals(identityVerification.getStatus())) {
            logger.warn("Invalid identity verification status {}, but expected {}, {}",
                    identityVerification.getStatus(), IdentityVerificationStatus.OTP_VERIFICATION_PENDING, ownerId);
            throw new OnboardingProcessException("Unexpected state of identity verification");
        }
    }

}
