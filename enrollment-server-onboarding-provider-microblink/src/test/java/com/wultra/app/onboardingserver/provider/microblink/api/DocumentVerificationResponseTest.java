/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.provider.microblink.api;

import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse.RecommendedOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for {@link DocumentVerificationResponse}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class DocumentVerificationResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @ParameterizedTest
    @CsvSource({
            "Unknown,UNKNOWN",
            "Undeterminable,UNDETERMINABLE",
            "Reject,REJECT",
            "Accept,ACCEPT",
            "Retry,RETRY",
            "ManuallyReview,MANUALLY_REVIEW",
            "SomethingNew,UNKNOWN"
    })
    void testParseRecommendedOutcome(final String value, final RecommendedOutcome expected) {
        final var json = """
                {"verification": {"recommendedOutcome": "%s"}}
                """.formatted(value);

        final var response = mapper.readValue(json, DocumentVerificationResponse.class);

        assertEquals(expected, response.verification().recommendedOutcome());
    }

    @Test
    void testParseRecommendedOutcome_missing() {
        final var response = mapper.readValue("{\"verification\": {\"result\": \"Pass\"}}", DocumentVerificationResponse.class);

        assertNull(response.verification().recommendedOutcome());
    }
}
