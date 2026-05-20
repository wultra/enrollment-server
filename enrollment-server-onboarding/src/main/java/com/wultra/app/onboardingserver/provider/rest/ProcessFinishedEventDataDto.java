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

import lombok.Builder;

import java.util.Map;

/**
 * {@link EventDataDto} for {@link EventTypeDto#PROCESS_FINISHED}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
record ProcessFinishedEventDataDto(Process process) implements EventDataDto {

    @Builder
    public record Process(
            String status,
            String errorDetail,
            DeviceData deviceData
    ) {}

    @Builder
    public record DeviceData(
            String locale,
            String ipAddress,
            String httpUserAgent,
            Map<String, Object> fdsData
    ) {}
}
