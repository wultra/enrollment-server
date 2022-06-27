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
package com.wultra.app.onboardingserver.database.entity;

import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
class OnboardingOtpEntityTest {

    @Test
    void hasExpiredTest() {
        final long TIME_MS = System.currentTimeMillis();

        OnboardingOtpEntity beforeExpiration = new OnboardingOtpEntity();
        beforeExpiration.setTimestampCreated(new Date(TIME_MS));
        beforeExpiration.setTimestampExpiration(new Date(TIME_MS + 1));
        assertFalse(beforeExpiration.hasExpired(), "Not yet expired OTP");

        OnboardingOtpEntity sharplyBeforeExpiration = new OnboardingOtpEntity();
        sharplyBeforeExpiration.setTimestampCreated(new Date(TIME_MS));
        sharplyBeforeExpiration.setTimestampExpiration(new Date(TIME_MS));
        assertFalse(sharplyBeforeExpiration.hasExpired(), "Sharply not expired OTP");

        OnboardingOtpEntity afterExpiration = new OnboardingOtpEntity();
        afterExpiration.setTimestampCreated(new Date(TIME_MS));
        afterExpiration.setTimestampExpiration(new Date(TIME_MS - 1));
        assertTrue(afterExpiration.hasExpired(), "Already expired OTP");
    }

}
