/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationCreateService;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class InitialTransitionTest extends AbstractStateMachineTest {

    @Autowired
    private StateMachineService stateMachineService;

    @MockBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @MockBean
    private IdentityVerificationCreateService identityVerificationCreateService;

    @Test
    public void testInitialTransition() throws Exception {
        StateMachine<OnboardingState, OnboardingEvent> stateMachine =
                stateMachineService.prepareStateMachine(PROCESS_ID, OnboardingState.INITIAL, null);

        when(onboardingProcessRepository.findExistingProcessForActivationWithLock(ACTIVATION_ID, OnboardingStatus.VERIFICATION_IN_PROGRESS))
                .thenReturn(Optional.of(ONBOARDING_PROCESS_ENTITY));

        doAnswer(args ->
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS)
        ).when(identityVerificationCreateService).createIdentityVerification(OWNER_ID, PROCESS_ID);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.IDENTITY_VERIFICATION_INIT);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS)
                .and()
                .build()
                .test();
    }

}
