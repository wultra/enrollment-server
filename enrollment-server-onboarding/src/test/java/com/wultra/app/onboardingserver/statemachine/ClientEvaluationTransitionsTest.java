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
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.ClientEvaluationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.guard.ClientEvaluationEnabledGuard;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.NOT_INITIALIZED;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Transactional
class ClientEvaluationTransitionsTest extends AbstractStateMachineTest {

    @MockitoBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockitoBean
    private IdentityVerificationService identityVerificationService;

    @MockitoBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockitoBean
    private ClientEvaluationService clientEvaluationService;

    @MockitoBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @MockitoBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @MockitoBean
    private ClientEvaluationEnabledGuard clientEvaluationEnabledGuard;

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
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.ACCEPTED);
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
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
        doAnswer(args -> {
            idVerification.setPhase(IdentityVerificationPhase.OTP_VERIFICATION);
            idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
            return null;
        }).when(identityVerificationOtpService).sendOtp(idVerification, OWNER_ID);
        when(onboardingProcessRepository.findById(PROCESS_ID))
                .thenReturn(createProcessWithConfiguration());

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

    private static Optional<OnboardingProcessEntity> createProcessWithConfiguration() {
        final OnboardingProcessConfigurationEntity configuration = new OnboardingProcessConfigurationEntity();
        configuration.setConfiguration(OnboardingProcessConfigurationValue.builder()
                .otpForIdentityVerification(true)
                .otpForIdentification(true)
                .build());

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setProcessConfiguration(configuration);
        return Optional.of(process);
    }

    @Test
    void testDocumentVerificationTransitionCompleted() throws Exception {
        IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(false);
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

    private IdentityVerificationEntity createIdentityVerification(IdentityVerificationStatus status) {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.CLIENT_EVALUATION, status);
        when(onboardingProcessRepository.findByActivationIdAndStatusWithLock(idVerification.getActivationId(), OnboardingStatus.VERIFICATION_IN_PROGRESS))
                .thenReturn(Optional.of(createOnboardingProcessEntity()));
        return idVerification;
    }

    private void testClientVerificationStatus(final IdentityVerificationStatus identityStatus, final OnboardingState expectedState) throws Exception {
        final IdentityVerificationEntity idVerification = createIdentityVerification(IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL, IdentityVerificationStatus.ACCEPTED);

        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(onboardingProcessRepository.findByActivationIdAndStatusWithLock(idVerification.getActivationId(), OnboardingStatus.VERIFICATION_IN_PROGRESS))
                .thenReturn(Optional.of(createOnboardingProcessEntity()));
        when(clientEvaluationEnabledGuard.evaluate(any())).thenReturn(true);

        doAnswer(args -> {
            final IdentityVerificationEntity identityVerification = args.getArgument(0, IdentityVerificationEntity.class);
            identityVerification.setPhase(IdentityVerificationPhase.CLIENT_EVALUATION);
            identityVerification.setStatus(identityStatus);
            return switch (identityStatus) {
                case ACCEPTED -> EvaluateClientResponse.EvaluationResult.OK;
                case REJECTED -> EvaluateClientResponse.EvaluationResult.NOK;
                case IN_PROGRESS -> EvaluateClientResponse.EvaluationResult.WAIT;
                default -> null; // FAILED -> null => should lead to CLIENT_EVALUATION_FAILED
            };
        }).when(clientEvaluationService).processClientEvaluation(any(IdentityVerificationEntity.class), eq(OWNER_ID));

        final Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        stateMachine.sendEvent(reactor.core.publisher.Mono.just(message)).blockLast();

        await()
                .atMost(Duration.ofSeconds(2))
                .pollInterval(Duration.ofMillis(10))
                .until(() -> stateMachine.getState().getId() == expectedState);

        assertEquals(expectedState, stateMachine.getState().getId());
    }
}
