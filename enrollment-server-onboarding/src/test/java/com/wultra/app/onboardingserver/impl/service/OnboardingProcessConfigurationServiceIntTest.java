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

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OnboardingProcessConfigurationService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class OnboardingProcessConfigurationServiceIntTest {

    private static final String PROCESS_ID_WITHOUT_CONFIGURATION = "6f1c9e4a-8b7d-4c3f-9a21-2e7c5f4b1d93";
    private static final String PROCESS_ID_WITH_CONFIGURATION = "c2a8f7d1-3e6b-4f5a-9d84-1b0e6c9a2f47";

    @Autowired
    private OnboardingProcessConfigurationService tested;

    @Test
    void testFindConfigByProcessId_recordIsNotFound_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.findConfigByProcessId("invalid-process-id"));

        // then
        assertEquals("Onboarding process configuration not found for process id: invalid-process-id", exception.getMessage());
    }

    @Test
    void testFindConfigByProcessId_processRecordExistsWithoutConfiguration_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.findConfigByProcessId(PROCESS_ID_WITHOUT_CONFIGURATION));

        // then
        assertEquals("Onboarding process configuration not found for process id: " + PROCESS_ID_WITHOUT_CONFIGURATION, exception.getMessage());
    }

    @Test
    void testFindConfigByProcessId_processRecordExistsWithConfiguration_recordIsReturned() {
        // given
        // -

        // when
        final var processConfig = tested.findConfigByProcessId(PROCESS_ID_WITH_CONFIGURATION);

        // then
        assertNotNull(processConfig);
    }
}
