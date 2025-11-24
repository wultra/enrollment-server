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
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationRequest;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing sensitive information from Microblink logs.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
final class MicroblinkLogSanitizationUtils {

    private static final Pattern LICENSE_KEY_PATTERN = Pattern.compile("license-key=([^,}\\s]+)");

    private static final Pattern IMAGE_BASE64_PATTERN = Pattern.compile(
            "class DocumentVerificationImageSource \\{[\\s\\S]*?base64:\\s*([^\\s]+)"
    );

    private static final Pattern JSON_SENSITIVE_FIELDS_PATTERN = Pattern.compile("\"(?i)(value|day|month|year|base64)\"\\s*:\\s*\"?(.*?)\"?(?=[,}])");

    private MicroblinkLogSanitizationUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Sanitizes {@link VerificationSdkInfo#attributes} by replacing {@code license-key} value with its length.
     *
     * @param sdkInfo SDK info to sanitize
     * @return sanitized SDK info as string
     */
    public static String sanitizeSdkInfo(final VerificationSdkInfo sdkInfo) {
        if (sdkInfo == null) {
            return null;
        }

        return LICENSE_KEY_PATTERN.matcher(sdkInfo.toString())
                .replaceAll(mr -> "license-key=length:" + mr.group(1).length());
    }

    /**
     * Sanitizes {@link DocumentVerificationRequest} by replacing all {@code base64} image data with its length.
     *
     * @param request Document verification request to sanitize
     * @return sanitized request as string
     */
    public static String sanitizeDocumentVerificationRequest(final DocumentVerificationRequest request) {
        if (request == null) {
            return null;
        }

        return IMAGE_BASE64_PATTERN.matcher(request.toString())
                .replaceAll(mr -> {
                    String base64 = mr.group(1);
                    return mr.group(0).replace(base64, "length=" + base64.length());
                });
    }

    /**
     * Sanitizes JSON response from document verification by replacing sensitive field values with their lengths.
     *
     * As sensitive fields are considered these fields: value, day, month, year, base64.
     *
     * @param jsonResponse JSON response to sanitize
     * @return sanitized JSON response
     */
    public static String sanitizeDocumentVerificationResponseJson(final String jsonResponse) {
        if (jsonResponse == null) {
            return null;
        }

        return JSON_SENSITIVE_FIELDS_PATTERN.matcher(jsonResponse)
                .replaceAll(match -> {
                    String key = match.group(1);
                    String value = match.group(2);
                    return "\"%s\": \"length=%d\"".formatted(key, value.length());
                });
    }
}
