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
package com.wultra.app.enrollmentserver.mockutils;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.http.Request;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WireMock response transformer that stores OTP codes received on the send endpoint
 * and exposes them through the detail endpoint for onboarding adapter mock scenarios.
 */
public class OtpStoreTransformer implements ResponseDefinitionTransformerV2 {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<OtpKey, String> otpStore = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "otp-store-transformer";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public void start() {
        otpStore.clear();
    }

    @Override
    public void stop() {
        otpStore.clear();
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        Request request = serveEvent.getRequest();

        String method = request.getMethod().value();
        String url = request.getUrl(); // path + query if present

        try {
            if ("POST".equals(method) && url.startsWith("/otp/send")) {
                return handleOtpSend(request);
            }

            if ("POST".equals(method) && url.startsWith("/otp/detail")) {
                return handleOtpDetail(request);
            }

            return new ResponseDefinitionBuilder()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "error": "Unsupported endpoint for otp-store-transformer"
                        }
                        """)
                    .build();

        } catch (BadRequestException e) {
            return new ResponseDefinitionBuilder()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "error": "%s"
                        }
                        """.formatted(escapeJson(e.getMessage())))
                    .build();

        } catch (Exception e) {
            return new ResponseDefinitionBuilder()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "error": "Internal transformer error",
                          "message": "%s"
                        }
                        """.formatted(escapeJson(e.getMessage())))
                    .build();
        }
    }

    private ResponseDefinition handleOtpSend(Request request) throws Exception {
        OtpSendRequest body = readBody(request, OtpSendRequest.class);

        requireNonBlank(body.processId, "Missing processId");
        requireNonBlank(body.otpType, "Missing otpType");
        requireNonBlank(body.otpCode, "Missing otpCode");

        otpStore.put(new OtpKey(body.processId, body.otpType), body.otpCode);

        return new ResponseDefinitionBuilder()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "otpSent": true
                    }
                    """)
                .build();
    }

    private ResponseDefinition handleOtpDetail(Request request) throws Exception {
        OtpDetailRequest body = readBody(request, OtpDetailRequest.class);

        requireNonBlank(body.processId, "Missing processId");
        requireNonBlank(body.otpType, "Missing otpType");

        String otpCode = otpStore.get(new OtpKey(body.processId, body.otpType));

        if (otpCode == null) {
            return new ResponseDefinitionBuilder()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("""
                        {
                          "error": "OTP not found for given processId and otpType"
                        }
                        """)
                    .build();
        }

        return new ResponseDefinitionBuilder()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "processId": "%s",
                      "otpType": "%s",
                      "otpCode": "%s"
                    }
                    """.formatted(
                        escapeJson(body.processId),
                        escapeJson(body.otpType),
                        escapeJson(otpCode)
                ))
                .build();
    }

    private <T> T readBody(Request request, Class<T> clazz) throws Exception {
        String body = request.getBodyAsString();
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Request body is empty");
        }
        return objectMapper.readValue(body, clazz);
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private record OtpKey(String processId, String otpType) {}

    /**
     * Request object representing {@code com.wultra.app.onboardingserver.provider.rest.OtpSendRequestDto}
     * in the onboarding adapter mock.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OtpSendRequest {

        public String processId;
        
        public String processType;

        public String userId;
        
        public String language;
        
        public String otpCode;
        
        public String otpType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OtpDetailRequest {
       
        public String processId;
       
        public String otpType;
    }

    private static class BadRequestException extends RuntimeException {
        BadRequestException(String message) {
            super(message);
        }
    }
}
