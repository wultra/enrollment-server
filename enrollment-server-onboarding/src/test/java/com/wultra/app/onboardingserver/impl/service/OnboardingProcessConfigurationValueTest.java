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

package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link OnboardingProcessConfigurationService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class OnboardingProcessConfigurationValueTest {

    @Mock
    private OnboardingProcessRepository onboardingProcessRepository;

    @InjectMocks
    private OnboardingProcessConfigurationService tested;

    @Test
    void testFindConfigByProcessId_processDoesNotExist_exceptionIsThrown() {
        // given
        when(onboardingProcessRepository.findById("1")).thenReturn(Optional.empty());

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.findConfigByProcessId("1"));

        // then
        assertEquals("Onboarding process configuration not found for process id: 1", exception.getMessage());
    }

    @Test
    void testFindConfigByProcessId_processConfigurationDoesNotExist_exceptionIsThrown() {
        // given
        when(onboardingProcessRepository.findById("1")).thenReturn(Optional.of(new OnboardingProcessEntity()));

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.findConfigByProcessId("1"));

        // then
        assertEquals("Onboarding process configuration not found for process id: 1", exception.getMessage());
    }

    @Test
    void testFindConfigByProcessId_processConfigurationExists_configurationIsReturned() {
        // given
        final var processConfigEntity = new OnboardingProcessConfigurationEntity();
        processConfigEntity.setConfiguration(OnboardingProcessConfigurationValue.builder().build());

        final var onboardingProcessEntity = new OnboardingProcessEntity();
        onboardingProcessEntity.setProcessConfiguration(processConfigEntity);

        when(onboardingProcessRepository.findById("1")).thenReturn(Optional.of(onboardingProcessEntity));

        // when
        final var result = tested.findConfigByProcessId("1");

        // then
        assertNotNull(result);
    }
}
