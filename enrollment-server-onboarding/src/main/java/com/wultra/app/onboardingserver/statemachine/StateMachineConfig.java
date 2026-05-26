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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.action.clientevaluation.ClientEvaluationAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationResendAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationSendAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckInitAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckVerificationAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.*;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.event.OnboardingCompletedAcceptedEvent;
import com.wultra.app.onboardingserver.statemachine.guard.*;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerifiedGuard;
import com.wultra.app.onboardingserver.statemachine.util.StateContextUtil;
import com.wultra.core.rest.model.base.response.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.config.configurers.StateConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

/**
 * State machine configuration
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Configuration
@AllArgsConstructor
@Slf4j
@EnableStateMachineFactory(name = "enrollmentStateMachine")
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OnboardingState, OnboardingEvent> {

    private final ClientEvaluationAction clientEvaluationAction;

    private final OtpVerificationResendAction otpVerificationResendAction;

    private final OtpVerificationSendAction otpVerificationSendAction;

    private final PresenceCheckInitAction presenceCheckInitAction;

    private final PresenceCheckVerificationAction presenceCheckVerificationAction;

    private final DocumentsVerificationPendingGuard documentsVerificationPendingGuard;

    private final VerificationDocumentStartAction verificationDocumentStartAction;

    private final VerificationInitAction verificationInitAction;

    private final VerificationProcessResultAction verificationProcessResultAction;

    private final DocumentVerificationFinalAction documentVerificationFinalAction;

    private final OnboardingApprovalAction onboardingApprovalAction;

    private final IdentityVerificationService identityVerificationService;

    private final OtpVerificationEnabledGuard otpVerificationEnabledGuard;

    private final OtpVerifiedGuard otpVerifiedGuard;

    private final PresenceCheckEnabledGuard presenceCheckEnabledGuard;

    private final TargetActivationFinishedGuard targetActivationFinishedGuard;

    private final TargetActivationEnabledGuard targetActivationEnabledGuard;

    private final OnboardingApprovalEnabledGuard onboardingApprovalEnabledGuard;

    private final ClientEvaluationEnabledGuard clientEvaluationEnabledGuard;

    private final VerifyPresenceWithOtpEnabledGuard verifyPresenceWithOtpEnabledGuard;

    private final VerifyPresenceWithOtpPassedGuard verifyPresenceWithOtpPassedGuard;

    private final ConsentResolvedGuard consentResolvedGuard;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void configure(StateMachineConfigurationConfigurer<OnboardingState, OnboardingEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<OnboardingState, OnboardingEvent> states) throws Exception {
        final var configurer = states.withStates();
        configurer
                .initial(OnboardingState.INITIAL)
                .choice(OnboardingState.CHOICE_ONBOARDING_CLIENT_EVALUATION_ENABLED)
                .choice(OnboardingState.CHOICE_ONBOARDING_CLIENT_EVALUATION_RESULT)
                .choice(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)
                .choice(OnboardingState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)
                .choice(OnboardingState.CHOICE_DOCUMENT_FINAL_VERIFICATION_PROCESSING)
                .choice(OnboardingState.CHOICE_OTP_ENABLED)
                .choice(OnboardingState.CHOICE_OTP_VERIFICATION)
                .choice(OnboardingState.CHOICE_PRESENCE_CHECK_ENABLED)
                .choice(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .choice(OnboardingState.CHOICE_COMPLETED_STATE)
                .choice(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED)
                .choice(OnboardingState.CHOICE_ONBOARDING_APPROVAL_RESULT)
                .choice(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED)
                .choice(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_ENABLED)
                .choice(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_PROCESSING)
                .end(OnboardingState.CLIENT_EVALUATION_FAILED)
                .end(OnboardingState.CLIENT_EVALUATION_REJECTED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_FAILED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_REJECTED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_FINAL_REJECTED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_FINAL_FAILED)
                .end(OnboardingState.ONBOARDING_APPROVAL_FAILED)
                .end(OnboardingState.ONBOARDING_APPROVAL_REJECTED)
                .end(OnboardingState.COMPLETED_ACCEPTED)
                .end(OnboardingState.COMPLETED_FAILED)
                .states(EnumSet.allOf(OnboardingState.class));

        registerPersistFunctions(configurer);

        registerPublishEventFunction(configurer);
    }

    private void registerPersistFunctions(final StateConfigurer<OnboardingState, OnboardingEvent> configurer) {
        final var states = List.of(
                OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS,
                OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING,
                OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED,
                OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS,
                OnboardingState.DOCUMENT_VERIFICATION_FINAL_ACCEPTED,
                OnboardingState.DOCUMENT_VERIFICATION_FINAL_REJECTED,
                OnboardingState.DOCUMENT_VERIFICATION_FINAL_FAILED,
                OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED,
                OnboardingState.PRESENCE_CHECK_IN_PROGRESS,
                OnboardingState.PRESENCE_CHECK_ACCEPTED,
                OnboardingState.PRESENCE_CHECK_REJECTED,
                OnboardingState.PRESENCE_CHECK_FAILED,
                OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING,
                OnboardingState.ONBOARDING_APPROVAL_REJECTED,
                OnboardingState.ONBOARDING_APPROVAL_FAILED,
                OnboardingState.ACTIVATION_FINISH_IN_PROGRESS,
                OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS,
                OnboardingState.ONBOARDING_APPROVAL_ACCEPTED,
                OnboardingState.OTP_VERIFICATION_PENDING,
                OnboardingState.CLIENT_EVALUATION_ACCEPTED,
                OnboardingState.CLIENT_EVALUATION_IN_PROGRESS,
                OnboardingState.CLIENT_EVALUATION_REJECTED,
                OnboardingState.CLIENT_EVALUATION_FAILED,
                OnboardingState.COMPLETED_FAILED);

        for (final OnboardingState state : states) {
            configurer.stateEntryFunction(state, persistState(state));
        }
    }

    private Function<StateContext<OnboardingState, OnboardingEvent>, Mono<Void>> persistState(final OnboardingState state) {
        return context -> persistState(context, state);
    }

    /*
     * We avoid .subscribeOn(Schedulers.boundedElastic()) in this method.
     * So far, the state is also changed out of the StateMachineConfig, for example, in actions.
     * If we switch to another thread, we might end up with state changes executed in parallel, which can cause race conditions.
     * By executing the moveToPhaseAndStatus method in the same thread, we ensure that state changes are processed sequentially, maintaining the integrity of the state machine.
     */
    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    private Mono<Void> persistState(final StateContext<OnboardingState, OnboardingEvent> context, final OnboardingState state) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        logger.debug("action: persistState, state: initiated, target: {}", state);
        return Mono.fromRunnable(() -> identityVerificationService.moveToPhaseAndStatus(identityVerification, state.getPhase(), state.getStatus(), ownerId));
    }

    private void registerPublishEventFunction(final StateConfigurer<OnboardingState, OnboardingEvent> configurer) {
        // For COMPLETED_ACCEPTED we have to combine the persist and publish entry functions into a single
        // entry function. Spring State Machine only keeps the last function registered via stateEntryFunction()
        // for a given state, so registering them separately would cause one of them to be silently dropped.
        final var persist = persistState(OnboardingState.COMPLETED_ACCEPTED);
        final var publish = publishCompletedAcceptedEvent();
        configurer.stateEntryFunction(
                OnboardingState.COMPLETED_ACCEPTED,
                context -> persist.apply(context).then(publish.apply(context)));
    }

    private Function<StateContext<OnboardingState, OnboardingEvent>, Mono<Void>> publishCompletedAcceptedEvent() {
        return context -> Mono.fromRunnable(() -> {
            final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
            final String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

            logger.debug("Publishing OnboardingCompletedAcceptedEvent for processId={}, {}", processId, ownerId);
            applicationEventPublisher.publishEvent(new OnboardingCompletedAcceptedEvent(StateMachineConfig.this, ownerId, processId));
        }).then();
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
                logger.error("Not accepted event {}, processId: {}", event.getPayload(), event.getHeaders().get(EventHeaderName.PROCESS_ID));
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
                .guard(consentResolvedGuard)
                .action(verificationInitAction)
                .target(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS);
    }

    private void configureDocumentUploadTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS)
                .event(OnboardingEvent.DOCUMENT_UPLOADED)
                .guard(documentsVerificationPendingGuard)
                .target(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING);
    }

    private void configureDocumentVerificationTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                .action(verificationDocumentStartAction)
                .target(OnboardingState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)
                .first(OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED, VerificationDocumentStartAction.isResultOk())
                .then(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS, VerificationDocumentStartAction.isResultInProgress())
                .last(OnboardingState.DOCUMENT_VERIFICATION_FAILED)

                .and()
                .withExternal()
                .source(OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED)
                .target(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS);
    }

    private void configureDocumentVerificationFinalTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS)
                .action(documentVerificationFinalAction)
                .target(OnboardingState.CHOICE_DOCUMENT_FINAL_VERIFICATION_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_DOCUMENT_FINAL_VERIFICATION_PROCESSING)
                .first(OnboardingState.DOCUMENT_VERIFICATION_FINAL_ACCEPTED, DocumentVerificationFinalAction.isResultOk())
                .then(OnboardingState.DOCUMENT_VERIFICATION_FINAL_REJECTED, DocumentVerificationFinalAction.isResultRejected())
                .last(OnboardingState.DOCUMENT_VERIFICATION_FINAL_FAILED)

                .and()
                .withExternal()
                .source(OnboardingState.DOCUMENT_VERIFICATION_FINAL_ACCEPTED)
                .target(OnboardingState.CHOICE_ONBOARDING_CLIENT_EVALUATION_ENABLED);
    }

    private void configureClientEvaluationTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_ONBOARDING_CLIENT_EVALUATION_ENABLED)
                .first(OnboardingState.CHOICE_ONBOARDING_CLIENT_EVALUATION_RESULT, clientEvaluationEnabledGuard, clientEvaluationAction)
                .last(OnboardingState.CHOICE_PRESENCE_CHECK_ENABLED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_ONBOARDING_CLIENT_EVALUATION_RESULT)
                .first(OnboardingState.CLIENT_EVALUATION_ACCEPTED, ClientEvaluationAction.isResultOk())
                .then(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS, ClientEvaluationAction.isResultInProgress())
                .then(OnboardingState.CLIENT_EVALUATION_REJECTED, ClientEvaluationAction.isResultRejected())
                .last(OnboardingState.CLIENT_EVALUATION_FAILED)

                .and()
                .withExternal()
                .source(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS)
                .event(OnboardingEvent.CLIENT_EVALUATION_ACKNOWLEDGED_APPROVE)
                .target(OnboardingState.CLIENT_EVALUATION_ACCEPTED)

                .and()
                .withExternal()
                .source(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS)
                .event(OnboardingEvent.CLIENT_EVALUATION_ACKNOWLEDGED_REJECT)
                .target(OnboardingState.CLIENT_EVALUATION_REJECTED)

                .and()
                .withExternal()
                .source(OnboardingState.CLIENT_EVALUATION_ACCEPTED)
                .target(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)
                .last(OnboardingState.CHOICE_PRESENCE_CHECK_ENABLED);
    }

    private void configurePresenceCheckTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_PRESENCE_CHECK_ENABLED)
                .first(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED, presenceCheckEnabledGuard)
                .last(OnboardingState.CHOICE_OTP_ENABLED)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED)
                .event(OnboardingEvent.PRESENCE_CHECK_INIT)
                .action(presenceCheckInitAction)
                .target(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
                .event(OnboardingEvent.PRESENCE_CHECK_INIT)
                .action(presenceCheckInitAction)
                .target(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
                .event(OnboardingEvent.PRESENCE_CHECK_SUBMITTED)
                .action(context -> StateContextUtil.setResponseOk(context, new Response()))
                .target(OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_VERIFICATION_PENDING)
                .action(presenceCheckVerificationAction)
                .target(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .first(OnboardingState.PRESENCE_CHECK_ACCEPTED, PresenceCheckVerificationAction.isResultOk())
                .then(OnboardingState.PRESENCE_CHECK_REJECTED, PresenceCheckVerificationAction.isResultRejected())
                .last(OnboardingState.PRESENCE_CHECK_FAILED)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_ACCEPTED)
                .target(OnboardingState.CHOICE_OTP_ENABLED)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_REJECTED)
                .target(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_ENABLED)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_FAILED)
                .target(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_ENABLED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_ENABLED)
                .first(OnboardingState.CHOICE_OTP_ENABLED, verifyPresenceWithOtpEnabledGuard)
                .last(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED);
    }

    private void configureOnboardingApproval(final StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED)
                .first(OnboardingState.CHOICE_ONBOARDING_APPROVAL_RESULT, onboardingApprovalEnabledGuard, onboardingApprovalAction)
                .last(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_ONBOARDING_APPROVAL_RESULT)
                .first(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED, OnboardingApprovalAction.isResultOk())
                .then(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS, OnboardingApprovalAction.isResultInProgress())
                .then(OnboardingState.ONBOARDING_APPROVAL_REJECTED, OnboardingApprovalAction.isResultRejected())
                .last(OnboardingState.ONBOARDING_APPROVAL_FAILED)

                .and()
                .withExternal()
                .source(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS)
                .event(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_APPROVE)
                .target(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED)

                .and()
                .withExternal()
                .source(OnboardingState.ONBOARDING_APPROVAL_IN_PROGRESS)
                .event(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_REJECT)
                .target(OnboardingState.ONBOARDING_APPROVAL_REJECTED)

                .and()
                .withExternal()
                .source(OnboardingState.ONBOARDING_APPROVAL_ACCEPTED)
                .target(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED);
    }

    private void configureOtpTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_OTP_ENABLED)
                .first(OnboardingState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction)
                .last(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED)

                .and()
                .withExternal()
                .source(OnboardingState.OTP_VERIFICATION_PENDING)
                .event(OnboardingEvent.OTP_RESEND)
                .guard(otpVerificationEnabledGuard)
                .action(otpVerificationResendAction)
                .target(OnboardingState.OTP_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(OnboardingState.OTP_VERIFICATION_PENDING)
                .event(OnboardingEvent.OTP_VERIFIED)
                .target(OnboardingState.CHOICE_OTP_VERIFICATION)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_OTP_VERIFICATION)
                .first(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_PROCESSING, otpVerifiedGuard)
                .last(OnboardingState.OTP_VERIFICATION_PENDING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_VERIFY_PRESENCE_WITH_OTP_PROCESSING)
                .first(OnboardingState.CHOICE_ONBOARDING_APPROVAL_ENABLED, verifyPresenceWithOtpPassedGuard)
                .last(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED);
    }


    private void configureActivationFinishTransitions(final StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_ACTIVATION_FINISH_ENABLED)
                .first(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS, targetActivationEnabledGuard)
                .last(OnboardingState.CHOICE_COMPLETED_STATE, verificationProcessResultAction)

                .and()
                .withExternal()
                .source(OnboardingState.ACTIVATION_FINISH_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE) // polling, because we don't get any event when the mobile app has finished the activation
                .guard(targetActivationFinishedGuard)
                .action(verificationProcessResultAction)
                .target(OnboardingState.CHOICE_COMPLETED_STATE);
    }

    private void configureCompletedTransition(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_COMPLETED_STATE)
                .first(OnboardingState.COMPLETED_ACCEPTED, VerificationProcessResultAction.isResultOk())
                .last(OnboardingState.COMPLETED_FAILED);
    }

}
