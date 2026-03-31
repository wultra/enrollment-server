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

package com.wultra.app.onboardingserver.statemachine.guard;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ConsentResolvedGuard}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ConsentResolvedGuardTest {

    private static final String PROCESS_ID = "9c3e1a7b-5f42-4d8e-9b6c-2a7f1d3e8c90";

    @Mock
    private OnboardingProcessRepository onboardingProcessRepository;

    @Mock
    private IdentityVerificationStatusService identityVerificationStatusService;

    @InjectMocks
    private ConsentResolvedGuard tested;

    @Test
    void testEvaluate_processNotFound_false() {
        // given
        // -

        // when
        final var result = tested.evaluate(PROCESS_ID, new OwnerId());

        // then
        assertFalse(result);
    }

    @Test
    void testEvaluate_exceptionForProcessConfig_false() {
        // given
        final var process = new OnboardingProcessEntity();

        when(onboardingProcessRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationStatusService.isConsentPending(process)).thenThrow(new IllegalArgumentException("test exception"));

        // when
        final var result = tested.evaluate(PROCESS_ID, new OwnerId());

        // then
        assertFalse(result);
    }

    @CsvSource({
            "true, false",
            "false, true"
    })
    @ParameterizedTest
    void testEvaluate(final boolean consentPending, final boolean expectedResult) {
        // given
        final var process = new OnboardingProcessEntity();

        when(onboardingProcessRepository.findById(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationStatusService.isConsentPending(process)).thenReturn(consentPending);

        // when
        final var result = tested.evaluate(PROCESS_ID, new OwnerId());

        // then
        assertEquals(expectedResult, result);
    }
}
