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
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@SpringBootTest(classes = { EnrollmentServerTestApplication.class })
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OtpVerificationTest {

    @Autowired
    private EnrollmentStateProvider enrollmentStateProvider;

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @Autowired
    private StateMachineService stateMachineService;

    @Test
    public void test() throws Exception {
        // TODO verification pending when OTP not verified
    }

    @Test
    public void testResendOtp() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification();
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OnboardingProcessEntity onboardingProcessEntity = new OnboardingProcessEntity();
        onboardingProcessEntity.setId(idVerification.getProcessId());

        when(onboardingProcessRepository.findProcessByActivationId(idVerification.getActivationId()))
                .thenReturn(Optional.of(onboardingProcessEntity));
        when(identityVerificationConfig.isVerificationOtpEnabled()).thenReturn(true);

        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(idVerification.getActivationId());
        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.OTP_VERIFICATION_RESEND);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.OTP_VERIFICATION_PENDING)
                        .and()
                        .build();
        expected.test();

        verify(identityVerificationOtpService).resendOtp(idVerification);
    }

    @Test
    public void testVerification() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification();
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(idVerification.getProcessId())).thenReturn(true);
        doAnswer(args -> {
            ((StateContext<EnrollmentState, EnrollmentEvent>) args.getArgument(0))
                    .getExtendedState()
                    .get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class)
                    .setStatus(IdentityVerificationStatus.ACCEPTED);
            return null;
        }).when(verificationProcessResultAction).execute(any(StateContext.class));

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                            .stateMachine(stateMachine)
                            .step()
                            .expectState(EnrollmentState.OTP_VERIFICATION_PENDING)
                        .and()
                            .step()
                            .sendEvent(EnrollmentEvent.EVENT_NEXT_STATE)
                            .expectState(EnrollmentState.COMPLETED_ACCEPTED)
                        .and()
                        .build();
        expected.test();
    }

    private StateMachine<EnrollmentState, EnrollmentEvent> createStateMachine(IdentityVerificationEntity entity) throws Exception {
        EnrollmentState state = enrollmentStateProvider.findByPhaseAndStatus(entity.getPhase(), entity.getStatus());
        return stateMachineService.prepareStateMachine(entity.getProcessId(), state, entity);
    }

    private IdentityVerificationEntity createIdentityVerification() {
        String activationId = "activationId";
        String processId = "processId";

        IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(activationId);
        entity.setProcessId(processId);
        entity.setPhase(IdentityVerificationPhase.OTP_VERIFICATION);
        entity.setStatus(IdentityVerificationStatus.OTP_VERIFICATION_PENDING);

        return entity;
    }

}
