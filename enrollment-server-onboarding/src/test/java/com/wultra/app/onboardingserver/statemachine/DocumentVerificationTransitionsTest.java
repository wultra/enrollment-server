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
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DocumentVerificationTransitionsTest extends AbstractTransitionTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private IdentityVerificationService identityVerificationService;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @Test
    public void testDocumentVerificationAccepted() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.ACCEPTED, EnrollmentState.DOCUMENT_VERIFICATION_ACCEPTED);
    }

    @Test
    public void testDocumentVerificationInProgress() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.IN_PROGRESS, EnrollmentState.DOCUMENT_VERIFICATION_IN_PROGRESS);
    }

    @Test
    public void testDocumentVerificationRejected() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.REJECTED, EnrollmentState.DOCUMENT_VERIFICATION_REJECTED);
    }

    @Test
    public void testDocumentVerificationFailed() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.FAILED, EnrollmentState.DOCUMENT_VERIFICATION_FAILED);
    }

    @Test
    public void testDocumentVerificationTransitionToPresenceCheck() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.PRESENCE_CHECK_NOT_INITIALIZED)
                        .and()
                        .build();
        expected.test();
        assertEquals(IdentityVerificationPhase.PRESENCE_CHECK, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.NOT_INITIALIZED, idVerification.getStatus());
    }

    @Test
    public void testDocumentVerificationTransitionToSendingOtp() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.OTP_VERIFICATION_PENDING)
                        .and()
                        .build();
        expected.test();
        assertEquals(IdentityVerificationPhase.OTP_VERIFICATION, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.OTP_VERIFICATION_PENDING, idVerification.getStatus());
        verify(identityVerificationOtpService).sendOtp(idVerification);
    }

    @Test
    public void testDocumentVerificationTransitionCompleted() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(false);
        doAnswer(args -> {
            IdentityVerificationEntity identityVerification = ((StateContext<EnrollmentState, EnrollmentEvent>) args.getArgument(0))
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
            identityVerification.setPhase(IdentityVerificationPhase.COMPLETED);
            identityVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(verificationProcessResultAction).execute(any(StateContext.class));

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.COMPLETED_ACCEPTED)
                        .and()
                        .build();
        expected.test();
        assertEquals(IdentityVerificationPhase.COMPLETED, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.ACCEPTED, idVerification.getStatus());
    }

    private void testDocumentVerificationStatus(IdentityVerificationStatus identityStatus, EnrollmentState expectedState) throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.VERIFICATION_PENDING);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        doAnswer(args -> {
            idVerification.setStatus(identityStatus);
            return null;
        }).when(identityVerificationService).startVerification(eq(ownerId), eq(idVerification));

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(expectedState)
                        .and()
                        .build();
        expected.test();
    }

}
