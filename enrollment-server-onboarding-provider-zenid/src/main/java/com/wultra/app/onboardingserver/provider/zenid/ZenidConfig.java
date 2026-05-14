/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.zenid;

import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.OffsetDateTime;

/**
 * ZenID configuration.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "zenid")
@ComponentScan("com.wultra.app.onboardingserver.provider.zenid")
@Configuration
@Slf4j
class ZenidConfig {

    /**
     * @param configProps Configuration properties
     * @return Object mapper bean specific to ZenID json format
     */
    @Bean("objectMapperZenid")
    public ObjectMapper objectMapperZenid(final ZenidConfigProps configProps) {
        final RestClientConfiguration restClientConfig = configProps.getRestClientConfig();
        final RestClientConfiguration.JacksonConfiguration jacksonConfiguration =
                restClientConfig != null ? restClientConfig.getJacksonConfiguration() : null;

        final JsonMapper.Builder builder = JsonMapper.builder()
                .addModule(createJavaTimeModule())
                .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        if (jacksonConfiguration != null) {
            jacksonConfiguration.getDeserialization().forEach(builder::configure);
            jacksonConfiguration.getSerialization().forEach(builder::configure);
        }
        return builder.build();
    }

    private static SimpleModule createJavaTimeModule() {
        SimpleModule timeModule = new SimpleModule();
        // Add custom deserialization to support also the ISO DATE format data where ISO DATE TIME expected (ZenID bug?)
        timeModule.addDeserializer(
                OffsetDateTime.class,
                new CustomOffsetDateTimeDeserializer()
        );
        return timeModule;
    }

    /**
     * Prepares REST client specific to ZenID
     * @param configProps Configuration properties
     * @return REST client for ZenID service API calls
     */
    @Bean("restClientZenid")
    public RestClient restClientZenid(final ZenidConfigProps configProps) throws RestClientException {
        final String serviceBaseUrl = configProps.getServiceBaseUrl();
        logger.info("Registering restClientZenid: {}", serviceBaseUrl);

        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, "api_key " + configProps.getApiKey());
        headers.add(HttpHeaders.USER_AGENT, configProps.getServiceUserAgent());

        final RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig() != null
                ? configProps.getRestClientConfig()
                : new RestClientConfiguration();
        restClientConfiguration.setBaseUrl(serviceBaseUrl);
        restClientConfiguration.setDefaultHttpHeaders(headers);
        return new DefaultRestClient(restClientConfiguration, createJavaTimeModule());
    }

}
