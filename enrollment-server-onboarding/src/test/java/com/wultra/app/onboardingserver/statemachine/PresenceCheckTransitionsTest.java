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
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.PresenceCheckService;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.MoveToPresenceCheckVerificationPendingAction;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PresenceCheckTransitionsTest extends AbstractStateMachineTest {

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

    @MockBean
    private MoveToPresenceCheckVerificationPendingAction moveToPresenceCheckVerificationPendingAction;

    @Test
    void testPresenceCheckInit() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.NOT_INITIALIZED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(onboardingProcessRepository.findProcessByActivationId(idVerification.getActivationId()))
                .thenReturn(Optional.of(ONBOARDING_PROCESS_ENTITY));
        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);
        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
            return new SessionInfo();
        }).when(presenceCheckService).init(OWNER_ID, idVerification.getProcessId());

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.PRESENCE_CHECK_INIT);

        prepareTest(stateMachine)
            .sendEvent(message)
            .expectState(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
            .and()
            .build()
            .test();

        verify(presenceCheckService).init(OWNER_ID, idVerification.getProcessId());
    }

    @Test
    void testPresenceCheckInProgress() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setSessionInfo("{}");
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        PresenceCheckResult presenceCheckResult = new PresenceCheckResult();
        presenceCheckResult.setStatus(PresenceCheckStatus.IN_PROGRESS);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
            return null;
        }).when(presenceCheckService).checkPresenceVerification(eq(OWNER_ID), eq(idVerification), any(SessionInfo.class));

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
            .sendEvent(message)
            .expectState(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
            .and()
            .build()
            .test();
    }

    @Test
    void testPresenceCheckAcceptedOtpEnabled() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setSessionInfo("{}");
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        PresenceCheckResult presenceCheckResult = new PresenceCheckResult();
        presenceCheckResult.setStatus(PresenceCheckStatus.ACCEPTED);

        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
            return null;
        }).when(moveToPresenceCheckVerificationPendingAction).execute(any());

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(presenceCheckService).checkPresenceVerification(eq(OWNER_ID), eq(idVerification), any(SessionInfo.class));

        final Message<OnboardingEvent> presenceCheckSubmittedMessage =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.PRESENCE_CHECK_SUBMITTED);
        final Message<OnboardingEvent> nextEventMessage =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
            .sendEvent(presenceCheckSubmittedMessage)
            .sendEvent(nextEventMessage)
            .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
            .and()
            .build()
            .test();
        verify(identityVerificationOtpService).sendOtp(idVerification);
    }

    @Test
    void testPresenceCheckAcceptedOtpDisabled() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setSessionInfo("{}");
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        PresenceCheckResult presenceCheckResult = new PresenceCheckResult();
        presenceCheckResult.setStatus(PresenceCheckStatus.ACCEPTED);

        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(false);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
            return null;
        }).when(moveToPresenceCheckVerificationPendingAction).execute(any());

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(presenceCheckService).checkPresenceVerification(eq(OWNER_ID), eq(idVerification), any(SessionInfo.class));

        doAnswer(args -> {
            args.getArgument(0, StateContext.class)
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class)
                    .setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(verificationProcessResultAction).execute(any());

        final Message<OnboardingEvent> presenceCheckSubmittedMessage =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.PRESENCE_CHECK_SUBMITTED);
        final Message<OnboardingEvent> nextEventMessage =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
            .sendEvent(presenceCheckSubmittedMessage)
            .sendEvent(nextEventMessage)
            .expectState(OnboardingState.COMPLETED_ACCEPTED)
            .and()
            .build()
            .test();
    }

    private IdentityVerificationEntity createIdentityVerification(IdentityVerificationStatus status) {
        return super.createIdentityVerification(IdentityVerificationPhase.PRESENCE_CHECK, status);
    }

}
