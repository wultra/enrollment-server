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
package com.wultra.app.docverify.zenid.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wultra.app.docverify.zenid.model.deserializer.CustomOffsetDateTimeDeserializer;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.OffsetDateTime;

/**
 * ZenID configuration.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.document-verification.provider", havingValue = "zenid")
@ComponentScan(basePackages = {"com.wultra.app.docverify"})
@Configuration
public class ZenidConfig {

    /**
     * @return Object mapper bean specific to ZenID json format
     */
    @Bean("objectMapperZenid")
    public ObjectMapper objectMapperZenid() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // Add custom deserialization to support also the ISO DATE format data where ISO DATE TIME expected (ZenID bug?)
        javaTimeModule.addDeserializer(OffsetDateTime.class, new CustomOffsetDateTimeDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(javaTimeModule)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        return mapper;
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

        RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig();
        restClientConfiguration.setBaseUrl(configProps.getServiceBaseUrl());
        restClientConfiguration.setDefaultHttpHeaders(headers);
        restClientConfiguration.setObjectMapper(objectMapperZenid());
        return new DefaultRestClient(restClientConfiguration);
    }

}
