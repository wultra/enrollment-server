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
 *
 */
package com.wultra.app.onboardingserver.provider.model.request;

import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.core.annotations.PublicApi;
import lombok.Builder;
import lombok.NonNull;

/**
 * Request object for {@link OnboardingProvider#approveClient(ApproveClientRequest)}.
 *
 * @param processId Process ID
 * @param processType Process type.
 * @param userId User ID.
 * @param identityVerificationId Identity verification ID.
 * @param provider Name of the configured external biometry provider. For example, {@code iProov}.
 * @param status Status of the identity verification process.
 * @param score Outcome confidence of the verification check on scale 0-10.
 * @param image Photo/image from the biometry session, encoded in base64.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@PublicApi
public record ApproveClientRequest(
        @NonNull String processId,
        @NonNull String processType,
        @NonNull String userId,
        @NonNull String identityVerificationId,
        @NonNull String provider,
        @NonNull Status status,
        @NonNull Integer score,
        String image
) {

    public enum Status {
        SUCCESS,
        FAILURE
    }
}
