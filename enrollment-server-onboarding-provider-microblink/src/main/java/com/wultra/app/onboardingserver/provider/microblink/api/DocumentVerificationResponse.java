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

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of Microblink BlinkID REST API.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
public record DocumentVerificationResponse(
        Verification verification,
        Extraction extraction,
        Runtime runtime,
        List<Image> images,
        List<Message> messages,
        List<Check> checks
){
    public record Verification(
            String result,
            @JsonFormat(with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            RecommendedOutcome recommendedOutcome,
            String certaintyLevel
    ) {}

    /**
     * Recommended outcome of the Microblink verification. Unrecognized values are mapped to {@link #UNKNOWN}.
     */
    public enum RecommendedOutcome {
        @JsonEnumDefaultValue
        @JsonProperty("Unknown") UNKNOWN,
        @JsonProperty("Undeterminable") UNDETERMINABLE,
        @JsonProperty("Reject") REJECT,
        @JsonProperty("Accept") ACCEPT,
        @JsonProperty("Retry") RETRY,
        @JsonProperty("ManuallyReview") MANUALLY_REVIEW
    }

    public record Extraction(
            List<Result> overall,
            ExtractionClassInfo classInfo
    ) {}

    public record Result(
            String field,
            String value,
            Integer day,
            Integer month,
            Integer year
    ) {}

    public record ExtractionClassInfo(
            String type,
            String isoAlpha3CountryCode
    ) {}

    public record Runtime(
            String traceId
    ) {}

    public record Image(
            String name,
            String base64
    ) {}

    public record Message(
            String code,
            String message,
            String status
    ) {}

    /**
     * A 1st level node ("group"/"category") in the Microblink {@code checks} tree.
     *
     * @param name name of the check group, e.g. {@code ExtractedDataCheck}
     * @param result result of the check group, unrecognized values are mapped to {@link CheckResult#UNKNOWN}
     * @param type type of the check node, e.g. {@code Check}, {@code DetailedCheck}, {@code TieredCheck}
     * @param checks 2nd level checks we evaluate; any 3rd level or deeper sub-checks are not needed for
     *               evaluation and intentionally not bound to a type, so they are ignored on parsing
     */
    public record Check(
            String name,
            @JsonFormat(with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            CheckResult result,
            String type,
            List<SecondLevelCheck> checks
    ) {}

    /**
     * A 2nd level node in the Microblink {@code checks} tree, i.e. the actual check we evaluate. Any nested
     * {@code checks} (3rd level or deeper) are intentionally not declared here, so they are ignored on parsing.
     *
     * @param name name of the check, e.g. {@code DataIntegrityCheck}
     * @param result result of the check, unrecognized values are mapped to {@link CheckResult#UNKNOWN}
     * @param type type of the check node, e.g. {@code Check}, {@code DetailedCheck}, {@code TieredCheck}
     */
    public record SecondLevelCheck(
            String name,
            @JsonFormat(with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            CheckResult result,
            String type
    ) {}

    /**
     * Result of a Microblink check. Unrecognized values are mapped to {@link #UNKNOWN}.
     */
    public enum CheckResult {
        @JsonEnumDefaultValue
        @JsonProperty("Unknown") UNKNOWN,
        @JsonProperty("NotPerformed") NOT_PERFORMED,
        @JsonProperty("Pass") PASS,
        @JsonProperty("Fail") FAIL
    }
}
