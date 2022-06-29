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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Utility class for date conversions.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class DateUtil {

    /**
     * Convert expiration time to minimal created date used for expiration.
     * @param expirationSeconds Expiration time in seconds.
     * @return Created date used for expiration.
     */
    public static Date convertExpirationToCreatedDate(int expirationSeconds) {
        Calendar c = GregorianCalendar.getInstance();
        c.add(Calendar.SECOND, -expirationSeconds);
        return c.getTime();
    }
}
