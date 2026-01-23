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

import com.wultra.app.onboardingserver.provider.model.response.ApproveClientResponse;
import com.wultra.app.onboardingserver.statemachine.action.PersistTargetStateAction;
import com.wultra.app.onboardingserver.statemachine.action.clientevaluation.ClientEvaluationAction;
import com.wultra.app.onboardingserver.statemachine.action.clientevaluation.ClientEvaluationInitAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationResendAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationSendAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.MoveToPresenceCheckVerificationPendingAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckInitAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckNotInitializedAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckVerificationAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.*;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.guard.*;
import com.wultra.app.onboardingserver.statemachine.guard.document.DocumentUploadVerificationPendingGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerifiedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusAcceptedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusFailedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusInProgressGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusRejectedGuard;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * State machine configuration
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server-onboarding.identity-verification.enabled",
        havingValue = "true"
)
@Configuration
@AllArgsConstructor
@Slf4j
@EnableStateMachineFactory(name = "enrollmentStateMachine")
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OnboardingState, OnboardingEvent> {

    private final ClientEvaluationInitAction clientEvaluationInitAction;

    private final ClientEvaluationAction clientEvaluationAction;

    private final OtpVerificationResendAction otpVerificationResendAction;

    private final OtpVerificationSendAction otpVerificationSendAction;

    private final PresenceCheckInitAction presenceCheckInitAction;

    private final PresenceCheckNotInitializedAction presenceCheckNotInitializedAction;

    private final PresenceCheckVerificationAction presenceCheckVerificationAction;

    private final MoveToPresenceCheckVerificationPendingAction moveToPresenceCheckVerificationPendingAction;

    private final MoveToDocumentUploadVerificationPendingAction moveToDocumentUploadVerificationPendingAction;

    private final MoveToDocumentVerificationFinalInProgressAction moveToDocumentVerificationFinalInProgressAction;

    private final DocumentsVerificationPendingGuard documentsVerificationPendingGuard;

    private final VerificationDocumentStartAction verificationDocumentStartAction;

    private final VerificationInitAction verificationInitAction;

    private final VerificationProcessResultAction verificationProcessResultAction;

    private final DocumentVerificationFinalAction documentVerificationFinalAction;

    private final OnboardingApprovalAction onboardingApprovalAction;

    private final PersistTargetStateAction persistTargetStateAction;

    private final DocumentUploadVerificationPendingGuard documentUploadVerificationPendingGuard;

    private final OtpVerificationEnabledGuard otpVerificationEnabledGuard;

    private final OtpVerifiedGuard otpVerifiedGuard;

    private final PresenceCheckEnabledGuard presenceCheckEnabledGuard;

    private final ProcessIdentifierGuard processIdentifierGuard;

    private final StatusAcceptedGuard statusAcceptedGuard;

    private final StatusFailedGuard statusFailedGuard;

    private final StatusInProgressGuard statusInProgressGuard;

    private final StatusRejectedGuard statusRejectedGuard;

    private final TargetActivationFinishedGuard targetActivationFinishedGuard;

    private final TargetActivationEnabledGuard targetActivationEnabledGuard;

    private final OnboardingApprovalEnabledGuard onboardingApprovalEnabledGuard;

    @Override
    public void configure(StateMachineConfigurationConfigurer<OnboardingState, OnboardingEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<OnboardingState, OnboardingEvent> states) throws Exception {
        states
                .withStates()
                .initial(OnboardingState.INITIAL)
                .choice(OnboardingState.CHOICE_CLIENT_EVALUATION_PROCESSING)
                .choice(OnboardingState.CHOICE_DOCUMENT_UPLOAD)
                .choice(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)
                .choice(OnboardingState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)
                .choice(OnboardingState.CHOICE_OTP_ENABLED)
                .choice(OnboardingState.CHOICE_OTP_VERIFICATION)
                .choice(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .choice(OnboardingState.CHOICE_COMPLETED_STATE)
                .choice(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED)
                .choice(OnboardingState.CHOICE_ONBOARDING_APPROVAL_RESULT)
                .choice(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED)
                .end(OnboardingState.CLIENT_EVALUATION_FAILED)
                .end(OnboardingState.CLIENT_EVALUATION_REJECTED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_FAILED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_REJECTED)
                .end(OnboardingState.ONBOARDING_APPROVAL_FAILED)
                .end(OnboardingState.ONBOARDING_APPROVAL_REJECTED)
                .end(OnboardingState.COMPLETED_ACCEPTED)
                .end(OnboardingState.COMPLETED_FAILED)
                .end(OnboardingState.COMPLETED_REJECTED)
                .states(EnumSet.allOf(OnboardingState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        configureInitialTransition(transitions);
        configureDocumentUploadTransitions(transitions);
        configureDocumentVerificationTransitions(transitions);
        configureDocumentVerificationFinalTransitions(transitions);
        configureClientEvaluationTransitions(transitions);
        configurePresenceCheckTransitions(transitions);
        configureOnboardingApproval(transitions);
        configureOtpTransitions(transitions);
        configureActivationFinishTransitions(transitions);
        configureCompletedTransition(transitions);
    }

    @Bean
    public StateMachineListener<OnboardingState, OnboardingEvent> listener() {
        return new StateMachineListenerAdapter<>() {

            @Override
            public void eventNotAccepted(Message<OnboardingEvent> event) {
                logger.error("Not accepted event {}", event.getPayload());
            }

            @Override
            public void stateChanged(State<OnboardingState, OnboardingEvent> from, State<OnboardingState, OnboardingEvent> to) {
                if (from != null) {
                    logger.debug("State changed from {} to {}", from.getId(), to.getId());
                }
            }

        };
    }

    private void configureInitialTransition(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.INITIAL)
                .event(OnboardingEvent.IDENTITY_VERIFICATION_INIT)
                .guard(processIdentifierGuard)
                .action(verificationInitAction)
                .target(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS);
    }

    private void configureDocumentUploadTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(documentsVerificationPendingGuard)
                .action(moveToDocumentUploadVerificationPendingAction)
                .target(OnboardingState.CHOICE_DOCUMENT_UPLOAD)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_DOCUMENT_UPLOAD)
                .first(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING, documentUploadVerificationPendingGuard)
                .last(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS);
    }

    private void configureDocumentVerificationTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .action(verificationDocumentStartAction)
                .guard(createCompositeGuard(processIdentifierGuard, documentsVerificationPendingGuard))
                .target(OnboardingState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)
                .first(OnboardingState.DOCUMENT_VERIFICATION_IN_PROGRESS, statusInProgressGuard)
                .then(OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED, statusAcceptedGuard)
                .then(OnboardingState.DOCUMENT_VERIFICATION_REJECTED, statusRejectedGuard)
                .then(OnboardingState.DOCUMENT_VERIFICATION_FAILED, statusFailedGuard)
                .last(OnboardingState.UNEXPECTED_STATE)

                .and()
                .withExternal()
                .source(OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(processIdentifierGuard)
                .action(moveToDocumentVerificationFinalInProgressAction)
                .target(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS);
    }

    private void configureDocumentVerificationFinalTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(processIdentifierGuard)
                .action(documentVerificationFinalAction)
                .target(OnboardingState.DOCUMENT_VERIFICATION_FINAL_ACCEPTED)

                .and()
                .withExternal()
                .source(OnboardingState.DOCUMENT_VERIFICATION_FINAL_ACCEPTED)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(processIdentifierGuard)
                .action(clientEvaluationInitAction)
                .target(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS);
    }

    private void configureClientEvaluationTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .action(clientEvaluationAction)
                .guard(processIdentifierGuard)
                .target(OnboardingState.CHOICE_CLIENT_EVALUATION_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_CLIENT_EVALUATION_PROCESSING)
                .first(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS, statusInProgressGuard)
                .then(OnboardingState.CLIENT_EVALUATION_ACCEPTED, statusAcceptedGuard)
                .then(OnboardingState.CLIENT_EVALUATION_REJECTED, statusRejectedGuard)
                .then(OnboardingState.CLIENT_EVALUATION_FAILED, statusFailedGuard)
                .last(OnboardingState.UNEXPECTED_STATE)

                .and()
                .withExternal()
                .source(OnboardingState.CLIENT_EVALUATION_ACCEPTED)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(processIdentifierGuard)
                .target(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)
                .first(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED, presenceCheckEnabledGuard, presenceCheckNotInitializedAction)
                .then(OnboardingState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction)
                .last(OnboardingState.CHOICE_COMPLETED_STATE, verificationProcessResultAction);
    }

    private void configurePresenceCheckTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED)
                .event(OnboardingEvent.PRESENCE_CHECK_INIT)
                .guard(createCompositeGuard(processIdentifierGuard, presenceCheckEnabledGuard))
                .action(presenceCheckInitAction)
                .target(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
                .event(OnboardingEvent.PRESENCE_CHECK_INIT)
                .guard(createCompositeGuard(processIdentifierGuard, presenceCheckEnabledGuard))
                .action(presenceCheckInitAction)
                .target(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
                .event(OnboardingEvent.PRESENCE_CHECK_SUBMITTED)
                .guard(processIdentifierGuard)
                .action(moveToPresenceCheckVerificationPendingAction)
                .target(OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING)
                .action(presenceCheckVerificationAction)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(processIdentifierGuard)
                .target(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .first(OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING, statusInProgressGuard)
                .then(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED, statusAcceptedGuard)
                .then(OnboardingState.PRESENCE_CHECK_REJECTED, statusRejectedGuard)
                .then(OnboardingState.PRESENCE_CHECK_FAILED, statusFailedGuard)
                .last(OnboardingState.UNEXPECTED_STATE)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_REJECTED)
                .target(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_FAILED)
                .target(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED);
    }

    private void configureOnboardingApproval(final StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED)
                .first(OnboardingState.CHOICE_ONBOARDING_APPROVAL_RESULT, onboardingApprovalEnabledGuard, onboardingApprovalAction)
                .last(OnboardingState.CHOICE_OTP_ENABLED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_ONBOARDING_APPROVAL_RESULT)
                .first(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED, isApprovalResult(ApproveClientResponse.EvaluationResult.OK), persistTargetStateAction)
                .then(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS, isApprovalResult(ApproveClientResponse.EvaluationResult.WAIT), persistTargetStateAction)
                .then(OnboardingState.ONBOARDING_APPROVAL_REJECTED, isApprovalResult(ApproveClientResponse.EvaluationResult.NOK), persistTargetStateAction)
                .last(OnboardingState.ONBOARDING_APPROVAL_FAILED, persistTargetStateAction)

                .and()
                .withExternal()
                .source(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS)
                .event(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_APPROVE)
                .target(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED)
                .action(persistTargetStateAction)

                .and()
                .withExternal()
                .source(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS)
                .event(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_REJECT)
                .target(OnboardingState.ONBOARDING_APPROVAL_REJECTED)
                .action(persistTargetStateAction)

                .and()
                .withExternal()
                .source(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .target(OnboardingState.CHOICE_OTP_ENABLED);
    }

    private static Guard<OnboardingState, OnboardingEvent> isApprovalResult(ApproveClientResponse.EvaluationResult expectedResult) {
        return context -> evaluateApprovalResult(context, expectedResult);
    }

    private static boolean evaluateApprovalResult(final StateContext<OnboardingState, OnboardingEvent> context, final ApproveClientResponse.EvaluationResult expectedResult) {
        final Object result = context.getExtendedState().getVariables().get(OnboardingApprovalAction.RESULT_KEY);
        if (!(result instanceof ApproveClientResponse.EvaluationResult)) {
            return false;
        }
        return result == expectedResult;
    }

    private void configureOtpTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_OTP_ENABLED)
                .first(OnboardingState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction) // action persist the state OTP_VERIFICATION_PENDING
                .last(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED)

                .and()
                .withExternal()
                .source(OnboardingState.OTP_VERIFICATION_PENDING)
                .event(OnboardingEvent.OTP_VERIFICATION_RESEND)
                .guard(createCompositeGuard(processIdentifierGuard, otpVerificationEnabledGuard))
                .action(otpVerificationResendAction)
                .target(OnboardingState.OTP_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(OnboardingState.OTP_VERIFICATION_PENDING)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(processIdentifierGuard)
                .target(OnboardingState.CHOICE_OTP_VERIFICATION)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_OTP_VERIFICATION)
                .first(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED, otpVerifiedGuard)
                .last(OnboardingState.OTP_VERIFICATION_PENDING);
    }

    @SafeVarargs
    private static Guard<OnboardingState, OnboardingEvent> createCompositeGuard(final Guard<OnboardingState, OnboardingEvent>... guards) {
        return context -> Arrays.stream(guards).allMatch(it -> it.evaluate(context));
    }

    private void configureActivationFinishTransitions(final StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED)
                .first(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, targetActivationEnabledGuard, persistTargetStateAction)
                .last(OnboardingState.CHOICE_COMPLETED_STATE, verificationProcessResultAction)

                .and()
                .withExternal()
                .source(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .guard(targetActivationFinishedGuard)
                .action(verificationProcessResultAction)
                .target(OnboardingState.CHOICE_COMPLETED_STATE);
    }

    private void configureCompletedTransition(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_COMPLETED_STATE)
                .first(OnboardingState.COMPLETED_ACCEPTED, statusAcceptedGuard)
                .then(OnboardingState.COMPLETED_REJECTED, statusRejectedGuard)
                .then(OnboardingState.COMPLETED_FAILED, statusFailedGuard)
                .last(OnboardingState.UNEXPECTED_STATE);
    }

}
