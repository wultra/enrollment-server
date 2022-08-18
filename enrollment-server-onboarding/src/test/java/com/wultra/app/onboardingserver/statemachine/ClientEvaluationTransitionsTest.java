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
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClientEvaluationTransitionsTest extends AbstractStateMachineTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @Test
    public void testClientEvaluationAccepted() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.ACCEPTED, OnboardingState.CLIENT_EVALUATION_ACCEPTED);
    }

    @Test
    public void testClientEvaluationInProgress() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.IN_PROGRESS, OnboardingState.CLIENT_EVALUATION_IN_PROGRESS);
    }

    @Test
    public void testClientEvaluationFailed() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.FAILED, OnboardingState.CLIENT_EVALUATION_FAILED);
    }

    @Test
    public void testClientEvaluationRejected() throws Exception {
        testClientVerificationStatus(IdentityVerificationStatus.REJECTED, OnboardingState.CLIENT_EVALUATION_REJECTED);
    }

    @Test
    public void testClientEvaluationAcceptedToPresenceCheckInit() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED)
                .and()
                .build()
                .test();

        assertEquals(IdentityVerificationPhase.PRESENCE_CHECK, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.NOT_INITIALIZED, idVerification.getStatus());
    }

    @Test
    public void testDocumentVerificationTransitionToSendingOtp() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);
        doAnswer(args -> {
            idVerification.setPhase(IdentityVerificationPhase.OTP_VERIFICATION);
            idVerification.setStatus(IdentityVerificationStatus.OTP_VERIFICATION_PENDING);
            return null;
        }).when(identityVerificationOtpService).sendOtp(idVerification);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
                .and()
                .build()
                .test();

        assertEquals(IdentityVerificationPhase.OTP_VERIFICATION, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.OTP_VERIFICATION_PENDING, idVerification.getStatus());
        verify(identityVerificationOtpService).sendOtp(idVerification);
    }

    @Test
    public void testDocumentVerificationTransitionCompleted() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(false);
        doAnswer(args -> {
            IdentityVerificationEntity identityVerification = ((StateContext<OnboardingState, OnboardingEvent>) args.getArgument(0))
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
            identityVerification.setPhase(IdentityVerificationPhase.COMPLETED);
            identityVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(verificationProcessResultAction).execute(any(StateContext.class));

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

        Assertions.assertTimeout(Duration.ofMillis(500), () ->
            idVerification.setStatus(identityStatus)
        );

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
