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
package com.wultra.app.onboardingserver.docverify.zenid.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wultra.app.onboardingserver.docverify.zenid.model.deserializer.CustomOffsetDateTimeDeserializer;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * ZenID configuration.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "zenid")
@ComponentScan(basePackages = {"com.wultra.app.onboardingserver.docverify"})
@Configuration
public class ZenidConfig {

    public static final Map<SerializationFeature, Boolean> SERIALIZATION_FEATURES = Map.of(
            SerializationFeature.FAIL_ON_EMPTY_BEANS, false,
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false,
            SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true
    );

    public static final Map<DeserializationFeature, Boolean> DESERIALIZATION_FEATURES = Map.of(
            DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    );

    /**
     * @return Object mapper bean specific to ZenID json format
     */
    @Bean("objectMapperZenid")
    public ObjectMapper objectMapperZenid() {
        final JavaTimeModule javaTimeModule = createJavaTimeModule();

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule);
        DESERIALIZATION_FEATURES.forEach(mapper::configure);
        SERIALIZATION_FEATURES.forEach(mapper::configure);
        return mapper;
    }

    private static JavaTimeModule createJavaTimeModule() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // Add custom deserialization to support also the ISO DATE format data where ISO DATE TIME expected (ZenID bug?)
        javaTimeModule.addDeserializer(OffsetDateTime.class, new CustomOffsetDateTimeDeserializer());
        return javaTimeModule;
    }

    /**
     * Prepares REST client specific to ZenID
     * @param configProps Configuration properties
     * @return REST client for ZenID service API calls
     */
    @Bean("restClientZenid")
    public RestClient restClientZenid(ZenidConfigProps configProps) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, "api_key " + configProps.getApiKey());
        headers.add(HttpHeaders.USER_AGENT, configProps.getServiceUserAgent());

        final RestClientConfiguration.JacksonConfiguration jacksonConfiguration = new RestClientConfiguration.JacksonConfiguration();
        jacksonConfiguration.getSerialization().putAll(SERIALIZATION_FEATURES);
        jacksonConfiguration.getDeserialization().putAll(DESERIALIZATION_FEATURES);

        final RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig();
        restClientConfiguration.setBaseUrl(configProps.getServiceBaseUrl());
        restClientConfiguration.setDefaultHttpHeaders(headers);
        restClientConfiguration.setJacksonConfiguration(jacksonConfiguration);
        return new DefaultRestClient(restClientConfiguration, createJavaTimeModule());
    }

}
