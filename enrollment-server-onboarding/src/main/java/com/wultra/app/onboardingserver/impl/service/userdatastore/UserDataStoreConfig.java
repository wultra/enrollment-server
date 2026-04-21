/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.impl.service.userdatastore;

import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.security.userdatastore.UserDataStoreRestClient;
import com.wultra.security.userdatastore.client.UserDataStoreClient;
import com.wultra.security.userdatastore.client.model.error.UserDataStoreClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for User Data Store integration.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ConditionalOnProperty(name = "enrollment-server-onboarding.user-data-store.enabled", havingValue = "true")
@Configuration
@EnableConfigurationProperties(UserDataStoreConfigurationProperties.class)
@Slf4j
class UserDataStoreConfig {

    @Bean
    public UserDataStoreClient userDataStoreClient(final UserDataStoreConfigurationProperties config) throws UserDataStoreClientException {
        final var clientConfig = config.getRestClientConfig();

        logger.info("Registering UserDataStore client with url: {}", clientConfig.getBaseUrl());
        return new UserDataStoreRestClient(clientConfig);
    }

    @Bean
    public UserDataStoreService userDataStoreService(
            final UserDataStoreClient client,
            final UserDataStoreConfigurationProperties config,
            final OnboardingProcessRepository onboardingProcessRepository,
            final IdentityVerificationRepository identityVerificationRepository,
            final ProcessedDocumentDataRepository processedDocumentDataRepository) {

        logger.info("Initializing DefaultUserDataStoreService.");
        return new DefaultUserDataStoreService(client, config, onboardingProcessRepository, identityVerificationRepository, processedDocumentDataRepository);
    }
}