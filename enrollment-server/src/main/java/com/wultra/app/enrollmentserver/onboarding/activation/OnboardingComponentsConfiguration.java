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
import com.wultra.app.enrollmentserver.common.onboarding.configuration.OnboardingConfig;
import com.wultra.app.enrollmentserver.common.onboarding.database.OnboardingOtpRepository;
import com.wultra.app.enrollmentserver.common.onboarding.database.OnboardingProcessRepository;
import com.wultra.app.enrollmentserver.common.onboarding.impl.service.CommonOnboardingService;
import com.wultra.app.enrollmentserver.common.onboarding.impl.service.CommonOtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of common components.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Configuration
@ConditionalOnProperty(name = "enrollment-server.onboarding.enabled", havingValue = "true")
@Slf4j
public class OnboardingComponentsConfiguration {

    /**
     * No-arg constructor.
     */
    public OnboardingComponentsConfiguration() {
        log.info("Onboarding feature turned on, configuring enrollment components specific for onboarding.");
    }

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
     * @param onboardingConfig onboarding config
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

    /**
     * Register activation otp service bean.
     *
     * @param otpService otp service
     * @return activation otp service bean
     */
    @Bean
    public ActivationOtpService activationOtpService(final OtpService otpService) {
        return new ActivationOtpService(otpService);
    }

    /**
     * Register activation process service bean.
     *
     * @param onboardingService onboading service
     * @return activation process service bean
     */
    @Bean
    public ActivationProcessService activationProcessService(final OnboardingService onboardingService) {
        return new ActivationProcessService(onboardingService);
    }
}
