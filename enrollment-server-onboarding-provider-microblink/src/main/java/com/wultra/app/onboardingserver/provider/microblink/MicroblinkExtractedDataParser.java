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

package com.wultra.app.onboardingserver.provider.microblink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.onboardingserver.common.database.entity.DocumentExtractedDataValue;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationParsedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parser for normalized data (according to {@link DocumentExtractedDataValue}) extracted from the Microblink response.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
@Slf4j
public class MicroblinkExtractedDataParser {

    private final ObjectMapper mapper;

    public MicroblinkExtractedDataParser() {
        mapper = new ObjectMapper();
    }

    /**
     * Parses Microblink extracted data JSON to {@link DocumentExtractedDataValue} JSON.
     *
     * @param extractedDataJson extracted data in Microblink JSON format
     * @return normalized extracted data as {@link DocumentExtractedDataValue} JSON string
     */
    public String parseExtractedData(final String extractedDataJson, final DocumentVerificationParsedResponse.Extraction extraction) {
        try {
            final var root = mapper.readTree(extractedDataJson);

            final var extractedValueByField = extractFieldValues(root);

            final var country = Optional.ofNullable(extraction)
                    .map(DocumentVerificationParsedResponse.Extraction::classInfo)
                    .map(DocumentVerificationParsedResponse.ExtractionClassInfo::isoAlpha3CountryCode)
                    .orElse(null);

            final var extractedDataValue = DocumentExtractedDataValue.builder()
                    .givenNames(extractedValueByField.getOrDefault(ExtractedDataField.GIVEN_NAMES, null))
                    .surname(extractedValueByField.getOrDefault(ExtractedDataField.SURNAME, null))
                    .dateOfBirth(extractedValueByField.getOrDefault(ExtractedDataField.DATE_OF_BIRTH, null))
                    .sex(extractedValueByField.getOrDefault(ExtractedDataField.SEX, null))
                    .nationality(extractedValueByField.getOrDefault(ExtractedDataField.NATIONALITY, null))
                    .personalNumber(extractedValueByField.getOrDefault(ExtractedDataField.PERSONAL_NUMBER, null))
                    .documentNumber(extractedValueByField.getOrDefault(ExtractedDataField.DOCUMENT_NUMBER, null))
                    .dateOfIssue(extractedValueByField.getOrDefault(ExtractedDataField.DATE_OF_ISSUE, null))
                    .dateOfExpiry(extractedValueByField.getOrDefault(ExtractedDataField.DATE_OF_EXPIRY, null))
                    .authority(extractedValueByField.getOrDefault(ExtractedDataField.AUTHORITY, null))
                    .country(country)
                    .build();

            return mapper.writeValueAsString(extractedDataValue);
        } catch (final JsonProcessingException e) {
            logger.warn("Serialization of normalized extracted data failed", e);
            return null;
        }
    }

    private static Map<ExtractedDataField, String> extractFieldValues(final JsonNode root) {
        final var result = new EnumMap<ExtractedDataField, String>(ExtractedDataField.class);

        for (final var node : root) {
            final var microblinkField = node.path("field").asText(null);
            final var field = ExtractedDataField.fromMicroblinkField(microblinkField);

            if (field == null) {
                continue;
            }

            final var value = node.has("value")
                    ? node.path("value").asText(null)
                    // fallback for Date values
                    : node.path("originalResult").path(0).path("value").asText(null);

            if (value != null) {
                result.put(field, value);
            }
        }
        return result;
    }
}
