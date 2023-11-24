/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.innovatrics;

import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Innovatrics configuration.
 * <p>
 * It is not possible to combine Innovatrics with other providers such as iProov or ZenID.
 * Both providers, document verifier and presence check, must be configured to {@code innovatrics}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
        """)
@ComponentScan(basePackages = {"com.wultra.app.onboardingserver.provider.innovatrics"})
@Configuration
@Slf4j
class InnovatricsConfig {

    /**
     * Prepares REST client specific to Innovatrics.
     *
     * @param configProps Configuration properties
     * @return REST client for Innovatrics service API calls.
     */
    @Bean("restClientInnovatrics")
    public RestClient restClientInnovatrics(final InnovatricsConfigProps configProps) throws RestClientException {
        final String serviceBaseUrl = configProps.getServiceBaseUrl();
        logger.info("Registering restClientInnovatrics: {}", serviceBaseUrl);

        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, configProps.getServiceUserAgent());
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + configProps.getServiceToken());

        final RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig();
        restClientConfiguration.setBaseUrl(serviceBaseUrl);
        restClientConfiguration.setDefaultHttpHeaders(headers);
        return new DefaultRestClient(restClientConfiguration);
    }

}
