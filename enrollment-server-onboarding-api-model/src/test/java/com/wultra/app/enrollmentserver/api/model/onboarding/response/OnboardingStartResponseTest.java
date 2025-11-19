/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.api.model.onboarding.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test for {@link OnboardingStartResponse}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class OnboardingStartResponseTest {

    @Test
    void testToString() {
        final var tested = OnboardingStartResponse.builder()
                .processId("test-process-id")
                .onboardingStatus(null)
                .config(null)
                .activationCode("top secret")
                .build();

        final var result = tested.toString();

        assertFalse(result.contains("top secret"), "activationCode must not be present at " + result);
    }
}