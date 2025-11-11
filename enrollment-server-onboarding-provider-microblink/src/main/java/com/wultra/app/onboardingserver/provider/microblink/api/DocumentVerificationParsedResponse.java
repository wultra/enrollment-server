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
package com.wultra.app.onboardingserver.provider.microblink.api;

import lombok.Builder;

import java.util.List;

/**
 * Response from Microblink BlinkID REST API.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Builder(toBuilder = true)
public record DocumentVerificationParsedResponse(
    Verification verification,
    Extraction extraction,
    Runtime runtime,
    List<Image> images,
    String verificationJson,
    String extractionFrontJson,
    String extractionBackJson
) {
    public record Verification(
            String result
    ) {}

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
            String type
    ) {}

    public record Runtime(
            String traceId
    ) {}

    public record Image(
            String name,
            String base64
    ) {}
}
