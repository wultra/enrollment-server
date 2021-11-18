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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.function.Supplier;

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
     * Definition of HTTP client with NTLM authentication support
     *
     * @param configProps Configuration properties
     * @return Instance of an HTTP client with NTLM authentication support
     */
    public CloseableHttpClient httpClient(ZenidConfigProps configProps) {
        String user = configProps.getNtlmUsername();
        String password = configProps.getNtlmPassword();
        String domain = configProps.getNtlmDomain();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new NTCredentials(user, password, null, domain));

        RequestConfig requestConfig = RequestConfig.custom()
                .setAuthenticationEnabled(true)
                .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.NTLM))
                .setTargetPreferredAuthSchemes(Collections.singletonList(AuthSchemes.NTLM))
                .build();

        return HttpClientBuilder
                .create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * @return Object mapper bean specific to ZenID json format
     */
    @Bean("objectMapperZenid")
    public ObjectMapper objectMapperZenid() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // Add custom deserialization to support also the ISO DATE format data where ISO DATE TIME expected (ZenID bug?)
        javaTimeModule.addDeserializer(OffsetDateTime.class, new CustomOffsetDateTimeDeserializer());
        mapper.registerModule(javaTimeModule)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        return mapper;
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(@Qualifier("objectMapperZenid") ObjectMapper mapper) {
        return new MappingJackson2HttpMessageConverter(mapper);
    }

    /**
     * Prepares REST template specific to ZenID
     * @param configProps Configuration properties
     * @param builder REST template builder
     * @return REST template for ZenID service API calls
     */
    @Bean("restTemplateZenid")
    public RestTemplate restTemplateZenid(
            ZenidConfigProps configProps,
            RestTemplateBuilder builder) {
        ZenidRequestFactorySupplier supplier = new ZenidRequestFactorySupplier(configProps);

        return builder
                .requestFactory(supplier)
                .rootUri(configProps.getServiceBaseUrl())
                .build();
    }

    /**
     * Supplier of the HTTP client request factory based on HTTP client with NTLM authentication
     */
    class ZenidRequestFactorySupplier implements Supplier<ClientHttpRequestFactory> {

        private final ZenidConfigProps configProps;

        public ZenidRequestFactorySupplier(ZenidConfigProps configProps) {
            this.configProps = configProps;
        }

        @Override
        public ClientHttpRequestFactory get() {
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                    httpClient(configProps)
            );
            // Prevent usage of streamed request which is not repeatable and
            // cannot be used in a standard not-preemptive NTLM auth
            requestFactory.setBufferRequestBody(true);
            return requestFactory;
        }

    }

}
