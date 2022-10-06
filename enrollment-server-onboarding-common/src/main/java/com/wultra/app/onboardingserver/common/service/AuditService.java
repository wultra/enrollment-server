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

import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.core.audit.base.Audit;
import com.wultra.core.audit.base.model.AuditDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementing audit functionality.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
public class AuditService {

    private static final String IDENTITY_VERIFICATION_ID = "identityVerificationId";
    private static final String PROCESS_ID = "processId";
    private static final String ACTIVATION_ID = "activationId";
    private static final String USER_ID = "userId";
    private static final String OTP_ID = "otpId";

    private final Audit audit;

    @Autowired
    AuditService(final Audit audit) {
        this.audit = audit;
    }

    /**
     * Audit the given process.
     *
     * @param process process to audit
     * @param message message, arguments may be put to via template {@code {}}
     * @param args message arguments
     */
    public void audit(final OnboardingProcessEntity process, final String message, final Object... args) {
        final AuditDetail auditDetail = createAuditDetail(process, null);
        audit.info(message, auditDetail, args);
    }

    /**
     * Audit the given process and the identity verification.
     *
     * @param process process to audit
     * @param identityVerification identity verification to audit
     * @param message message, arguments may be put to via template {@code {}}
     * @param args message arguments
     */
    public void audit(final OnboardingProcessEntity process, final IdentityVerificationEntity identityVerification, final String message, final Object... args) {
        final AuditDetail auditDetail = createAuditDetail(process, identityVerification.getId());
        audit.info(message, auditDetail, args);
    }

    /**
     * Audit the given otp and the identity verification.
     *
     * @param otp otp to audit
     * @param identityVerification identity verification to audit
     * @param message message, arguments may be put to via template {@code {}}
     * @param args message arguments
     */
    public void audit(final OnboardingOtpEntity otp, final IdentityVerificationEntity identityVerification, final String message, final Object... args) {
        final AuditDetail auditDetail = createAuditDetail(otp, identityVerification);
        audit.info(message, auditDetail, args);
    }

    /**
     * Audit the given otp.
     *
     * @param otp otp to audit
     * @param message message, arguments may be put to via template {@code {}}
     * @param args message arguments
     */
    public void audit(final OnboardingOtpEntity otp, final String message, final Object... args) {
        final AuditDetail auditDetail = createAuditDetail(otp);
        audit.info(message, auditDetail, args);
    }

    /**
     * Audit the given identity verification.
     *
     * @param identityVerification identity verification to audit
     * @param message message, arguments may be put to via template {@code {}}
     * @param args message arguments
     */
    public void audit(final IdentityVerificationEntity identityVerification, final String message, final Object... args) {
        final AuditDetail auditDetail = createAuditDetail(identityVerification);
        audit.info(message, auditDetail, args);
    }

    private static AuditDetail createAuditDetail(final OnboardingOtpEntity otp, final IdentityVerificationEntity identityVerification) {
        return AuditDetail.builder()
                .type("otp")
                .param(IDENTITY_VERIFICATION_ID, identityVerification.getId())
                .param(PROCESS_ID, identityVerification.getProcessId())
                .param(ACTIVATION_ID, identityVerification.getActivationId())
                .param(USER_ID, identityVerification.getUserId())
                .param(OTP_ID, otp.getId())
                .build();
    }

    private static AuditDetail createAuditDetail(final OnboardingOtpEntity otp) {
        final OnboardingProcessEntity process = otp.getProcess();
        return AuditDetail.builder()
                .type("otp")
                .param(ACTIVATION_ID, process.getActivationId())
                .param(PROCESS_ID, process.getId())
                .param(USER_ID, process.getUserId())
                .param(OTP_ID, otp.getId())
                .build();
    }

    private static AuditDetail createAuditDetail(final IdentityVerificationEntity identityVerification) {
        return AuditDetail.builder()
                .type("identityVerification")
                .param(IDENTITY_VERIFICATION_ID, identityVerification.getId())
                .param(PROCESS_ID, identityVerification.getProcessId())
                .param(ACTIVATION_ID, identityVerification.getActivationId())
                .param(USER_ID, identityVerification.getUserId())
                .build();
    }

    private static AuditDetail createAuditDetail(final OnboardingProcessEntity process, final String identityVerificationId) {
        final AuditDetail.Builder builder = AuditDetail.builder()
                .type("process")
                .param(PROCESS_ID, process.getId());

        if (identityVerificationId != null) {
            builder.param(IDENTITY_VERIFICATION_ID, identityVerificationId);
        }

        final String activationId = process.getActivationId();
        if (activationId != null) {
            builder.param(ACTIVATION_ID, activationId);
        }

        final String userId = process.getUserId();
        if (userId != null) {
            builder.param(USER_ID, userId);
        }

        return builder.build();
    }
}
