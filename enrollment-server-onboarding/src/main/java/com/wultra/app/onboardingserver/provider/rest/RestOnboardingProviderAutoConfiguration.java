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
import com.wultra.app.onboardingserver.provider.OnboardingProviderAutoConfiguration;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeTypeUtils;

/**
 * Autoconfiguration for {@link OnboardingProvider} registering REST implementation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Configuration
@ConditionalOnProperty(value = "enrollment-server-onboarding.onboarding-adapter.url")
@EnableConfigurationProperties({RestOnboardingProviderConfiguration.class})
@AutoConfigureBefore(OnboardingProviderAutoConfiguration.class)
@Slf4j
class RestOnboardingProviderAutoConfiguration {

    @Bean("onboardingAdapterRestClient")
    public RestClient restClient(
            final RestOnboardingProviderConfiguration configuration,
            @Value("${enrollment-server-onboarding.onboarding-adapter.url}") final String url) throws RestClientException {

        logger.info("Initializing onboarding adapter RestClient for url={}", url);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MimeTypeUtils.APPLICATION_JSON_VALUE);
        headers.add("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE);

        final RestClientConfiguration config = new RestClientConfiguration();
        config.setBaseUrl(url);
        config.setConnectionTimeout((int) configuration.getConnectionTimeout().toMillis());
        config.setResponseTimeout(configuration.getResponseTimeout());
        config.setHandshakeTimeout(configuration.getHandshakeTimeout());
        config.setDefaultHttpHeaders(headers);

        return new DefaultRestClient(config);
    }

    @Bean
    OnboardingProvider onboardingProvider(@Qualifier("onboardingAdapterRestClient") RestClient restClient) {
        logger.warn("Initializing RestOnboardingProvider");
        return new RestOnboardingProvider(restClient);
    }
}
