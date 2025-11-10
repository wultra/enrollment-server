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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

/**
 * Parser for Microblink BlinkID REST API response.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Getter
public class DocumentVerificationResponseParser {

    private final String responseJson;
    private String verificationJson;
    private String extractionFrontJson;
    private String extractionBackJson;

    private DocumentVerificationResponse response;

    public DocumentVerificationResponseParser(String responseJson) throws JsonProcessingException {
        this.responseJson = responseJson;

        parseResponse(responseJson);
    }

    private void parseResponse(final String responseJson) throws JsonProcessingException {
        final var mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        response = mapper.readValue(responseJson, DocumentVerificationResponse.class);

        final var root = mapper.readTree(responseJson);
        extractionFrontJson = root.path("extraction")
                .path("viz")
                .path("front")
                .toString();

        extractionBackJson = root.path("extraction")
                .path("viz")
                .path("back")
                .toString();

        verificationJson = root.path("verification")
                .toString();
    }
}
