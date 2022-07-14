/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.impl.util;

import java.time.Duration;
import java.util.Date;

/**
 * Utility class for date conversions.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class DateUtil {

    /**
     * Convert expiration time interval to minimal created date used for expiration.
     * @param expiration Expiration time interval.
     * @return Created date used for expiration.
     */
    public static Date convertExpirationToCreatedDate(Duration expiration) {
        long currentTime = System.currentTimeMillis();
        long expiredTime = currentTime - expiration.toMillis();
        return new Date(expiredTime);
    }
}
