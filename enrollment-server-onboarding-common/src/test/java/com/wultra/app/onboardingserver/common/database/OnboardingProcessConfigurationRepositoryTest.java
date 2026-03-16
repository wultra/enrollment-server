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
package com.wultra.app.onboardingserver.common.database;

import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link OnboardingProcessConfigurationRepository}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql
class OnboardingProcessConfigurationRepositoryTest {

    @Autowired
    private OnboardingProcessConfigurationRepository tested;

    @Test
    void testJsonMapping() {
        final OnboardingProcessConfigurationEntity entity = tested.findByProcessType("reactivation").orElseThrow(AssertionFailedError::new);
        final OnboardingProcessConfigurationValue result = entity.getConfiguration();
        final var expectedConfiguration = buildExpectedConfiguration();

        assertEquals(expectedConfiguration, result);
    }

    @Test
    void testJsonMapping_defaultValues() {
        final OnboardingProcessConfigurationEntity entity = tested.findByProcessType("onboarding").orElseThrow(AssertionFailedError::new);
        final OnboardingProcessConfigurationValue result = entity.getConfiguration();
        final var expectedConfiguration = buildExpectedEmptyConfiguration();

        assertEquals(expectedConfiguration, result);
    }

    private static OnboardingProcessConfigurationValue buildExpectedConfiguration() {
        return OnboardingProcessConfigurationValue.builder()
                .enabled(true)
                .activationType(OnboardingProcessConfigurationValue.ActivationType.CODE)
                .otpForIdentification(true)
                .otpForIdentityVerification(true)
                .useTemporaryActivation(true)
                .approvalEnabled(true)
                .verifyPresenceWithOtp(false)
                .documents(
                        OnboardingProcessConfigurationValue.Documents.builder()
                                .totalRequiredDocumentsCount((byte) 2)
                                .groups(
                                        Set.of(
                                                OnboardingProcessConfigurationValue.Group.builder()
                                                        .requiredDocumentsCount((byte) 1)
                                                        .items(Set.of(
                                                                OnboardingProcessConfigurationValue.Document.builder()
                                                                        .type(OnboardingProcessConfigurationValue.DocumentType.ID_CARD)
                                                                        .sideCount((byte) 2)
                                                                        .build(),
                                                                OnboardingProcessConfigurationValue.Document.builder()
                                                                        .type(OnboardingProcessConfigurationValue.DocumentType.PASSPORT)
                                                                        .sideCount((byte) 1)
                                                                        .build()
                                                        ))
                                                        .build(),
                                                OnboardingProcessConfigurationValue.Group.builder()
                                                        .items(Set.of(
                                                                OnboardingProcessConfigurationValue.Document.builder()
                                                                        .type(OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE)
                                                                        .sideCount((byte) 1)
                                                                        .build()
                                                        ))
                                                        .build()
                                        ))
                                .build()
                )
                .build();
    }

    private static OnboardingProcessConfigurationValue buildExpectedEmptyConfiguration() {
        return OnboardingProcessConfigurationValue.builder()
                .enabled(false)
                .activationType(OnboardingProcessConfigurationValue.ActivationType.IDENTITY)
                .otpForIdentification(false)
                .otpForIdentityVerification(false)
                .verifyPresenceWithOtp(true)
                .documents(
                        OnboardingProcessConfigurationValue.Documents.builder()
                                .totalRequiredDocumentsCount((byte) 0)
                                .groups(Set.of())
                                .build()
                )
                .build();
    }
}
