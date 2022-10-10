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

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OtpStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOtpService;
import com.wultra.app.onboardingserver.common.service.IdentityVerificationLimitService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingOtpDeliveryException;
import com.wultra.app.onboardingserver.impl.service.internal.OtpGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

/**
 * Service implementing OTP delivery and verification specific for the onboarding process.
 * Shared behavior is inherited from {@link CommonOtpService}.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OtpServiceImpl extends CommonOtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final OtpGeneratorService otpGeneratorService;

    private final OnboardingConfig onboardingConfig;

    /**
     * Service constructor.
     * @param otpGeneratorService OTP generator service.
     * @param onboardingOtpRepository Onboarding OTP repository.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param onboardingConfig Onboarding configuration.
     * @param processLimitService Onboarding process limit service.
     * @param auditService auditService.
     */
    @Autowired
    public OtpServiceImpl(
            final OtpGeneratorService otpGeneratorService,
            final OnboardingOtpRepository onboardingOtpRepository,
            final OnboardingProcessRepository onboardingProcessRepository,
            final OnboardingConfig onboardingConfig,
            final OnboardingProcessLimitService processLimitService,
            final IdentityVerificationLimitService verificationLimitService,
            final AuditService auditService) {

        super(onboardingOtpRepository, onboardingProcessRepository, onboardingConfig, processLimitService, verificationLimitService, auditService);
        this.otpGeneratorService = otpGeneratorService;
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
            logger.warn("Resend OTP functionality is not available yet (due to resend period), process ID: {}", processId);
            throw new OnboardingOtpDeliveryException();
        }
        final OnboardingOtpEntity existingOtp = onboardingOtpRepository.findLastOtp(processId, otpType).orElseThrow(() ->
                new OnboardingProcessException("Onboarding OTP not found, process ID: " + processId));

        if (existingOtp.getStatus() != OtpStatus.FAILED) {
            existingOtp.setStatus(OtpStatus.FAILED);
            existingOtp.setTimestampLastUpdated(new Date());
            onboardingOtpRepository.save(existingOtp);
            logger.info("Marked previous {} as {} to allow new send of the OTP code", existingOtp, OtpStatus.FAILED);
            auditService.audit(existingOtp, "Resending OTP for user: {}", process.getUserId());
        }

        return generateOtpCode(process, otpType);
    }

    /**
     * Cancel an OTP for an onboarding process.
     * @param process Onboarding process.
     */
    public void cancelOtp(OnboardingProcessEntity process, OtpType otpType) {
        final String processId = process.getId();
        // Fail current OTP, if it is present
        onboardingOtpRepository.findLastOtp(processId, otpType).ifPresent(otp -> {
            final Date now = new Date();
            if (otp.getStatus() != OtpStatus.FAILED) {
                otp.setStatus(OtpStatus.FAILED);
                otp.setTimestampLastUpdated(now);
                otp.setTimestampFailed(now);
                otp.setErrorDetail(OnboardingOtpEntity.ERROR_CANCELED);
                otp.setErrorOrigin(ErrorOrigin.OTP_VERIFICATION);
                onboardingOtpRepository.save(otp);
                auditService.audit(otp, "OTP canceled for user: {}", process.getUserId());
            }
        });
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
        final OnboardingOtpEntity savedOtp = onboardingOtpRepository.save(otp);
        auditService.auditDebug(savedOtp, "Generated OTP for user: {}", process.getUserId());
        return otpCode;
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
