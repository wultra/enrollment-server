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
package com.wultra.app.onboardingserver.task.cleaning;

import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for configuration of {@link PersonalDataCleaningTask}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class PersonalDataCleaningTaskTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("spring.config.location=classpath:application.properties")
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(IdentityVerificationConfig.class, PersonalDataCleaningTask.class)
            .withBean(CleaningService.class, () -> mock(CleaningService.class));

    @Test
    void testDefaultRetentionEnablesCleanup() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PersonalDataCleaningTask.class);
            assertThat(context.getBean(IdentityVerificationConfig.class).getDataRetentionTime())
                    .contains(Duration.ofHours(1));
        });
    }

    @Test
    void testExplicitRetentionOverridesDefault() {
        contextRunner.withPropertyValues("enrollment-server-onboarding.identity-verification.data-retention=2h")
                .run(context -> {
                    assertThat(context).hasSingleBean(PersonalDataCleaningTask.class);
                    assertThat(context.getBean(IdentityVerificationConfig.class).getDataRetentionTime())
                            .contains(Duration.ofHours(2));
                });
    }

    @Test
    void testEmptyRetentionDisablesCleanup() {
        contextRunner.withPropertyValues("enrollment-server-onboarding.identity-verification.data-retention=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(PersonalDataCleaningTask.class);
                    assertThat(context.getBean(IdentityVerificationConfig.class).getDataRetentionTime())
                            .isEmpty();
                });
    }
}
