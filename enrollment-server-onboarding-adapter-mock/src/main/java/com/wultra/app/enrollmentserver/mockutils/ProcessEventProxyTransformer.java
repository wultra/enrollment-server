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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * WireMock response transformer for the {@code POST /process/event} endpoint.
 *
 * <p>Behaviour:</p>
 * <ul>
 *     <li>When {@code LIVENESS_CHECK_PROXY_BASE_URL} is set, the request is proxied to
 *         {@code ${LIVENESS_CHECK_PROXY_BASE_URL}/onboarding-events} (preserving the original
 *         {@code POST} method and body). An HTTP Basic {@code Authorization} header built from
 *         {@code LIVENESS_CHECK_PROXY_USERNAME} and {@code LIVENESS_CHECK_PROXY_PASSWORD} is
 *         added to the proxied request.</li>
 *     <li>When {@code LIVENESS_CHECK_PROXY_BASE_URL} is not set (blank), a static
 *         {@code 200} response with body file {@code body-process-event.json} is returned.</li>
 * </ul>
 */
public class ProcessEventProxyTransformer implements ResponseDefinitionTransformerV2 {

    private static final String PROXY_BASE_URL_ENV = "LIVENESS_CHECK_PROXY_BASE_URL";
    private static final String PROXY_USERNAME_ENV = "LIVENESS_CHECK_PROXY_USERNAME";
    private static final String PROXY_PASSWORD_ENV = "LIVENESS_CHECK_PROXY_PASSWORD";

    private static final String ONBOARDING_EVENTS_PATH = "/onboarding-events";
    private static final String PROCESS_EVENT_PATH = "/process/event";
    private static final String STATIC_BODY_FILE = "body-process-event.json";

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    @Override
    public String getName() {
        return "process-event-proxy-transformer";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public ResponseDefinition transform(final ServeEvent serveEvent) {
        final String baseUrl = System.getenv(PROXY_BASE_URL_ENV);

        if (baseUrl == null || baseUrl.isBlank()) {
            // No proxy configured, return the static stub body
            return ResponseDefinitionBuilder.like(serveEvent.getResponseDefinition())
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBodyFile(STATIC_BODY_FILE)
                    .build();
        }

        final String targetUrl = baseUrl + ONBOARDING_EVENTS_PATH;

        final String username = System.getenv(PROXY_USERNAME_ENV);
        final String password = System.getenv(PROXY_PASSWORD_ENV);
        final String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        return ResponseDefinitionBuilder.responseDefinition()
                .proxiedFrom(targetUrl)
                .withProxyUrlPrefixToRemove(PROCESS_EVENT_PATH)
                .withAdditionalRequestHeader("Authorization", "Basic " + credentials)
                .build();
    }
}

