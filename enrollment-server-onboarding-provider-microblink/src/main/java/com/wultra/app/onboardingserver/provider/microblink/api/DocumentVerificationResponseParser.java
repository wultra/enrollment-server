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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Parser for Microblink BlinkID REST API response.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
public class DocumentVerificationResponseParser {

    final ObjectMapper objectMapper;

    public DocumentVerificationResponseParser(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DocumentVerificationParsedResponse parseResponse(final String responseJson) throws JsonProcessingException {
        final var mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final var response = mapper.readValue(responseJson, DocumentVerificationParsedResponse.class);

        final var root = mapper.readTree(responseJson);
        final var extractionFrontJson = root.path("extraction")
                .path("viz")
                .path("front")
                .toString();

        final var extractionBackJson = root.path("extraction")
                .path("viz")
                .path("back")
                .toString();

        final var verificationJson = root.path("verification")
                .toString();

        ((ObjectNode) root).remove("images");
        final var responseWithoutImagesJson = root.toString();

        ((ObjectNode) root).remove("extraction");
        final var responseWithoutPersonalDataJson = root.toString();

        return response.toBuilder()
                .extractionFrontJson(extractionFrontJson)
                .extractionBackJson(extractionBackJson)
                .verificationJson(verificationJson)
                .responseWithoutImagesJson(responseWithoutImagesJson)
                .responseWithoutPersonalDataJson(responseWithoutPersonalDataJson)
                .build();
    }
}
