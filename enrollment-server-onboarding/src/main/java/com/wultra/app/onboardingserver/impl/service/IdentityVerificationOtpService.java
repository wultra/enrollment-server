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
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.SendOtpCodeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.OTP_VERIFICATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.NOT_INITIALIZED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.VERIFICATION_PENDING;

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

    private final OnboardingProcessLimitService processLimitService;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final IdentityVerificationConfig identityVerificationConfig;

    private final IdentityVerificationService identityVerificationService;

    private final AuditService auditService;

    /**
     * Service constructor.
     *
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param otpService OTP service.
     * @param processLimitService Process limit service.
     * @param identityVerificationRepository Identity verification repository.
     * @param identityVerificationConfig Identity verification config.
     * @param identityVerificationService Identity verification service.
     * @param auditService Audit service.
     */
    public IdentityVerificationOtpService(
            final OnboardingProcessRepository onboardingProcessRepository,
            final OnboardingOtpRepository onboardingOtpRepository,
            final OtpServiceImpl otpService,
            final OnboardingProcessLimitService processLimitService,
            final IdentityVerificationRepository identityVerificationRepository,
            final IdentityVerificationConfig identityVerificationConfig,
            final IdentityVerificationService identityVerificationService,
            final AuditService auditService) {

        this.onboardingProcessRepository = onboardingProcessRepository;
        this.onboardingOtpRepository = onboardingOtpRepository;
        this.otpService = otpService;
        this.processLimitService = processLimitService;
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationConfig = identityVerificationConfig;
        this.identityVerificationService = identityVerificationService;
        this.auditService = auditService;
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
     * @param identityVerification Identity verification entity.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    @Transactional
    public void resendOtp(IdentityVerificationEntity identityVerification) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        sendOtpCode(identityVerification.getProcessId(), true);
    }

    /**
     * Sends an OTP code for a process during identity verification.
     * @param identityVerification Identity verification entity.
     * @param ownerId Owner identification.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    @Transactional
    public void sendOtp(IdentityVerificationEntity identityVerification, OwnerId ownerId) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        identityVerificationService.moveToPhaseAndStatus(identityVerification, OTP_VERIFICATION, VERIFICATION_PENDING, ownerId);
        sendOtpCode(identityVerification.getProcessId(), false);
    }

    /**
     * Verify an OTP code as part of SCA.
     * <p>
     * If process is configured to do PRESENCE_CHECK, that step is verified here as well.
     * Because of SCA compliance, users MUST NOT be able to distinguish what went wrong.
     *
     * @param processId Onboarding process identification.
     * @param ownerId Owner identification.
     * @param otpCode OTP code.
     * @return OTP verification response.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Transactional
    public OtpVerifyResponse verifyOtpCode(String processId, OwnerId ownerId, String otpCode) throws OnboardingProcessException {
        final OnboardingProcessEntity process = onboardingProcessRepository.findById(processId).orElseThrow(() ->
            new OnboardingProcessException(String.format("Onboarding Process ID: %s not found.", processId)));

        final OtpVerifyResponse response = otpService.verifyOtpCode(process.getId(), ownerId, otpCode, OtpType.USER_VERIFICATION);
        logger.debug("OTP code verified: {}, process ID: {}", response.isVerified(), processId);
        if (!identityVerificationConfig.isPresenceCheckEnabled()) {
            return response;
        }
        if (!response.isVerified()) {
            logger.info("SCA failed, wrong OTP code, process ID: {}", processId);
            return response;
        }
        return verifyPresenceCheck(process, response, ownerId);
    }

    /**
     * Get whether user is verified using OTP code.
     * @param processId Onboarding process ID.
     * @return Whether user is verified using OTP code.
     */
    public boolean isUserVerifiedUsingOtp(String processId) {
        return onboardingOtpRepository.findNewestByProcessIdAndType(processId, OtpType.USER_VERIFICATION)
                .map(OnboardingOtpEntity::getStatus)
                .filter(it -> it == OtpStatus.VERIFIED)
                .isPresent();
    }

    private OtpVerifyResponse verifyPresenceCheck(final OnboardingProcessEntity process, final OtpVerifyResponse response, final OwnerId ownerId) throws OnboardingProcessException {
        final String processId = process.getId();
        if (!identityVerificationConfig.isPresenceCheckEnabled()) {
            logger.debug("Presence check is not enabled, process ID: {}", processId);
            return response;
        }

        logger.debug("Evaluating result of presence check, process ID: {}", processId);

        final IdentityVerificationEntity idVerification = getIdentityVerificationEntity(process);
        final String errorDetail = idVerification.getErrorDetail();
        final ErrorOrigin errorOrigin = idVerification.getErrorOrigin();
        final String rejectReason = idVerification.getRejectReason();
        final RejectOrigin rejectOrigin = idVerification.getRejectOrigin();

        // TODO (racansky, 2022-10-18, #453) this is quite fragile condition, could be improved e.g. with data historization
        if (errorOrigin == ErrorOrigin.PRESENCE_CHECK || rejectOrigin == RejectOrigin.PRESENCE_CHECK) {
            logger.info("SCA failed, identity verification ID: {}, {} contains errorDetail: {}, rejectReason: {} from previous step",
                    idVerification.getId(), ownerId, errorDetail, rejectReason);
            return moveToPhasePresenceCheck(process, response, idVerification, ownerId);
        } else {
            logger.debug("PRESENCE_CHECK without error or reject origin, {}", ownerId);
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
            final IdentityVerificationEntity idVerification,
            final OwnerId ownerId) throws OnboardingProcessException {

        idVerification.setErrorDetail(null);
        idVerification.setErrorOrigin(null);
        idVerification.setRejectReason(null);
        idVerification.setRejectOrigin(null);
        identityVerificationService.moveToPhaseAndStatus(idVerification, PRESENCE_CHECK, NOT_INITIALIZED, ownerId);

        markVerificationOtpAsFailed(process.getId(), idVerification);

        processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_USER_VERIFICATION_OTP_FAILED);
        final OnboardingStatus status = processLimitService.checkOnboardingProcessErrorLimits(process).getStatus();
        response.setOnboardingStatus(status);
        response.setVerified(false);
        response.setRemainingAttempts(0);
        return response;
    }

    private void markVerificationOtpAsFailed(String processId, IdentityVerificationEntity idVerification) throws OnboardingProcessException {
        final OnboardingOtpEntity otp = onboardingOtpRepository.findNewestByProcessIdAndType(processId, OtpType.USER_VERIFICATION).orElseThrow(() ->
            new OnboardingProcessException("Onboarding OTP not found, process ID: " + processId));
        otp.setStatus(OtpStatus.FAILED);
        otp.setErrorDetail(OnboardingOtpEntity.ERROR_CANCELED);
        otp.setErrorOrigin(ErrorOrigin.OTP_VERIFICATION);
        final Date now = new Date();
        otp.setTimestampLastUpdated(now);
        otp.setTimestampFailed(now);
        onboardingOtpRepository.save(otp);
        auditService.audit(otp, idVerification, "OTP failed for user: {}", idVerification.getUserId());
    }

    /**
     * Sends or resends an OTP code for a process during identity verification.
     * @param processId Process ID.
     * @param isResend Whether the OTP code is being resent.
     * @throws OnboardingProcessException Thrown when OTP code could not be generated.
     * @throws OnboardingOtpDeliveryException Thrown when OTP code could not be sent.
     */
    private void sendOtpCode(String processId, boolean isResend) throws OnboardingProcessException, OnboardingOtpDeliveryException {
        final OnboardingProcessEntity process = onboardingProcessRepository.findById(processId).orElseThrow(() ->
                new OnboardingProcessException("Onboarding process not found: " + processId));

        final String otpCode = createOtpCode(isResend, process);
        final String userId = process.getUserId();
        final SendOtpCodeRequest request = SendOtpCodeRequest.builder()
                .processId(processId)
                .userId(userId)
                .otpCode(otpCode)
                .resend(isResend)
                .locale(LocaleContextHolder.getLocale())
                .otpType(SendOtpCodeRequest.OtpType.USER_VERIFICATION)
                .build();
        try {
            onboardingProvider.sendOtpCode(request);
        } catch (OnboardingProviderException e) {
            throw new OnboardingOtpDeliveryException("OTP code delivery failed, error: " + e.getMessage(), e);
        }

        final String resentPrefix = isResend ? "Resent" : "Sent";
        auditService.auditOnboardingProvider(process, "{} user verification OTP for user: {}", resentPrefix, userId);
    }

    private String createOtpCode(final boolean isResend, final OnboardingProcessEntity process) throws OnboardingOtpDeliveryException, OnboardingProcessException {
        if (isResend) {
            return otpService.createOtpCodeForResend(process, OtpType.USER_VERIFICATION);
        } else {
            return otpService.createOtpCode(process, OtpType.USER_VERIFICATION);
        }
    }
}
