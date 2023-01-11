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
 */
package com.wultra.app.onboardingserver.provider;

import com.wultra.app.onboardingserver.provider.model.request.SendOtpCodeRequest;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test for {@link SendOtpCodeRequest}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class SendOtpCodeRequestTest {

    @Test
    void testToString() {
        final String result = SendOtpCodeRequest.builder()
                .otpCode("top secret")
                .processId("666")
                .locale(Locale.ENGLISH)
                .otpType(SendOtpCodeRequest.OtpType.ACTIVATION)
                .userId("42")
                .build().toString();
        assertFalse(result.contains("top secret"), "otp must not be present at " + result);
    }
}
