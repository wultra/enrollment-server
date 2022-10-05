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
     * Audit the given process and identity verification.
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

    private static AuditDetail createAuditDetail(final IdentityVerificationEntity identityVerification) {
        return AuditDetail.builder()
                .type("identityVerification")
                .param("identityVerification", identityVerification)
                .param("processId", identityVerification.getProcessId())
                .param("activationId", identityVerification.getActivationId())
                .param("userId", identityVerification.getUserId())
                .build();
    }

    private static AuditDetail createAuditDetail(final OnboardingProcessEntity process, final String identityVerificationId) {
        final AuditDetail.Builder builder = AuditDetail.builder()
                .type("process")
                .param("processId", process.getId());

        if (identityVerificationId != null) {
            builder.param("identityVerificationId", identityVerificationId);
        }

        final String activationId = process.getActivationId();
        if (activationId != null) {
            builder.param("activationId", activationId);
        }

        final String userId = process.getUserId();
        if (userId != null) {
            builder.param("userId", userId);
        }

        return builder.build();
    }
}
