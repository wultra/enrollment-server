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

import com.wultra.app.onboardingserver.common.database.entity.DocumentExtractedDataValue;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parser for normalized data (according to {@link DocumentExtractedDataValue}) extracted from the Microblink response.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class MicroblinkExtractedDataParser {

    private final ObjectMapper mapper;

    /**
     * Parses Microblink extracted data JSON to {@link DocumentExtractedDataValue} JSON.
     *
     * @param extractedDataJson extracted data in Microblink JSON format
     * @return normalized extracted data as {@link DocumentExtractedDataValue} JSON string
     */
    public String parseExtractedData(final String extractedDataJson, final DocumentVerificationResponse.Extraction extraction) {
        if (!StringUtils.hasLength(extractedDataJson)) {
            return null;
        }

        try {
            final var root = mapper.readTree(extractedDataJson);

            final var extractedValueByField = extractFieldValues(root);

            final var country = Optional.ofNullable(extraction)
                    .map(DocumentVerificationResponse.Extraction::classInfo)
                    .map(DocumentVerificationResponse.ExtractionClassInfo::isoAlpha3CountryCode)
                    .orElse(null);

            final var extractedDataValue = DocumentExtractedDataValue.builder()
                    .givenNames(asText(extractedValueByField.getOrDefault(ExtractedDataField.GIVEN_NAMES, null)))
                    .surname(asText(extractedValueByField.getOrDefault(ExtractedDataField.SURNAME, null)))
                    .dateOfBirth(asDate(extractedValueByField.getOrDefault(ExtractedDataField.DATE_OF_BIRTH, null)))
                    .placeOfBirth(asText(extractedValueByField.getOrDefault(ExtractedDataField.PLACE_OF_BIRTH, null)))
                    .sex(asText(extractedValueByField.getOrDefault(ExtractedDataField.SEX, null)))
                    .nationality(asText(extractedValueByField.getOrDefault(ExtractedDataField.NATIONALITY, null)))
                    .personalNumber(asText(extractedValueByField.getOrDefault(ExtractedDataField.PERSONAL_NUMBER, null)))
                    .documentNumber(asText(extractedValueByField.getOrDefault(ExtractedDataField.DOCUMENT_NUMBER, null)))
                    .dateOfIssue(asDate(extractedValueByField.getOrDefault(ExtractedDataField.DATE_OF_ISSUE, null)))
                    .dateOfExpiry(asDate(extractedValueByField.getOrDefault(ExtractedDataField.DATE_OF_EXPIRY, null)))
                    .authority(asText(extractedValueByField.getOrDefault(ExtractedDataField.AUTHORITY, null)))
                    .country(country)
                    .build();

            return mapper.writeValueAsString(extractedDataValue);
        } catch (final JacksonException e) {
            logger.warn("Serialization of normalized extracted data failed", e);
            return null;
        }
    }

    private static Map<ExtractedDataField, ExtractedValue> extractFieldValues(final JsonNode root) {
        final var result = new EnumMap<ExtractedDataField, ExtractedValue>(ExtractedDataField.class);

        for (final var node : root) {
            final var microblinkField = node.path("field").asText(null);
            final var field = ExtractedDataField.fromMicroblinkField(microblinkField);

            if (field == null) {
                continue;
            }

            final ExtractedValue value = extractValue(node);

            if (value != null) {
                result.put(field, value);
            }
        }
        return result;
    }

    private static ExtractedValue extractValue(final JsonNode node) {
        if (node.hasNonNull("value")) {
            return new ExtractedValue.Text(node.path("value").asText(null));
        } else if (isSuccessfullyParsed(node)) {
            final int year = node.path("year").asInt(0);
            final int month = node.path("month").asInt(0);
            final int day = node.path("day").asInt(0);

            return new ExtractedValue.Date(LocalDate.of(year, month, day));
        } else {
            return null;
        }
    }

    private static boolean isSuccessfullyParsed(final JsonNode node) {
        return node.path("successfullyParsed").asBoolean(false);
    }

    private static String asText(final ExtractedValue source) {
        return (source instanceof ExtractedValue.Text target) ? target.value() : null;
    }

    private static LocalDate asDate(final ExtractedValue source) {
        return (source instanceof ExtractedValue.Date target) ? target.value() : null;
    }

    private sealed interface ExtractedValue permits ExtractedValue.Text, ExtractedValue.Date {
        record Text(String value) implements ExtractedValue {}
        record Date(LocalDate value) implements ExtractedValue {}
    }
}
