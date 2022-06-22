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
package com.wultra.app.enrollmentserver.onboarding.activation;

import com.wultra.app.enrollmentserver.common.onboarding.api.OnboardingService;
import com.wultra.app.enrollmentserver.common.onboarding.api.OtpService;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.impl.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.impl.service.CommonOtpService;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration of components placed in {@code enrollment-server-onboarding} needed for {@code enrollment-server}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@EnableJpaRepositories(basePackages = {
        "com.wultra.app.enrollmentserver.database", // not to override component scan for enrollment-server
        "com.wultra.app.onboardingserver.database" // dependencies from enrollment-server-onboarding
})
@EntityScan(basePackages = {
        "com.wultra.app.enrollmentserver.database.entity", // not to override component scan for enrollment-server
        "com.wultra.app.onboardingserver.database.entity" // dependencies from enrollment-server-onboarding
})
@Configuration
// TODO (racansky, 2022-06-21) improve isolation, enrollment includes all classes from onboarding
public class OnboardingComponentsConfiguration {

    /**
     * Register onboarding service bean.
     *
     * @param onboardingProcessRepository onboarding process repository
     * @return onboarding service bean
     */
    @Bean
    public OnboardingService onboardingService(final OnboardingProcessRepository onboardingProcessRepository) {
        return new CommonOnboardingService(onboardingProcessRepository);
    }

    /**
     * Register otp service bean.
     *
     * @param onboardingOtpRepository onboarding otp repository
     * @param onboardingProcessRepository onboarding process repository
     * @param onboardingConfig  onboarding config
     * @return otp service bean
     */
    @Bean
    public OtpService otpService(
            final OnboardingOtpRepository onboardingOtpRepository,
            final OnboardingProcessRepository onboardingProcessRepository,
            final OnboardingConfig onboardingConfig) {

        return new CommonOtpService(onboardingOtpRepository, onboardingProcessRepository, onboardingConfig);
    }

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
