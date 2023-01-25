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
 */
package com.wultra.app.onboardingserver.common.database.entity;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link OnboardingProcessEntityWrapper}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class OnboardingProcessEntityWrapperTest {

    @Test
    void testSetValues() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        final OnboardingProcessEntityWrapper tested = new OnboardingProcessEntityWrapper(process);

        tested.setLocale(Locale.GERMAN);
        tested.setIpAddress("127.0.0.1");
        tested.setUserAgent("Mozilla/5.0");

        assertEquals("{\"locale\":\"de\",\"ipAddress\":\"127.0.0.1\",\"userAgent\":\"Mozilla/5.0\"}", process.getCustomData());
    }

    @Test
    void testGetValues() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setCustomData("{\"locale\":\"fr\",\"ipAddress\":\"192.168.12.1\",\"userAgent\":\"Chrome/10.0\"}");

        final OnboardingProcessEntityWrapper tested = new OnboardingProcessEntityWrapper(process);

        assertAll(
                () -> assertEquals(new Locale("fr"), tested.getLocale()),
                () -> assertEquals("192.168.12.1", tested.getIpAddress()),
                () -> assertEquals("Chrome/10.0", tested.getUserAgent())
        );
    }

    @Test
    void testSetFdsValues() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        final OnboardingProcessEntityWrapper tested = new OnboardingProcessEntityWrapper(process);

        tested.setFdsData(Map.of("fdsIdentifier", "42"));

        assertEquals("{\"fdsData\":{\"fdsIdentifier\":\"42\"}}", process.getCustomData());
    }

    @Test
    void testGetFdsValues() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setCustomData("{\"fdsData\":{\"fdsIdentifier\":\"42\"}}");

        final OnboardingProcessEntityWrapper tested = new OnboardingProcessEntityWrapper(process);

        assertNotNull(tested.getFdsData());
        assertEquals("42", tested.getFdsData().get("fdsIdentifier"));
    }
}
