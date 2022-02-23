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
package com.wultra.app.presencecheck.iproov.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * iProov configuration.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.presence-check.provider", havingValue = "iproov")
@ComponentScan(basePackages = {"com.wultra.app.presencecheck"})
@Configuration
public class IProovConfig {

    /**
     * @return Object mapper bean specific to iProov json format
     */
    @Bean("objectMapperIproov")
    public ObjectMapper objectMapperIproov() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * Prepares REST client specific to iProov
     * @param configProps Configuration properties
     * @return REST client for iProov service API calls
     */
    @Bean("restClientIProov")
    public RestClient restClientIProov(IProovConfigProps configProps) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.HOST, configProps.getServiceHostname());
        headers.add(HttpHeaders.USER_AGENT, configProps.getServiceUserAgent());

        RestClientConfiguration restClientConfiguration = configProps.getRestClientConfig();
        restClientConfiguration.setBaseUrl(configProps.getServiceBaseUrl());
        restClientConfiguration.setDefaultHttpHeaders(headers);
        return new DefaultRestClient(restClientConfiguration);
    }

}
