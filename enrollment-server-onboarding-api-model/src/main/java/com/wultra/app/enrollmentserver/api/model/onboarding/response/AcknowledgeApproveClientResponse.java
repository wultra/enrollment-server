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
package com.wultra.app.enrollmentserver.api.model.onboarding.response;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

/**
 * Response object for client approval acknowledgement.
 *
 * @param result The transition outcome, whether the transition to the next phase was successful.
 * @param resultReason The reason is used when the result is NOK to disclose the reason of the failed process (for example, user started new identity verification subprocess).
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Jacksonized
public record AcknowledgeApproveClientResponse(
        @NonNull Result result,
        String resultReason
) {
    public enum Result {
        OK, NOK
    }
}
