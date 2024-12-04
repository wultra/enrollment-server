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
package com.wultra.app.onboardingserver.provider;

import com.wultra.app.onboardingserver.provider.rest.RestOnboardingProvider;
import com.wultra.app.onboardingserver.provider.rest.RestOnboardingProviderConfigProperties;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

/**
 * Configuration for {@link OnboardingProvider} registering {@link EmptyOnboardingProvider} or {@link RestOnboardingProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Configuration
@EnableConfigurationProperties(RestOnboardingProviderConfigProperties.class)
@Slf4j
class OnboardingProviderConfiguration {

    private static final String ONBOARDING_ADAPTER_REST_CLIENT = "onboardingAdapterRestClient";

    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${enrollment-server-onboarding.onboarding-adapter.url:}')")
    @Bean(ONBOARDING_ADAPTER_REST_CLIENT)
    RestClient restClient(
            final RestOnboardingProviderConfigProperties configuration,
            @Value("${enrollment-server-onboarding.onboarding-adapter.url}") final String url) throws RestClientException {

        logger.info("Initializing onboarding adapter RestClient for url={}", url);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MimeTypeUtils.APPLICATION_JSON_VALUE);
        headers.add("Content-Type", MimeTypeUtils.APPLICATION_JSON_VALUE);

        for (final Map.Entry<String, String> entry : configuration.getHeaders().entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }

        final RestClientConfiguration config = new RestClientConfiguration();
        config.setBaseUrl(url);
        config.setConnectionTimeout(configuration.getConnectionTimeout());
        config.setResponseTimeout(configuration.getResponseTimeout());
        config.setHandshakeTimeout(configuration.getHandshakeTimeout());
        config.setDefaultHttpHeaders(headers);
        config.setHttpBasicAuthEnabled(configuration.isHttpBasicAuthEnabled());
        config.setHttpBasicAuthUsername(configuration.getHttpBasicAuthUsername());
        config.setHttpBasicAuthPassword(configuration.getHttpBasicAuthPassword());

        if (configuration.isAcceptInvalidSslCertificate()) {
            logger.warn("Allowed usage of invalid ssl certificate for RestOnboardingProvider");
            config.setAcceptInvalidSslCertificate(configuration.isAcceptInvalidSslCertificate());
        }

        return new DefaultRestClient(config);
    }

    @ConditionalOnBean(name = ONBOARDING_ADAPTER_REST_CLIENT)
    @Bean
    OnboardingProvider restOnboardingProvider(
            @Qualifier(ONBOARDING_ADAPTER_REST_CLIENT) RestClient restClient,
            final RestOnboardingProviderConfigProperties configuration) {

        logger.info("Initializing RestOnboardingProvider");
        return new RestOnboardingProvider(restClient, configuration);
    }

    @ConditionalOnMissingBean(OnboardingProvider.class)
    @Bean
    OnboardingProvider emptyOnboardingProvider() {
        logger.warn("No OnboardingProvider found, registering default EmptyOnboardingProvider");
        return new EmptyOnboardingProvider();
    }
}