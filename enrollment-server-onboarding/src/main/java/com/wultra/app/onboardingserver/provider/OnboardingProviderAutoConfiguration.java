/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for {@link OnboardingProvider} registering {@link EmptyOnboardingProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Configuration
@ConditionalOnMissingBean(OnboardingProvider.class)
class OnboardingProviderAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingProviderAutoConfiguration.class);

    @Bean
    OnboardingProvider onboardingProvider() {
        logger.warn("No OnboardingProvider found, registering default EmptyOnboardingProvider");
        return new EmptyOnboardingProvider();
    }
}