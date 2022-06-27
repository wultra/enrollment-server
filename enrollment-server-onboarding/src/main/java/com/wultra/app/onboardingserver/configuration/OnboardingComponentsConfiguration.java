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
package com.wultra.app.onboardingserver.configuration;

import com.wultra.app.enrollmentserver.common.onboarding.configuration.OnboardingConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration of common components.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@EnableJpaRepositories(basePackages = {
        "com.wultra.app.enrollmentserver.common.onboarding.database", // dependencies from common
        "com.wultra.app.onboardingserver.database" // not to override component scan for enrollment-server-onboarding
})
@EntityScan(basePackages = {
        "com.wultra.app.enrollmentserver.common.onboarding.database.entity", // dependencies from common
        "com.wultra.app.onboardingserver.database.entity"  // not to override component scan for enrollment-server-onboarding
})
@Configuration
public class OnboardingComponentsConfiguration {

    /**
     * Register onboarding config bean.
     *
     * @return onboarding config bean
     */
    @Bean
    public OnboardingConfig onboardingConfig() {
        return new OnboardingConfig();
    }
}
