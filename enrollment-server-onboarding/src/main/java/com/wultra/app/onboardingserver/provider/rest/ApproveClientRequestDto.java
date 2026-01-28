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
package com.wultra.app.onboardingserver.provider.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

/**
 * Request object for client approval.
 *
 * @param processId Process ID
 * @param processType Process type.
 * @param userId User ID.
 * @param identityVerificationId Identity verification ID.
 * @param provider Name of the configured external biometry provider. For example, {@code iProov}.
 * @param status Status of the identity verification process.
 * @param score Outcome confidence of the verification check on scale 0-10.
 * @param presenceCheckResult Result of the presence check.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Jacksonized
record ApproveClientRequestDto(
        @NonNull String processId,
        @NonNull String processType,
        @NonNull String userId,
        @NonNull String identityVerificationId,
        @NonNull String provider,
        @NonNull Status status,
        @NonNull @Min(0) @Max(10) Integer score,
        @NonNull PresenceCheckResult presenceCheckResult
) {

    /**
     * Result of the presence check.
     *
     * @param frame Photo/image from the biometry session, encoded in base64.
     */
    public record PresenceCheckResult(
            String frame
    ) {}

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
