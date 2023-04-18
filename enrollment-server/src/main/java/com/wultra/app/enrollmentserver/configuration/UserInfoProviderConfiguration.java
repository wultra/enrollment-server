/*
 * PowerAuth Server and related software components
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
package com.wultra.app.enrollmentserver.configuration;

import com.wultra.app.enrollmentserver.impl.provider.userinfo.RestUserInfoProvider;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import io.getlime.security.powerauth.rest.api.model.entity.UserInfoStage;
import io.getlime.security.powerauth.rest.api.spring.provider.MinimalClaimsUserInfoProvider;
import io.getlime.security.powerauth.rest.api.spring.provider.UserInfoProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Set;

import static io.getlime.security.powerauth.rest.api.model.entity.UserInfoStage.USER_INFO_ENDPOINT;

/**
 * Configuration of PowerAuth Restful Integration.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Configuration
@EnableConfigurationProperties
@Slf4j
public class UserInfoProviderConfiguration {

    /**
     * Prepare minimal user-info provider.
     *
     * @return user-info provider.
     */
    @Bean
    @ConditionalOnProperty(value = "enrollment-server.user-info.provider", havingValue = "MINIMAL")
    public UserInfoProvider minimalUserInfoProvider() {
        logger.info("Registering MinimalClaimsUserInfoProvider");
        return new MinimalClaimsUserInfoProvider();
    }

    /**
     * Prepare REST user-info provider.
     *
     * @return user-info provider.
     */
    @Bean
    @ConditionalOnProperty(value = "enrollment-server.user-info.provider", havingValue = "REST")
    public UserInfoProvider restUserInfoProvider(RestUserInfoProviderConfiguration restUserInfoProviderConfiguration) throws RestClientException {
        logger.info("Registering RestUserInfoProvider");
        return new RestUserInfoProvider(restUserInfoProviderConfiguration.restClientConfig, restUserInfoProviderConfiguration.getAllowedStages());
    }

    @ConfigurationProperties(prefix = "enrollment-server.user-info.rest-provider", ignoreInvalidFields = true)
    @Component
    @Getter
    @Setter
    public static class RestUserInfoProviderConfiguration {
        private RestClientConfiguration restClientConfig = new RestClientConfiguration();
        private Set<UserInfoStage> allowedStages = Set.of(USER_INFO_ENDPOINT);
    }
}
