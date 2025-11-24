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

package com.wultra.app.onboardingserver.provider.microblink;

import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationImageSource;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link MicroblinkLogSanitizationUtils}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class MicroblinkLogSanitizationUtilsTest {

    private static String responseBeforeSanitization;
    private static String responseAfterSanitization;

    @BeforeAll
    static void setup() throws IOException {
        responseBeforeSanitization = new ClassPathResource("microblink_response_before_sanitization.json").getContentAsString(StandardCharsets.UTF_8);
        responseAfterSanitization = new ClassPathResource("microblink_response_after_sanitization.json").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void testSanitizeSdkInfo_sdkInfoIsNull_nullIsReturned() {
        // given
        // -

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeSdkInfo(null);

        // then
        assertNull(sanitizedString);
    }

    @Test
    void testSanitizeSdkInfo_licenseKeyIsNotInAttributes_nothingIsSanitized() {
        // given
        final var sdkInfo = new VerificationSdkInfo(Map.of("key", "value"));

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeSdkInfo(sdkInfo);

        // then
        assertEquals("VerificationSdkInfo(attributes={key=value})", sanitizedString);
    }

    @Test
    void testSanitizeSdkInfo_licenseKeyIsInAttributes_licenseKeyValueIsReplaced() {
        // given
        final var sdkInfo = new VerificationSdkInfo(Map.of("license-key", "abc-123"));

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeSdkInfo(sdkInfo);

        // then
        assertEquals("VerificationSdkInfo(attributes={license-key=length:7})", sanitizedString);
    }

    @Test
    void testSanitizeDocumentVerificationRequest_requestIsNull_nullIsReturned() {
        // given
        // -

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationRequest(null);

        // then
        assertNull(sanitizedString);
    }

    @Test
    void testSanitizeDocumentVerificationRequest_requestWithManyBase64Values_allValuesAreReplacedByLength() {
        // given
        final var imageFront = new DocumentVerificationImageSource();
        imageFront.setBase64(Base64.getEncoder().encodeToString(new byte[] { 1, 2 } ));

        final var imageBack = new DocumentVerificationImageSource();
        imageBack.setBase64(Base64.getEncoder().encodeToString(new byte[] { 3, 4, 5 } ));

        final var request = new DocumentVerificationRequest();
        request.setImageFront(imageFront);
        request.setImageBack(imageBack);

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationRequest(request);

        // then
        final var expectedString = """
                class DocumentVerificationRequest {
                    imageFront: class DocumentVerificationImageSource {
                        url: null
                        base64: length=4
                    }
                    imageBack: class DocumentVerificationImageSource {
                        url: null
                        base64: length=4
                    }
                    imageBarcode: null
                    options: null
                    useCase: null
                    captureSessionId: null
                    sessionID: null
                }""";

        assertEquals(expectedString, sanitizedString);
    }

    @Test
    void testSanitizeDocumentVerificationResponseJson_responseIsNull_nullIsReturned() {
        // given
        // -

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationResponseJson(null);

        // then
        assertNull(sanitizedString);
    }

    @Test
    void testSanitizeDocumentVerificationResponseJson_requestWithManySensitiveValues_allValuesAreReplacedByLength() {
        // given
        // -

        // when
        final var sanitizedString = MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationResponseJson(responseBeforeSanitization);

        // then
        assertEquals(responseAfterSanitization, sanitizedString);
    }
}
