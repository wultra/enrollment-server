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
import org.springframework.util.Assert;
import tools.jackson.databind.ObjectMapper;
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
        final SimpleModule timeModule = createJavaTimeModule();
        final JsonMapper.Builder builder = JsonMapper.builder()
                .addModule(timeModule);
        final RestClientConfiguration.JacksonConfiguration jacksonConfiguration =
                configProps.getRestClientConfig().getJacksonConfiguration();

        Assert.state(
                jacksonConfiguration != null,
                "Jackson configuration is expected for ZenID working properly"
        );

        final ObjectMapper mapper = builder.build();
        jacksonConfiguration.getDeserialization()
                .forEach((feature, state) -> {
                    if (state) {
                        builder.enable(feature);
                    } else {
                        builder.disable(feature);
                    }
                });
        jacksonConfiguration.getSerialization()
                .forEach((feature, state) -> {
                    if (state) {
                        builder.enable(feature);
                    } else {
                        builder.disable(feature);
                    }
                });
        return mapper;
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

        final RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig();
        restClientConfiguration.setBaseUrl(serviceBaseUrl);
        restClientConfiguration.setDefaultHttpHeaders(headers);
        return new DefaultRestClient(restClientConfiguration, createJavaTimeModule());
    }

}
