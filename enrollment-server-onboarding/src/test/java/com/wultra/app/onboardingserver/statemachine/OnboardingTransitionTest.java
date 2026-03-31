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
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.*;
import com.wultra.app.onboardingserver.impl.service.document.DocumentVerificationService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationResultService;
import com.wultra.app.onboardingserver.provider.model.response.ApproveClientResponse;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationResendAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationInitAction;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.guard.ProcessIdentifierGuard;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

    @MockitoBean
    private VerificationResultService verificationResultService;

    @Test
    void testInitialToDocumentUploaded() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(null, IdentityVerificationStatus.NOT_INITIALIZED, visitedStates);

        sendMessage(OnboardingEvent.IDENTITY_VERIFICATION_INIT, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testDocumentUploadToPresenceCheckNotInitialized() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

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

        sendMessage(OnboardingEvent.DOCUMENT_UPLOADED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(6, visitedStates.size(), "Should have exactly 6 visited states. Visited: " + visitedStates);
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

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        when(identityVerificationService.startDocumentVerification(any(), any()))
                .thenReturn(IdentityVerificationService.DocumentEvaluationStatus.NOK);

        when(documentVerificationService.hasDocumentsVerificationPending(any()))
                .thenReturn(true);

        sendMessage(OnboardingEvent.DOCUMENT_UPLOADED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(2, visitedStates.size(), "Should have exactly 2 visited states. Visited: " + visitedStates);
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING, visitedStates.get(0));
        assertEquals(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS, visitedStates.get(1));
    }

    @Test
    void testPresenceCheckNotInitializedToInProgress() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.NOT_INITIALIZED, visitedStates);

        when(presenceCheckService.init(any(), any()))
                .thenReturn(new SessionInfo());

        sendMessage(OnboardingEvent.PRESENCE_CHECK_INIT, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.PRESENCE_CHECK_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testPresenceCheckInProgressToAccepted() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        when(presenceCheckService.checkPresenceVerification(any(), any()))
                .thenReturn(PresenceCheckStatus.ACCEPTED);

        when(otpServiceImpl.isOtpVerificationEnabled(any()))
                .thenReturn(true);

        sendMessage(OnboardingEvent.PRESENCE_CHECK_SUBMITTED, stateMachine);

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

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        when(presenceCheckService.checkPresenceVerification(any(), any()))
                .thenReturn(PresenceCheckStatus.REJECTED);

        when(onboardingService.isVerifyPresenceWithOtpEnabled(any()))
                .thenReturn(false);

        sendMessage(OnboardingEvent.PRESENCE_CHECK_INIT, stateMachine);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.PRESENCE_CHECK_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPendingToActivationFinishInProgress() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(true);

        when(presenceCheckService.isVerifyPresenceWithOtpPassed(any()))
                .thenReturn(true);

        when(onboardingApprovalService.isOnboardingApprovalEnabled(any()))
                .thenReturn(false);

        when(identityVerificationTargetActivationService.isTargetActivationEnabled(any()))
                .thenReturn(true);

        sendMessage(OnboardingEvent.OTP_VERIFIED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPending_failed() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(false);

        sendMessage(OnboardingEvent.OTP_VERIFIED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.OTP_VERIFICATION_PENDING, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPending_resend() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        when(otpServiceImpl.isOtpVerificationEnabled(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        sendMessage(OnboardingEvent.OTP_RESEND, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.OTP_VERIFICATION_PENDING, visitedStates.get(0));
    }

    @Test
    void testActivationFinishInProgressToCompletedAccepted() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID))
                .thenReturn(true);

        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.OK);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.ACTIVATION_FINISH, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        sendMessage(OnboardingEvent.EVENT_NEXT_STATE, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.COMPLETED_ACCEPTED, visitedStates.get(0));
    }

    @Test
    void testActivationFinishInProgressToCompletedFailed() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID))
                .thenReturn(true);

        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.FAILED);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.ACTIVATION_FINISH, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        sendMessage(OnboardingEvent.EVENT_NEXT_STATE, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.COMPLETED_FAILED, visitedStates.get(0));
    }

    @Test
    void testActivationFinishInProgress_notFinishedStayInProgress() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID))
                .thenReturn(false);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.ACTIVATION_FINISH, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        sendMessage(OnboardingEvent.EVENT_NEXT_STATE, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(0, visitedStates.size(), "Should not transition anywhere, listener should not capture any state entry. Visited: " + visitedStates);
        assertEquals(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, stateMachine.getState().getId());
    }

    @Test
    void testOtpVerificationPendingToOnboardingApprovalAccepted() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(true);
        when(presenceCheckService.isVerifyPresenceWithOtpPassed(any()))
                .thenReturn(true);
        when(onboardingApprovalService.isOnboardingApprovalEnabled(any()))
                .thenReturn(true);
        when(identityVerificationTargetActivationService.isTargetActivationEnabled(any()))
                .thenReturn(true);

        when(onboardingApprovalService.approve(any()))
                .thenReturn(ApproveClientResponse.ApprovalResult.OK);

        sendMessage(OnboardingEvent.OTP_VERIFIED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(2, visitedStates.size(), "Should have exactly 2 visited states. Visited: " + visitedStates);
        assertEquals(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED, visitedStates.get(0));
        assertEquals(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, visitedStates.get(1));
    }

    @Test
    void testOtpVerificationPendingToOnboardingApprovalInProgress() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(true);
        when(presenceCheckService.isVerifyPresenceWithOtpPassed(any()))
                .thenReturn(true);
        when(onboardingApprovalService.isOnboardingApprovalEnabled(any()))
                .thenReturn(true);

        when(onboardingApprovalService.approve(any()))
                .thenReturn(ApproveClientResponse.ApprovalResult.WAIT);

        sendMessage(OnboardingEvent.OTP_VERIFIED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS, visitedStates.get(0));
    }

    @Test
    void testOnboardingApprovalInProgressToAccepted() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.ONBOARDING_APPROVAL, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        when(identityVerificationTargetActivationService.isTargetActivationEnabled(any()))
                .thenReturn(true);

        sendMessage(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_APPROVE, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(2, visitedStates.size(), "Should have exactly 2 visited states. Visited: " + visitedStates);
        assertEquals(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED, visitedStates.get(0));
        assertEquals(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, visitedStates.get(1));
    }

    @Test
    void testOnboardingApprovalInProgressToRejected() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.ONBOARDING_APPROVAL, IdentityVerificationStatus.IN_PROGRESS, visitedStates);

        sendMessage(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_REJECT, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.ONBOARDING_APPROVAL_REJECTED, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPendingToOnboardingApprovalRejected() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(true);
        when(presenceCheckService.isVerifyPresenceWithOtpPassed(any()))
                .thenReturn(true);
        when(onboardingApprovalService.isOnboardingApprovalEnabled(any()))
                .thenReturn(true);
        when(onboardingApprovalService.approve(any()))
                .thenReturn(ApproveClientResponse.ApprovalResult.NOK);

        sendMessage(OnboardingEvent.OTP_VERIFIED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates);
        assertEquals(OnboardingState.ONBOARDING_APPROVAL_REJECTED, visitedStates.get(0));
    }

    @Test
    void testOtpVerificationPendingToOnboardingApprovalFailed() throws Exception {
        when(processIdentifierGuard.evaluate(any()))
                .thenReturn(true);

        final List<OnboardingState> visitedStates = new LinkedList<>();
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = startStateMachine(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING, visitedStates);

        when(identityVerificationOtpService.isUserVerifiedUsingOtp(any()))
                .thenReturn(true);
        when(presenceCheckService.isVerifyPresenceWithOtpPassed(any()))
                .thenReturn(true);
        when(onboardingApprovalService.isOnboardingApprovalEnabled(any()))
                .thenReturn(true);
        when(onboardingApprovalService.approve(any()))
                .thenReturn(null);

        sendMessage(OnboardingEvent.OTP_VERIFIED, stateMachine);

        logger.info("Visited states: {}", visitedStates);

        assertEquals(1, visitedStates.size(), "Should have exactly 1 visited state. Visited: " + visitedStates + ". Current state: " + stateMachine.getState().getId());
        assertEquals(OnboardingState.ONBOARDING_APPROVAL_FAILED, visitedStates.get(0));
    }

    private @NonNull StateMachine<OnboardingState, OnboardingEvent> startStateMachine(
            final IdentityVerificationPhase phase,
            final IdentityVerificationStatus status,
            final List<OnboardingState> visitedStates) throws Exception {

        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(phase, status);
        stateMachine.startReactively().block();
        stateMachine.addStateListener(createListener(visitedStates));
        return stateMachine;
    }

    private void sendMessage(final OnboardingEvent event, final StateMachine<OnboardingState, OnboardingEvent> stateMachine) {
        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, event);
        stateMachine.sendEvent(Mono.just(message)).blockLast();
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
