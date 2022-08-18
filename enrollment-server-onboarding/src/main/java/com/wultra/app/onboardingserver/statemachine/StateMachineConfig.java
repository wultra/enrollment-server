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

import com.wultra.app.onboardingserver.statemachine.action.clientevaluation.ClientEvaluationInitAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationResendAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationSendAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckInitAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckNotInitializedAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckVerificationAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationCheckIdentityDocumentsAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationDocumentStartAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationInitAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.guard.PresenceCheckEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.ProcessIdentifierGuard;
import com.wultra.app.onboardingserver.statemachine.guard.document.DocumentUploadVerificationPendingGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerifiedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusAcceptedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusFailedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusInProgressGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusRejectedGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

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
@EnableStateMachineFactory(name = "enrollmentStateMachine")
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OnboardingState, OnboardingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StateMachineConfig.class);

    private final ClientEvaluationInitAction clientEvaluationInitAction;

    private final OtpVerificationResendAction otpVerificationResendAction;

    private final OtpVerificationSendAction otpVerificationSendAction;

    private final PresenceCheckInitAction presenceCheckInitAction;

    private final PresenceCheckNotInitializedAction presenceCheckNotInitializedAction;

    private final PresenceCheckVerificationAction presenceCheckVerificationAction;

    private final VerificationCheckIdentityDocumentsAction verificationCheckIdentityDocumentsAction;

    private final VerificationDocumentStartAction verificationDocumentStartAction;

    private final VerificationInitAction verificationInitAction;

    private final VerificationProcessResultAction verificationProcessResultAction;

    private final DocumentUploadVerificationPendingGuard documentUploadVerificationPendingGuard;

    private final OtpVerificationEnabledGuard otpVerificationEnabledGuard;

    private final OtpVerifiedGuard otpVerifiedGuard;

    private final PresenceCheckEnabledGuard presenceCheckEnabledGuard;

    private final ProcessIdentifierGuard processIdentifierGuard;

    private final StatusAcceptedGuard statusAcceptedGuard;

    private final StatusFailedGuard statusFailedGuard;

    private final StatusInProgressGuard statusInProgressGuard;

    private final StatusRejectedGuard statusRejectedGuard;

    public StateMachineConfig(
            final ClientEvaluationInitAction clientEvaluationInitAction,
            final OtpVerificationResendAction otpVerificationResendAction,
            final OtpVerificationSendAction otpVerificationSendAction,
            final PresenceCheckInitAction presenceCheckInitAction,
            final PresenceCheckNotInitializedAction presenceCheckNotInitializedAction,
            final PresenceCheckVerificationAction presenceCheckVerificationAction,
            final VerificationCheckIdentityDocumentsAction verificationCheckIdentityDocumentsAction,
            final VerificationDocumentStartAction verificationDocumentStartAction,
            final VerificationInitAction verificationInitAction,
            final VerificationProcessResultAction verificationProcessResultAction,
            final DocumentUploadVerificationPendingGuard documentUploadVerificationPendingGuard,
            final OtpVerificationEnabledGuard otpVerificationEnabledGuard,
            final OtpVerifiedGuard otpVerifiedGuard,
            final PresenceCheckEnabledGuard presenceCheckEnabledGuard,
            final ProcessIdentifierGuard processIdentifierGuard,
            final StatusAcceptedGuard statusAcceptedGuard,
            final StatusFailedGuard statusFailedGuard,
            final StatusInProgressGuard statusInProgressGuard,
            final StatusRejectedGuard statusRejectedGuard) {
        this.clientEvaluationInitAction = clientEvaluationInitAction;
        this.otpVerificationResendAction = otpVerificationResendAction;
        this.otpVerificationSendAction = otpVerificationSendAction;

        this.presenceCheckInitAction = presenceCheckInitAction;
        this.presenceCheckNotInitializedAction = presenceCheckNotInitializedAction;
        this.presenceCheckVerificationAction = presenceCheckVerificationAction;

        this.verificationCheckIdentityDocumentsAction = verificationCheckIdentityDocumentsAction;
        this.verificationDocumentStartAction = verificationDocumentStartAction;
        this.verificationInitAction = verificationInitAction;
        this.verificationProcessResultAction = verificationProcessResultAction;

        this.documentUploadVerificationPendingGuard = documentUploadVerificationPendingGuard;

        this.otpVerificationEnabledGuard = otpVerificationEnabledGuard;
        this.otpVerifiedGuard = otpVerifiedGuard;

        this.presenceCheckEnabledGuard = presenceCheckEnabledGuard;

        this.processIdentifierGuard = processIdentifierGuard;

        this.statusAcceptedGuard = statusAcceptedGuard;
        this.statusFailedGuard = statusFailedGuard;
        this.statusInProgressGuard = statusInProgressGuard;
        this.statusRejectedGuard = statusRejectedGuard;
    }

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
                .choice(OnboardingState.CHOICE_OTP_VERIFICATION)
                .choice(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .choice(OnboardingState.CHOICE_VERIFICATION_PROCESSING)
                .choice(OnboardingState.PRESENCE_CHECK_FAILED)
                .choice(OnboardingState.PRESENCE_CHECK_REJECTED)
                .end(OnboardingState.CLIENT_EVALUATION_FAILED)
                .end(OnboardingState.CLIENT_EVALUATION_REJECTED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_FAILED)
                .end(OnboardingState.DOCUMENT_VERIFICATION_REJECTED)
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
        configureClientEvaluationTransitions(transitions);
        configurePresenceCheckTransitions(transitions);
        configureOtpTransitions(transitions);
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
                .action(verificationCheckIdentityDocumentsAction)
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
                .action(clientEvaluationInitAction)
                .target(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS);
    }

    private void configureClientEvaluationTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
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
                .target(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_CLIENT_EVALUATION_ACCEPTED)
                .first(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED, presenceCheckEnabledGuard, presenceCheckNotInitializedAction)
                .then(OnboardingState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction)
                .last(OnboardingState.CHOICE_VERIFICATION_PROCESSING, verificationProcessResultAction);
    }

    private void configurePresenceCheckTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_NOT_INITIALIZED)
                .event(OnboardingEvent.PRESENCE_CHECK_INIT)
                .guard(
                        context -> processIdentifierGuard.evaluate(context) && presenceCheckEnabledGuard.evaluate(context)
                )
                .action(presenceCheckInitAction)
                .target(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)

                .and()
                .withExternal()
                .source(OnboardingState.PRESENCE_CHECK_IN_PROGRESS)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .action(presenceCheckVerificationAction)
                .target(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .first(OnboardingState.PRESENCE_CHECK_IN_PROGRESS, statusInProgressGuard)
                .then(OnboardingState.OTP_VERIFICATION_PENDING,
                        context -> otpVerificationEnabledGuard.evaluate(context) && statusAcceptedGuard.evaluate(context),
                        otpVerificationSendAction
                )
                .then(OnboardingState.CHOICE_VERIFICATION_PROCESSING,
                        context -> !otpVerificationEnabledGuard.evaluate(context) && statusAcceptedGuard.evaluate(context),
                        verificationProcessResultAction
                )
                .then(OnboardingState.PRESENCE_CHECK_REJECTED, statusRejectedGuard)
                .then(OnboardingState.PRESENCE_CHECK_FAILED, statusFailedGuard)
                .last(OnboardingState.UNEXPECTED_STATE)

                .and()
                .withChoice()
                .source(OnboardingState.PRESENCE_CHECK_REJECTED)
                .first(OnboardingState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction)
                .last(OnboardingState.CHOICE_VERIFICATION_PROCESSING, verificationProcessResultAction)

                .and()
                .withChoice()
                .source(OnboardingState.PRESENCE_CHECK_FAILED)
                .first(OnboardingState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction)
                .last(OnboardingState.CHOICE_VERIFICATION_PROCESSING, verificationProcessResultAction);
    }

    private void configureOtpTransitions(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OnboardingState.OTP_VERIFICATION_PENDING)
                .event(OnboardingEvent.OTP_VERIFICATION_RESEND)
                .guard(
                        context -> processIdentifierGuard.evaluate(context) && otpVerificationEnabledGuard.evaluate(context)
                )
                .action(otpVerificationResendAction)
                .target(OnboardingState.OTP_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(OnboardingState.OTP_VERIFICATION_PENDING)
                .event(OnboardingEvent.EVENT_NEXT_STATE)
                .target(OnboardingState.CHOICE_OTP_VERIFICATION)

                .and()
                .withChoice()
                .source(OnboardingState.CHOICE_OTP_VERIFICATION)
                .first(OnboardingState.CHOICE_VERIFICATION_PROCESSING, otpVerifiedGuard, verificationProcessResultAction)
                .last(OnboardingState.OTP_VERIFICATION_PENDING);
    }

    private void configureCompletedTransition(StateMachineTransitionConfigurer<OnboardingState, OnboardingEvent> transitions) throws Exception {
        transitions
                .withChoice()
                .source(OnboardingState.CHOICE_VERIFICATION_PROCESSING)
                .first(OnboardingState.COMPLETED_ACCEPTED, statusAcceptedGuard)
                .then(OnboardingState.COMPLETED_REJECTED, statusRejectedGuard)
                .then(OnboardingState.COMPLETED_FAILED, statusFailedGuard)
                .last(OnboardingState.UNEXPECTED_STATE);
    }

}
