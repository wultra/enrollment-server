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

import com.wultra.core.annotations.PublicApi;
import lombok.*;

import java.util.Locale;
import java.util.Map;

/**
 * Specialization of {@link EventData} for {@link EventType#PROCESS_FINISHED}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Getter
@ToString
@PublicApi
@EqualsAndHashCode
public final class ProcessFinishedEventData implements EventData {

    @NonNull
    private EventStatus status;

    private String errorDetail;

    @NonNull
    private DeviceData deviceData;

    @Builder
    @Getter
    @ToString
    @PublicApi
    @EqualsAndHashCode
    public static class DeviceData {

        @NonNull
        private Locale locale;

        private String ipAddress;

        private String httpUserAgent;

        private Map<String, Object> fdsData;
    }
}
