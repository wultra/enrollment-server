/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationTargetActivationService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationResultService;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * Test for transitions in {@link OnboardingState#ACTIVATION_FINISH_IN_PROGRESS} state.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Transactional
class ActivationFinishTransitionsTest extends AbstractStateMachineTest {

    @MockitoBean
    private IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    @MockitoBean
    private VerificationResultService verificationResultService;

    @Test
    void testActivationFinishInProgress_completed() throws Exception {
        final IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationPhase.ACTIVATION_FINISH, IdentityVerificationStatus.IN_PROGRESS);

        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.OK);
        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID))
                .thenReturn(true);

        final Message<OnboardingEvent> nextEventMessage =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(nextEventMessage)
                .expectState(OnboardingState.COMPLETED_ACCEPTED)
                .and()
                .build()
                .test();
    }
}
