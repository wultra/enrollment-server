/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.rest;

import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Configuration for {@link OnboardingProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConfigurationProperties("enrollment-server-onboarding.onboarding-adapter")
@Getter
@Setter
class RestOnboardingProviderConfiguration {

    private static final String CORRELATION_HEADER_DEFAULT_NAME = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER_DEFAULT_NAME = "X-Request-Id";

    private Duration connectionTimeout = Duration.ofSeconds(2);

    private Duration handshakeTimeout = Duration.ofSeconds(5);

    private Duration responseTimeout = Duration.ofSeconds(5);

    private boolean acceptInvalidSslCertificate;

    private boolean httpBasicAuthEnabled;

    private String httpBasicAuthUsername;

    private String httpBasicAuthPassword;

    private Header correlationHeader = new Header(CORRELATION_HEADER_DEFAULT_NAME);

    private Header requestIdHeader = new Header(REQUEST_ID_HEADER_DEFAULT_NAME);

    private Map<String, String> headers = Collections.emptyMap();

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Header {
        private String name;
    }
}
