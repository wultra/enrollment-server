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
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
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
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
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
class OtpTransitionsTest extends AbstractStateMachineTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @Test
    void testOtpResend() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification();
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(onboardingProcessRepository.findProcessByActivationId(idVerification.getActivationId()))
                .thenReturn(Optional.of(ONBOARDING_PROCESS_ENTITY));
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.OTP_VERIFICATION_RESEND);

        StateMachineTestPlan<OnboardingState, OnboardingEvent> expected =
                StateMachineTestPlanBuilder.<OnboardingState, OnboardingEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
                        .and()
                        .build();
        expected.test();

        verify(identityVerificationOtpService).resendOtp(idVerification);
    }

    @Test
    void testOtpVerified() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification();
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(idVerification.getProcessId())).thenReturn(true);
        doAnswer(args -> {
            args.getArgument(0, StateContext.class)
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class)
                    .setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(verificationProcessResultAction).execute(any());

        prepareTest(stateMachine)
                .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
                .and()
                .step()
                .sendEvent(OnboardingEvent.EVENT_NEXT_STATE)
                .expectState(OnboardingState.COMPLETED_ACCEPTED)
                .and()
                .build()
                .test();

        verify(verificationProcessResultAction).execute(any());
    }

    @Test
    void testOtpNotVerified() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification();
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(idVerification.getProcessId())).thenReturn(false);

        prepareTest(stateMachine)
                .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
                .and()
                .step()
                .sendEvent(OnboardingEvent.EVENT_NEXT_STATE)
                .expectState(OnboardingState.OTP_VERIFICATION_PENDING)
                .and()
                .build()
                .test();

        verify(verificationProcessResultAction, never()).execute(any());
    }

    private IdentityVerificationEntity createIdentityVerification() {
        return super.createIdentityVerification(
                IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.OTP_VERIFICATION_PENDING
        );
    }

}
