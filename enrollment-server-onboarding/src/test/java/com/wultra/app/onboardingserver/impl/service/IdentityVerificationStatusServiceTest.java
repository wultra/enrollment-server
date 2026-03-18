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

package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link IdentityVerificationStatusService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationStatusServiceTest {

    @InjectMocks
    private IdentityVerificationStatusService tested;

    @Test
    void testIsConsentPending_processConfigNotFound_exceptionIsThrown() {
        // given
        final var process = new OnboardingProcessEntity();
        process.setId("0c4d8e71-2f6b-4a99-b3d5-7e1f2c8a6b55");

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.isConsentPending(process));

        // then
        assertEquals("Configuration not found for process ID: 0c4d8e71-2f6b-4a99-b3d5-7e1f2c8a6b55", exception.getMessage());
    }

    @CsvSource({
            "true,,false",
            "true,true,false",
            "true,false,true",
            "false,,false",
            "false,true,false",
            "false,false,false"
    })
    @ParameterizedTest
    void testIsConsentPending(final boolean consentRequired, final Boolean consentAccepted, final boolean expectedResult) {
        // given
        final var processConfigValue = OnboardingProcessConfigurationValue.builder()
                .consentRequired(consentRequired)
                .build();

        final var processConfig = new OnboardingProcessConfigurationEntity();
        processConfig.setConfiguration(processConfigValue);

        final var process = new OnboardingProcessEntity();
        process.setId("0c4d8e71-2f6b-4a99-b3d5-7e1f2c8a6b55");
        process.setProcessConfiguration(processConfig);
        process.setConsentAccepted(consentAccepted);

        // when
        final var result = tested.isConsentPending(process);

        // then
        assertEquals(expectedResult, result);
    }
}
