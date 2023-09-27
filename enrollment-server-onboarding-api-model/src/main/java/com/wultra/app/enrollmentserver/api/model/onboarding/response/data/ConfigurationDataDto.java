/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.enrollmentserver.api.model.onboarding.response.data;

import lombok.Data;

/**
 * Configuration data of the server useful for the client.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Data
public class ConfigurationDataDto {

    /**
     * OTP resend period (ISO 8601 format).
     *
     * @deprecated ISO 8601 format does not have native support on mobile platforms,
     * leading to complexities in mobile client processing. Use {@link #otpResendPeriodSeconds}
     * for a simpler representation in seconds. Scheduled for removal in future versions.
     * See https://github.com/wultra/enrollment-server/issues/829 for more details.
     * TODO (@jandusil, 2023-08-12, #829)
     */
    @Deprecated
    private String otpResendPeriod;

    /**
     * OTP resend period in seconds for easier parsing on mobile platforms.
     */
    private long otpResendPeriodSeconds;

}
