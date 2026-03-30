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
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.ClientEvaluationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationCreateService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationTargetActivationService;
import com.wultra.app.onboardingserver.impl.service.OnboardingApprovalService;
import com.wultra.app.onboardingserver.impl.service.OnboardingServiceImpl;
import com.wultra.app.onboardingserver.impl.service.OtpServiceImpl;
import com.wultra.app.onboardingserver.impl.service.PresenceCheckService;
import com.wultra.app.onboardingserver.impl.service.document.DocumentVerificationService;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationResendAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationInitAction;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.guard.ProcessIdentifierGuard;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test transitions defined in {@link StateMachineConfig}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Slf4j
class OnboardingTransitionTest extends AbstractStateMachineTest {

    @MockitoBean
    private ProcessIdentifierGuard processIdentifierGuard;

    @MockitoBean
    private IdentityVerificationService identityVerificationService;

    @MockitoBean
    private IdentityVerificationCreateService identityVerificationCreateService;

    @MockitoBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockitoBean
    private DocumentVerificationService documentVerificationService;

    @MockitoBean
    private VerificationInitAction verificationInitAction;

    @MockitoBean
    private OtpVerificationResendAction otpVerificationResendAction;

    @MockitoBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockitoBean
    private ClientEvaluationService clientEvaluationService;

    @MockitoBean
    private OtpServiceImpl otpServiceImpl;

    @MockitoBean
    private OnboardingServiceImpl onboardingService;

    @MockitoBean
    private PresenceCheckService presenceCheckService;

    @MockitoBean
    private OnboardingApprovalService onboardingApprovalService;

    @MockitoBean
    private IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    @Test
    void testInitialToDocumentUploaded() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(null, IdentityVerificationStatus.NOT_INITIALIZED);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> initMessage = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.IDENTITY_VERIFICATION_INIT);
        stateMachine.sendEvent(Mono.just(initMessage)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testDocumentUploadToPresenceCheckNotInitalized() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(identityVerificationService.startDocumentVerification(any(), any()))
                .thenReturn(IdentityVerificationService.DocumentEvaluationStatus.OK);

        when(documentVerificationService.hasDocumentsVerificationPending(any()))
                .thenReturn(true);

        when(documentVerificationService.executeFinalDocumentVerification(any(), any()))
                .thenReturn(DocumentVerificationService.FinalDocumentVerificationResult.OK);

        when(identityVerificationConfig.isPresenceCheckEnabled())
                .thenReturn(true);
        when(clientEvaluationService.isClientEvaluationEnabled(any()))
                .thenReturn(true);
        when(otpServiceImpl.isOtpVerificationEnabled(any()))
                .thenReturn(true);
        when(clientEvaluationService.processClientEvaluation(any(), any()))
                .thenReturn(EvaluateClientResponse.EvaluationResult.OK);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.DOCUMENT_UPLOADED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(6, visitedStates.size(), "Should have exactly 5 visited states. Visited: " + visitedStates);
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING, visitedStates.get(0));
        assertEquals(OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED, visitedStates.get(1));
        assertEquals(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS, visitedStates.get(2));
        assertEquals(OnboardingState.DOCUMENT_VERIFICATION_FINAL_ACCEPTED, visitedStates.get(3));
        assertEquals(OnboardingState.CLIENT_EVALUATION_ACCEPTED, visitedStates.get(4));
        assertEquals(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED, visitedStates.get(5));
    }

    @Test
    void testDocumentUploadInProgress_documentsPending() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(identityVerificationService.startDocumentVerification(any(), any()))
                .thenReturn(IdentityVerificationService.DocumentEvaluationStatus.NOK);

        when(documentVerificationService.hasDocumentsVerificationPending(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.DOCUMENT_UPLOADED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(2, visitedStates.size(), "Should have exactly 2 visited states. Visited: " + visitedStates);
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING, visitedStates.get(0));
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS, visitedStates.get(1));
    }

    @Test
    void testPresenceCheckNotInitializedToInProgress() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.NOT_INITIALIZED);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(presenceCheckService.init(any(), any()))
                .thenReturn(new SessionInfo());

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.PRESENCE_CHECK_INIT);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size());
        assertEquals(OnboardingState.PRESENCE_CHECK_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testPresenceCheckInProgressToAccepted() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(presenceCheckService.checkPresenceVerification(any(), any()))
                .thenReturn(PresenceCheckStatus.ACCEPTED);

        when(otpServiceImpl.isOtpVerificationEnabled(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.PRESENCE_CHECK_SUBMITTED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(3, visitedStates.size(), "Should have exactly 3 visited states. Visited: " + visitedStates);
        assertEquals(OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING, visitedStates.get(0));
        assertEquals(OnboardingState.PRESENCE_CHECK_ACCEPTED, visitedStates.get(1));
        assertEquals(OnboardingState.OTP_VERIFICATION_PENDING, visitedStates.get(2));
    }

    @Test
    void testPresenceCheckInProgressToNotInitialized() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(presenceCheckService.checkPresenceVerification(any(), any()))
                .thenReturn(PresenceCheckStatus.REJECTED);

        when(onboardingService.isVerifyPresenceWithOtpEnabled(any()))
                .thenReturn(false);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.PRESENCE_CHECK_INIT);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        assertEquals(1, visitedStates.size());
        assertEquals(OnboardingState.PRESENCE_CHECK_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPendingToActivationFinishInProgress() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(true);

        when(presenceCheckService.isVerifyPresenceWithOtpPassed(any()))
                .thenReturn(true);

        when(onboardingApprovalService.isOnboardingApprovalEnabled(any()))
                .thenReturn(false);

        when(identityVerificationTargetActivationService.isTargetActivationEnabled(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.OTP_VERIFIED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPending_failed() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(false);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.OTP_VERIFIED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size());
        assertEquals(OnboardingState.OTP_VERIFICATION_PENDING, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPending_resend() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        when(otpServiceImpl.isOtpVerificationEnabled(any()))
                .thenReturn(true);

        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        final List<OnboardingState> visitedStates = new LinkedList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.OTP_RESEND);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size());
        assertEquals(OnboardingState.OTP_VERIFICATION_PENDING, visitedStates.get(0));
    }

    private static StateMachineListenerAdapter<OnboardingState, OnboardingEvent> createListener(final List<OnboardingState> visitedStates) {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void stateEntered(final State<OnboardingState, OnboardingEvent> state) {
                logger.info("Entered state: {}", state.getId());
                visitedStates.add(state.getId());
            }
        };
    }
}
