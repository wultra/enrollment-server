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

import com.wultra.app.onboardingserver.common.annotation.PublicApi;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import lombok.*;

import java.util.Locale;
import java.util.Map;

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

    @NonNull
    private String processId;

    @NonNull
    private String userId;

    @NonNull
    private String identityVerificationId;

    @NonNull
    private EventType type;

    @NonNull
    private EventData eventData;

    public interface EventData {
        /**
         * Return data represented as a map.
         *
         * @return map
         */
        Map<String, Object> asMap();
    }

    public interface FinishedEventData extends EventData {
    }

    /**
     * Specialization of {@link EventData} for {@link EventType#FINISHED} at Broadcom FDS.
     */
    @Builder
    @Getter
    @ToString
    @PublicApi
    @EqualsAndHashCode
    public static class BroadcomFinishedEventData implements FinishedEventData {

        @NonNull
        private Locale locale;

        @NonNull
        private String httpUserAgent;

        @NonNull
        private String clientIPAddress;

        /**
         * Unique ID of the request
         */
        private String callerId;

        @Builder.Default
        private String deviceIDType = "DEVICEID.HTTP";

        /**
         * Value to be obtained from Broadcom SDK on mobile device. Not filled yet.
         */
        private String deviceIDValue;

        /**
         * Value to be obtained from Broadcom SDK on mobile device. Not filled yet.
         */
        private String deviceSignature;

        @Override
        public Map<String, Object> asMap() {
            return Map.of(
                    "locale", locale.getLanguage(),
                    "httpUserAgent", httpUserAgent,
                    "clientIPAddress", clientIPAddress,
                    "callerId", callerId,
                    "deviceIDType", deviceIDType
                    // deviceIDValue - Not filled yet
                    // deviceSignature - Not filled yet
            );
        }
    }

    public enum EventType {
        FINISHED
    }
}
