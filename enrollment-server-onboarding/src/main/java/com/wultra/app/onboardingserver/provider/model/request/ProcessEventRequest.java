/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request object for {@link OnboardingProvider#processEvent(ProcessEventRequest)}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Getter
@ToString
@PublicApi
@EqualsAndHashCode
public final class ProcessEventRequest {

    /**
     * Unique identifier of the event.
     */
    @NonNull
    private String id;

    /**
     * Timestamp when the event was created.
     */
    @NonNull
    private LocalDateTime timestamp;

    @NonNull
    private String processId;

    @NonNull
    private String processType;

    @NonNull
    private String userId;

    /**
     * User ID in the external system, which is used by the provider.
     * It can be the same as {@code userId} or different, depending on the provider implementation.
     * It is {@code null} at the early phases of the process.
     */
    private String externalUserId;

    @NonNull
    private String identityVerificationId;

    @NonNull
    private EventType type;

    @NonNull
    private EventData eventData;
}
