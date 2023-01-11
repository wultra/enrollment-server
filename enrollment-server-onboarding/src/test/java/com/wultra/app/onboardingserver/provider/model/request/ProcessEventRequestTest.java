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

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link ProcessEventRequest}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class ProcessEventRequestTest {

    @Test
    void testBroadcomFinishedEventDataAsMap() {
        final Map<String, Object> result = ProcessEventRequest.DefaultFinishedEventData.builder()
                .requestId("75ABE013-CA32-40FD-A104-0C481D44FCD1")
                .locale(Locale.ENGLISH)
                .clientIPAddress("127.0.0.1")
                .httpUserAgent("Mozilla/5.0")
                .build()
                .asMap();

        assertAll(
                () -> assertEquals("75ABE013-CA32-40FD-A104-0C481D44FCD1", result.get("requestId")),
                () -> assertEquals("en", result.get("language")),
                () -> assertEquals("127.0.0.1", result.get("clientIPAddress")),
                () -> assertEquals("Mozilla/5.0", result.get("httpUserAgent"))
        );
    }
}
