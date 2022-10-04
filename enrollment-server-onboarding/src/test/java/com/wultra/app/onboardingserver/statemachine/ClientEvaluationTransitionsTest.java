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
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.ClientEvaluationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.NOT_INITIALIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientEvaluationTransitionsTest extends AbstractStateMachineTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private IdentityVerificationService identityVerificationService;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private ClientEvaluationService clientEvaluationService;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @Test
    void testClientEvaluationAccepted() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.ACCEPTED, OnboardingState.CLIENT_EVALUATION_ACCEPTED);
    }

    @Test
    void testClientEvaluationInProgress() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.IN_PROGRESS, OnboardingState.CLIENT_EVALUATION_IN_PROGRESS);
    }

    @Test
    void testClientEvaluationFailed() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.FAILED, OnboardingState.CLIENT_EVALUATION_FAILED);
    }

    @Test
    void testClientEvaluationRejected() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.REJECTED, OnboardingState.CLIENT_EVALUATION_REJECTED);
    }

    @Test
    void testClientEvaluationAcceptedToPresenceCheckInit() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);
        when(identityVerificationService.moveToPhaseAndStatus(idVerification, PRESENCE_CHECK, NOT_INITIALIZED, OWNER_ID))
                .thenReturn(idVerification);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED)
                .and()
                .build()
                .test();
    }

    @Test
    void testDocumentVerificationTransitionToSendingOtp() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);
        doAnswer(args -> {
            idVerification.setPhase(IdentityVerificationPhase.OTP_VERIFICATION);
            idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
            return null;
        }).when(identityVerificationOtpService).sendOtp(idVerification, OWNER_ID);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
                .and()
                .build()
                .test();

        assertEquals(IdentityVerificationPhase.OTP_VERIFICATION, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.VERIFICATION_PENDING, idVerification.getStatus());
        verify(identityVerificationOtpService).sendOtp(idVerification, OWNER_ID);
    }

    @Test
    void testDocumentVerificationTransitionCompleted() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(false);
        doAnswer(args -> {
            IdentityVerificationEntity identityVerification = args.getArgument(0, StateContext.class)
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
            identityVerification.setPhase(IdentityVerificationPhase.COMPLETED);
            identityVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(verificationProcessResultAction).execute(any());

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.COMPLETED_ACCEPTED)
                .and()
                .build()
                .test();

        assertEquals(IdentityVerificationPhase.COMPLETED, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.ACCEPTED, idVerification.getStatus());
    }

    private void testClientVerificationStatus(IdentityVerificationStatus identityStatus, OnboardingState expectedState) throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.IN_PROGRESS);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        doAnswer(args -> {
            final IdentityVerificationEntity identityVerification = args.getArgument(0, IdentityVerificationEntity.class);
            identityVerification.setStatus(identityStatus);
            return null;
        }).when(clientEvaluationService).processClientEvaluation(idVerification, OWNER_ID);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(expectedState)
                .and()
                .build()
                .test();
    }
}
