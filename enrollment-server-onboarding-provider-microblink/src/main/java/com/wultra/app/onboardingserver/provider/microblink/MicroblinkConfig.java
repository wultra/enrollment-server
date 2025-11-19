/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.microblink;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponseParser;
import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import com.wultra.security.powerauth.client.v3.PowerAuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Configuration for Microblink specific beans.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "microblink")
@EnableConfigurationProperties(MicroblinkConfigProperties.class)
@Configuration
@Slf4j
class MicroblinkConfig {

    private MicroblinkConfigProperties properties;

    @Bean("microblinkRestClient")
    RestClient microblinkRestClient(MicroblinkConfigProperties properties) throws RestClientException {
        logger.info("Registering Microblink RestClient with base URL: {}", properties.getRestClientConfig().getBaseUrl());

        final var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        final var restClientConfig = properties.getRestClientConfig();
        restClientConfig.setDefaultHttpHeaders(httpHeaders);

        return new DefaultRestClient(restClientConfig);
    }

    @Bean("microblinkDocumentVerificationResponseParser")
    DocumentVerificationResponseParser documentVerificationResponseParser() {
        final var objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new DocumentVerificationResponseParser(objectMapper);
    }

    @Bean("microblinkCacheManager")
    public CacheManager cacheManager(final MicroblinkConfigProperties properties) {
        logger.info("Registering Microblink CacheManager");

        final var caffeineBuilder = Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheRecordTTL());

        final var cacheManager = new CaffeineCacheManager(
                MicroblinkConfigProperties.DOCUMENTS_CACHE_NAME,
                MicroblinkConfigProperties.PHOTO_CACHE_NAME
        );

        cacheManager.setCaffeine(caffeineBuilder);

        return cacheManager;
    }

    @Bean
    public MicroblinkDocumentVerificationProvider microblinkDocumentVerificationProvider(
            @Qualifier("microblinkCacheManager") CacheManager cacheManager,
            @Qualifier("microblinkRestClient") RestClient restClient,
            @Qualifier("microblinkDocumentVerificationResponseParser") DocumentVerificationResponseParser responseParser,
            MicroblinkConfigProperties properties,
            PowerAuthClient powerAuthClient
    ) {
        return new MicroblinkDocumentVerificationProvider(
                cacheManager,
                restClient,
                responseParser,
                properties.getMobileSdkLicenseKeys(),
                powerAuthClient
        );
    }
}
