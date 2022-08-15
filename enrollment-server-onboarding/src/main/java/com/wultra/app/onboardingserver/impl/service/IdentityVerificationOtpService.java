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

import com.wultra.app.enrollmentserver.api.model.onboarding.response.OtpVerifyResponse;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.CommonProcessLimitService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.SendOtpCodeRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;

/**
 * Service implementing OTP delivery and verification during identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@Slf4j
public class IdentityVerificationOtpService {

    private final OnboardingProcessRepository onboardingProcessRepository;
    private final OnboardingOtpRepository onboardingOtpRepository;
    private final OtpServiceImpl otpService;

    private OnboardingProvider onboardingProvider;

    private final CommonProcessLimitService processLimitService;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final IdentityVerificationConfig identityVerificationConfig;

    /**
     * Service constructor.
     *
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param otpService OTP service.
     * @param processLimitService Process limit service.
     * @param identityVerificationRepository Identity verification repository.
     * @param identityVerificationConfig Identity verification config.
     */
    public IdentityVerificationOtpService(
            final OnboardingProcessRepository onboardingProcessRepository,
            final OnboardingOtpRepository onboardingOtpRepository,
            final OtpServiceImpl otpService,
            final CommonProcessLimitService processLimitService,
            final IdentityVerificationRepository identityVerificationRepository,
            final IdentityVerificationConfig identityVerificationConfig) {
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.onboardingOtpRepository = onboardingOtpRepository;
        this.otpService = otpService;
        this.processLimitService = processLimitService;
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationConfig = identityVerificationConfig;
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
     * Verify an OTP code as part of SCA.
     * <p>
     * If process is configured to do PRESENCE_CHECK, that step is verified here as well.
     * Because of SCA compliance, users MUST NOT be able to distinguish what went wrong.
     *
     * @param processId Onboarding process identification.
     * @param otpCode OTP code.
     * @return OTP verification response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Transactional
    public OtpVerifyResponse verifyOtpCode(String processId, String otpCode) throws OnboardingProcessException {
        final OnboardingProcessEntity process = onboardingProcessRepository.findById(processId).orElseThrow(() ->
            new OnboardingProcessException(String.format("Onboarding Process ID: %s not found.", processId)));

        final OtpVerifyResponse response = otpService.verifyOtpCode(process.getId(), otpCode, OtpType.USER_VERIFICATION);
        logger.debug("OTP code verified: {}, process ID: {}", response.isVerified(), processId);
        if (!response.isVerified() && identityVerificationConfig.isPresenceCheckEnabled()) {
            logger.info("SCA failed, wrong OTP code, process ID: {}", processId);
            final IdentityVerificationEntity idVerification = getIdentityVerificationEntity(process);
            return moveToPhasePresenceCheck(process, response, idVerification);
        }

        return verifyPresenceCheck(process, response);
    }

    /**
     * Get whether user is verified using OTP code.
     * @param processId Onboarding process ID.
     * @return Whether user is verified using OTP code.
     */
    public boolean isUserVerifiedUsingOtp(String processId) {
        final Optional<OnboardingOtpEntity> otpOptional = onboardingOtpRepository.findLastOtp(processId, OtpType.USER_VERIFICATION);
        if (otpOptional.isEmpty()) {
            return false;
        }
        final OnboardingOtpEntity otp = otpOptional.get();
        return otp.getStatus() == OtpStatus.VERIFIED;
    }

    private OtpVerifyResponse verifyPresenceCheck(final OnboardingProcessEntity process, final OtpVerifyResponse response) throws OnboardingProcessException {
        final String processId = process.getId();
        if (!identityVerificationConfig.isPresenceCheckEnabled()) {
            logger.debug("Presence check is not enabled, process ID: {}", processId);
            return response;
        }

        logger.debug("Evaluating result of presence check, process ID: {}", processId);

        final IdentityVerificationEntity idVerification = getIdentityVerificationEntity(process);
        final String errorDetail = idVerification.getErrorDetail();
        final String rejectReason = idVerification.getRejectReason();

        if (StringUtils.isNotBlank(errorDetail) || StringUtils.isNotBlank(rejectReason)) {
            logger.info("SCA failed, IdentityVerification ID: {} of Process ID: {} contains errorDetail: {}, rejectReason: {} from previous step",
                    idVerification.getId(), processId, errorDetail, rejectReason);
            return moveToPhasePresenceCheck(process, response, idVerification);
        } else {
            logger.debug("PRESENCE_CHECK without error or reject reason, process ID: {}", idVerification.getProcessId());
        }
        return response;
    }

    private IdentityVerificationEntity getIdentityVerificationEntity(final OnboardingProcessEntity process) throws OnboardingProcessException {
        return identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(process.getActivationId()).orElseThrow(() ->
                new OnboardingProcessException("No IdentityVerification found, process ID: " + process.getId()));
    }

    private OtpVerifyResponse moveToPhasePresenceCheck(
            final OnboardingProcessEntity process,
            final OtpVerifyResponse response,
            final IdentityVerificationEntity idVerification) throws OnboardingProcessException {

        idVerification.setPhase(PRESENCE_CHECK);
        idVerification.setStatus(IdentityVerificationStatus.NOT_INITIALIZED);
        idVerification.setTimestampLastUpdated(new Date());
        idVerification.setErrorDetail(null);
        idVerification.setRejectReason(null);
        identityVerificationRepository.save(idVerification);

        logger.info("Switched to PRESENCE_CHECK/NOT_INITIALIZED, process ID: {}", idVerification.getProcessId());

        markVerificationOtpAsFailed(process.getId());

        processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_USER_VERIFICATION_OTP_FAILED);
        final OnboardingStatus status = processLimitService.checkOnboardingProcessErrorLimits(process).getStatus();
        response.setOnboardingStatus(status);
        response.setVerified(false);
        return response;
    }

    private void markVerificationOtpAsFailed(String processId) throws OnboardingProcessException {
        final OnboardingOtpEntity otp = onboardingOtpRepository.findLastOtp(processId, OtpType.USER_VERIFICATION).orElseThrow(() ->
            new OnboardingProcessException("Onboarding OTP not found, process ID: " + processId));
        otp.setStatus(OtpStatus.FAILED);
        otp.setErrorDetail(OnboardingOtpEntity.ERROR_CANCELED);
        otp.setTimestampLastUpdated(new Date());
        onboardingOtpRepository.save(otp);
    }

    /**
     * Sends or resends an OTP code for a process during identity verification.
     * @param processId Process ID.
     * @param isResend Whether the OTP code is being resent.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    private void sendOtpCode(String processId, boolean isResend) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        final Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findById(processId);
        if (processOptional.isEmpty()) {
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
                    .processId(processId)
                    .userId(process.getUserId())
                    .otpCode(otpCode)
                    .resend(isResend)
                    .locale(LocaleContextHolder.getLocale())
                    .otpType(SendOtpCodeRequest.OtpType.USER_VERIFICATION)
                    .build();
            onboardingProvider.sendOtpCode(request);
        } catch (OnboardingProviderException e) {
            logger.warn("OTP code delivery failed, error: {}", e.getMessage(), e);
            throw new OnboardingOtpDeliveryException(e);
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
