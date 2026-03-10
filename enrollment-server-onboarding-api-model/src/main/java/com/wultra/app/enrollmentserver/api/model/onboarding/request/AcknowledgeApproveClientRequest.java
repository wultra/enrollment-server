/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.api.model.onboarding.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request object for client approval acknowledgement.
 *
 * @param processId Process ID
 * @param userId User ID.
 * @param identityVerificationId Identity verification ID.
 * @param approvalResult The approval result.
 * @param resultReason The reason is used when the result is NOK to disclose the reason of rejection.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public record AcknowledgeApproveClientRequest(
        @NotBlank String processId,
        @NotBlank String userId,
        @NotBlank String identityVerificationId,
        @NotNull ApprovalResult approvalResult,
        String resultReason
) {
    public enum ApprovalResult {

        /**
         * Approval was successful.
         */
        OK,

        /**
         * Approval was not successful.
         */
        NOK,

        /**
         * Wait, still not decided.
         */
        WAIT
    }
}
