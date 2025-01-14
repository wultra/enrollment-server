/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.api.model.enrollment.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

/**
 * Class representing a device registration request.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
public class PushRegisterRequest {

    /**
     * The platform.
     */
    @NotNull
    private Platform platform;

    /**
     * APNs environment (optional).
     */
    private Environment environment;

    /**
     * The push token is the value received from APNs, FCM, or HMS services without any modification.
     */
    @NotBlank
    @ToString.Exclude
    @Schema(description = "The push token is the value received from APNs, FCM, or HMS services without any modification.")
    private String token;

    public enum Platform {
        @JsonProperty("apns")
        APNS,

        @JsonProperty("fcm")
        FCM,

        @JsonProperty("hms")
        HMS,

        @JsonProperty("ios")
        @Deprecated
        IOS,

        @JsonProperty("android")
        @Deprecated
        ANDROID,

        @JsonProperty("huawei")
        @Deprecated
        HUAWEI
    }

    public enum Environment {
        @JsonProperty("development")
        DEVELOPMENT,

        @JsonProperty("production")
        PRODUCTION
    }

}
