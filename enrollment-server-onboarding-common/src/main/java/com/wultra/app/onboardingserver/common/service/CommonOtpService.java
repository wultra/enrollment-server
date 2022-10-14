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
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.api.OtpService;
import com.wultra.app.onboardingserver.common.configuration.CommonOnboardingConfig;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

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
    protected final OnboardingProcessLimitService processLimitService;
    protected final IdentityVerificationLimitService verificationLimitService;

    protected final AuditService auditService;

    /**
     * Service constructor.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingConfig Common onboarding configuration.
     * @param processLimitService Common onboarding process limit service.
     * @param verificationLimitService Common identity verification limit service.
     * @param auditService Audit service.
     */
    public CommonOtpService(
            final OnboardingOtpRepository onboardingOtpRepository,
            final OnboardingProcessRepository onboardingProcessRepository,
            final CommonOnboardingConfig onboardingConfig,
            final OnboardingProcessLimitService processLimitService,
            final IdentityVerificationLimitService verificationLimitService,
            final AuditService auditService) {

        this.onboardingOtpRepository = onboardingOtpRepository;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.commonOnboardingConfig = onboardingConfig;
        this.processLimitService = processLimitService;
        this.verificationLimitService = verificationLimitService;
        this.auditService = auditService;
    }

    @Override
    public OtpVerifyResponse verifyOtpCode(String processId, OwnerId ownerId, String otpCode, OtpType otpType) throws OnboardingProcessException {
        final OnboardingProcessEntity process = onboardingProcessRepository.findById(processId).orElseThrow(() ->
            new OnboardingProcessException("Onboarding process not found: " + processId));

        final OnboardingOtpEntity otp = onboardingOtpRepository.findNewestByProcessIdAndType(processId, otpType).orElseThrow(() ->
                new OnboardingProcessException("Onboarding OTP not found, process ID: " + processId));

        // Verify OTP code
        final Date now = ownerId.getTimestamp();
        boolean expired = false;
        boolean verified = false;
        int failedAttempts = onboardingOtpRepository.countFailedAttemptsByProcessIdAndType(processId, otpType);
        int maxFailedAttempts = commonOnboardingConfig.getOtpMaxFailedAttempts();
        if (OtpStatus.ACTIVE != otp.getStatus()) {
            logger.warn("Unexpected not active {}, process ID: {}", otp, processId);
        } else if (failedAttempts >= maxFailedAttempts) {
            logger.warn("Unexpected OTP code verification when already exhausted max failed attempts, process ID: {}", processId);
            failProcessOrIdentityVerification(process, otp, ownerId);
        } else if (otp.hasExpired()) {
            logger.info("Expired OTP code received, process ID: {}", processId);
            expired = true;
            otp.setStatus(OtpStatus.FAILED);
            otp.setErrorDetail(OnboardingOtpEntity.ERROR_EXPIRED);
            otp.setErrorOrigin(ErrorOrigin.OTP_VERIFICATION);
            otp.setTimestampLastUpdated(now);
            otp.setTimestampFailed(now);
            onboardingOtpRepository.save(otp);
            auditService.audit(otp, "OTP expired for user: {}", process.getUserId());
        } else if (otp.getOtpCode().equals(otpCode)) {
            verified = true;
            otp.setStatus(OtpStatus.VERIFIED);
            otp.setTimestampVerified(now);
            otp.setTimestampLastUpdated(now);
            onboardingOtpRepository.save(otp);
            logger.info("OTP verified, {}", ownerId);
            auditService.audit(otp, "OTP {} verified for user: {}", otpType, process.getUserId());
        } else {
            handleFailedOtpVerification(process, ownerId, otp, otpType);
        }

        final OtpVerifyResponse response = new OtpVerifyResponse();
        response.setProcessId(processId);
        response.setOnboardingStatus(process.getStatus());
        response.setExpired(expired);
        response.setVerified(verified);
        if (otp.getStatus() == OtpStatus.ACTIVE) {
            response.setRemainingAttempts(maxFailedAttempts - otp.getFailedAttempts());
        } else {
            response.setRemainingAttempts(0);
        }
        return response;
    }

    /**
     * Handle failed OTP verification.
     * @param process Onboarding process entity.
     * @param ownerId Owner identification.
     * @param otp OTP entity.
     * @param otpType OTP type.
     * @throws OnboardingProcessException In case process is not found.
     */
    private void handleFailedOtpVerification(OnboardingProcessEntity process, OwnerId ownerId, OnboardingOtpEntity otp, OtpType otpType) throws OnboardingProcessException {
        int failedAttempts = onboardingOtpRepository.countFailedAttemptsByProcessIdAndType(process.getId(), otpType);
        final int maxFailedAttempts = commonOnboardingConfig.getOtpMaxFailedAttempts();
        otp.setFailedAttempts(otp.getFailedAttempts() + 1);
        otp.setTimestampLastUpdated(ownerId.getTimestamp());
        otp = onboardingOtpRepository.save(otp);
        failedAttempts++;
        if (failedAttempts >= maxFailedAttempts) {
            otp.setStatus(OtpStatus.FAILED);
            otp.setErrorDetail(OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS);
            otp.setErrorOrigin(ErrorOrigin.OTP_VERIFICATION);
            otp.setTimestampFailed(ownerId.getTimestamp());
            onboardingOtpRepository.save(otp);
            auditService.audit(otp, "OTP max attempts reached for user: {}", process.getUserId());

            failProcessOrIdentityVerification(process, otp, ownerId);
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
                otp.setErrorOrigin(ErrorOrigin.OTP_VERIFICATION);
                otp.setTimestampLastUpdated(ownerId.getTimestamp());
                otp.setTimestampFailed(ownerId.getTimestamp());
                onboardingOtpRepository.save(otp);
                auditService.audit(otp, "OTP failed because of failed process for user: {}", process.getUserId());
            }
        }

        onboardingProcessRepository.save(process);
    }

    /**
     * Handle maximum failed attempts for OTP verification based on OTP type.
     * @param process Onboarding process.
     * @param otp Onboarding OTP.
     * @param ownerId Owner identification.
     * @return Updated onboarding process entity.
     * @throws OnboardingProcessException In case process is not found.
     */
    private OnboardingProcessEntity failProcessOrIdentityVerification(OnboardingProcessEntity process, OnboardingOtpEntity otp, OwnerId ownerId) throws OnboardingProcessException {
        if (otp.getType() == OtpType.USER_VERIFICATION) {
            // Reset current identity verification, if possible.
            try {
                verificationLimitService.resetIdentityVerification(ownerId);
            } catch (RemoteCommunicationException | IdentityVerificationException | OnboardingProcessLimitException | OnboardingProcessException ex) {
                logger.error("Identity verification reset failed, error: {}", ex.getMessage(), ex);
                // Obtain most current process entity, the process may have failed due to reached limit of identity verification resets
                final String processId = process.getId();
                process = onboardingProcessRepository.findById(processId).orElseThrow(() ->
                    new OnboardingProcessException("Onboarding process not found, process ID: " + processId));
            }
        } else {
            // Fail onboarding process completely
            process = processLimitService.failProcess(process, OnboardingOtpEntity.ERROR_MAX_FAILED_ATTEMPTS, ErrorOrigin.PROCESS_LIMIT_CHECK);
        }
        return process;
    }
}
