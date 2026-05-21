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

import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link MicroblinkExtractedDataParser}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class MicroblinkExtractedDataParserTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String EXTRACTED_DATA_JSON = loadExtractedDataJson();

    private MicroblinkExtractedDataParser tested;

    @BeforeEach
    void setUp() {
        tested = new MicroblinkExtractedDataParser(MAPPER);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testParseExtractedData_extractedDataJsonWithoutLength(final String extractedDataJson) {
        // given
        // -

        // when
        final var result = tested.parseExtractedData(extractedDataJson, null);

        // then
        assertNull(result);
    }

    @Test
    void testParseExtractedData_extractedDataInvalidJson() {
        // given
        // -

        // when
        final var result = tested.parseExtractedData("invalid_json", null);

        // then
        assertNull(result);
    }

    @Test
    void testParseExtractedData_extractedDataEmptyJson() {
        // given
        // -

        // when
        final var result = tested.parseExtractedData("{}", null);

        // then
        assertEquals(buildExpectedEmptyResult(), result);
    }

    @Test
    void testParseExtractedData_extractedDataWithAllValues() {
        // given
        // -

        // when
        final var result = tested.parseExtractedData(EXTRACTED_DATA_JSON, buildExtraction());

        // then
        assertEquals(buildExpectedEmptyResultWithAllValues(), result);
    }

    private static String buildExpectedEmptyResult() {
        return """
                {"givenNames":null,"surname":null,"dateOfBirth":null,"placeOfBirth":null,"country":null,"sex":null,"nationality":null,"personalNumber":null,"documentNumber":null,"dateOfIssue":null,"dateOfExpiry":null,"authority":null}""";
    }

    private static String buildExpectedEmptyResultWithAllValues() {
        return """
                {"givenNames":"PRENUMELE","surname":"NUMELE","dateOfBirth":"1999-12-24","placeOfBirth":"Prague","country":"MDA","sex":"X","nationality":"Moldovan","personalNumber":"816008/0610","documentNumber":"EA0000000","dateOfIssue":"2012-01-10","dateOfExpiry":"2025-10-13","authority":"Chisinau A"}""";
    }

    @SneakyThrows
    private static String loadExtractedDataJson() {
        final var json = new ClassPathResource("microblink_id_card_pass_response_body.json").getContentAsString(StandardCharsets.UTF_8);
        return MAPPER.readTree(json)
                .path("extraction")
                .path("overall")
                .toString();
    }

    private static DocumentVerificationResponse.Extraction buildExtraction() {
        return new DocumentVerificationResponse.Extraction(
                List.of(),
                new DocumentVerificationResponse.ExtractionClassInfo("Id", "MDA")
        );
    }
}
