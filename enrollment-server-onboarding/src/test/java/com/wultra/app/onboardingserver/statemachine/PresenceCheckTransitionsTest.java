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
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.PresenceCheckService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PresenceCheckTransitionsTest extends AbstractTransitionTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @MockBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private PresenceCheckService presenceCheckService;

    @Test
    public void testPresenceCheckInit() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.NOT_INITIALIZED);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OnboardingProcessEntity onboardingProcessEntity = createOnboardingProcessEntity();
        OwnerId ownerId = createOwnerId();

        when(onboardingProcessRepository.findProcessByActivationId(idVerification.getActivationId()))
                .thenReturn(Optional.of(onboardingProcessEntity));
        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);
        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
            return new SessionInfo();
        }).when(presenceCheckService).init(ownerId, idVerification.getProcessId());

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.PRESENCE_CHECK_INIT);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                        .and()
                        .build();
        expected.test();

        verify(presenceCheckService).init(ownerId, idVerification.getProcessId());
    }

    @Test
    public void testPresenceCheckInProgress() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setSessionInfo("{}");
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        PresenceCheckResult presenceCheckResult = new PresenceCheckResult();
        presenceCheckResult.setStatus(PresenceCheckStatus.IN_PROGRESS);

        when(presenceCheckService.checkPresenceVerification(eq(ownerId), eq(idVerification), any(SessionInfo.class)))
                .thenReturn(presenceCheckResult);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
            return null;
        }).when(presenceCheckService).evaluatePresenceCheckResult(ownerId, idVerification, presenceCheckResult);

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                        .and()
                        .build();
        expected.test();
    }

    @Test
    public void testPresenceCheckAcceptedOtpEnabled() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setSessionInfo("{}");
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        PresenceCheckResult presenceCheckResult = new PresenceCheckResult();
        presenceCheckResult.setStatus(PresenceCheckStatus.ACCEPTED);

        when(presenceCheckService.checkPresenceVerification(eq(ownerId), eq(idVerification), any(SessionInfo.class)))
                .thenReturn(presenceCheckResult);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(presenceCheckService).evaluatePresenceCheckResult(ownerId, idVerification, presenceCheckResult);

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
        verify(identityVerificationOtpService).sendOtp(idVerification);
    }

    @Test
    public void testPresenceCheckAcceptedOtpDisabled() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setSessionInfo("{}");
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId();

        PresenceCheckResult presenceCheckResult = new PresenceCheckResult();
        presenceCheckResult.setStatus(PresenceCheckStatus.ACCEPTED);

        when(presenceCheckService.checkPresenceVerification(eq(ownerId), eq(idVerification), any(SessionInfo.class)))
                .thenReturn(presenceCheckResult);
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(false);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(presenceCheckService).evaluatePresenceCheckResult(ownerId, idVerification, presenceCheckResult);

        doAnswer(args -> {
            ((StateContext<EnrollmentState, EnrollmentEvent>) args.getArgument(0))
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class)
                    .setStatus(IdentityVerificationStatus.ACCEPTED);
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
    }

    private IdentityVerificationEntity createIdentityVerification(IdentityVerificationStatus status) {
        return super.createIdentityVerification(IdentityVerificationPhase.PRESENCE_CHECK, status);
    }

}
