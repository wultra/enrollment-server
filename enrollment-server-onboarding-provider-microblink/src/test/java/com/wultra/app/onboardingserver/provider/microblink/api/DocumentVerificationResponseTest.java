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

import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse.CheckResult;
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

    @ParameterizedTest
    @CsvSource({
            "Unknown,UNKNOWN",
            "NotPerformed,NOT_PERFORMED",
            "Pass,PASS",
            "Fail,FAIL",
            "SomethingNew,UNKNOWN"
    })
    void testParseCheckResult(final String value, final CheckResult expected) {
        final var json = """
                {"checks": [{"name": "VisualCheck", "result": "%s", "type": "Check"}]}
                """.formatted(value);

        final var response = mapper.readValue(json, DocumentVerificationResponse.class);

        assertEquals(expected, response.checks().iterator().next().result());
    }

    @Test
    void testParseCheckResult_missing() {
        final var json = """
                {"checks": [{"name": "VisualCheck", "checks": [{"name": "DataIntegrityCheck", "type": "Check"}]}]}
                """;

        final var response = mapper.readValue(json, DocumentVerificationResponse.class);

        assertNull(response.checks().iterator().next().checks().iterator().next().result());
    }

    @Test
    void testParseCheckResult_null() {
        final var json = """
                {"checks": [{"name": "VisualCheck", "checks": [{"name": "DataIntegrityCheck", "result": null, "type": "Check"}]}]}
                """;

        final var response = mapper.readValue(json, DocumentVerificationResponse.class);

        assertNull(response.checks().iterator().next().checks().iterator().next().result());
    }

    @Test
    void testParseChecksTree() {
        final var json = """
              {
                "checks": [
                  {
                    "name": "ExtractedDataCheck",
                    "result": "Fail",
                    "certaintyLevel": "High",
                    "type": "DetailedCheck",
                    "checks": [
                      {
                        "name": "MatchCheck",
                        "result": "Fail",
                        "matchLevel": "Level1",
                        "type": "TieredCheck",
                        "checks": [
                          {
                            "field": "FirstName",
                            "result": "Fail",
                            "type": "FieldCheck"
                          }
                        ]
                      },
                      {
                        "name": "DataIntegrityCheck",
                        "result": "Pass",
                        "type": "Check"
                      }
                    ]
                  },
                  {
                    "name": "VisualCheck",
                    "result": "Pass",
                    "type": "Check",
                    "checks": [
                      {
                        "name": "GenerativeAiCheck",
                        "result": "Pass",
                        "matchLevel": "Level6",
                        "type": "TieredCheck"
                      }
                    ]
                  }
                ]
              }
              """;

        final var response = mapper.readValue(json, DocumentVerificationResponse.class);

        assertEquals(2, response.checks().size());

        final var extractedDataCheck = response.checks().get(0);
        assertEquals("ExtractedDataCheck", extractedDataCheck.name());
        assertEquals(CheckResult.FAIL, extractedDataCheck.result());
        assertEquals(2, extractedDataCheck.checks().size());

        final var matchCheck = extractedDataCheck.checks().get(0);
        assertEquals("MatchCheck", matchCheck.name());
        assertEquals(CheckResult.FAIL, matchCheck.result());
        // 3rd level sub-checks are not bound to a type and are therefore dropped, e.g. matchCheck has
        // no checks() accessor at all - SecondLevelCheck intentionally has no nested checks field

        final var dataIntegrityCheck = extractedDataCheck.checks().get(1);
        assertEquals("DataIntegrityCheck", dataIntegrityCheck.name());
        assertEquals(CheckResult.PASS, dataIntegrityCheck.result());

        final var visualCheck = response.checks().get(1);
        assertEquals("VisualCheck", visualCheck.name());
        assertEquals(CheckResult.PASS, visualCheck.result());

        final var generativeAiCheck = visualCheck.checks().iterator().next();
        assertEquals("GenerativeAiCheck", generativeAiCheck.name());
        assertEquals(CheckResult.PASS, generativeAiCheck.result());
    }
}
