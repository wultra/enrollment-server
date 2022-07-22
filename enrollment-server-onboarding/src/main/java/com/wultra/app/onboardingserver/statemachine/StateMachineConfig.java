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

import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationResendAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationSendAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckInitAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckNotInitializedAction;
import com.wultra.app.onboardingserver.statemachine.action.presencecheck.PresenceCheckVerificationAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationCheckIdentityDocumentsAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationDocumentStartAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationInitAction;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import com.wultra.app.onboardingserver.statemachine.guard.PresenceCheckEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.ProcessIdentifierGuard;
import com.wultra.app.onboardingserver.statemachine.guard.document.DocumentUploadVerificationInProgressGuard;
import com.wultra.app.onboardingserver.statemachine.guard.document.DocumentUploadVerificationPendingGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationPreconditionsGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerifiedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusFailedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusInProgressGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusRejectedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusVerificationPendingGuard;
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
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<EnrollmentState, EnrollmentEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StateMachineConfig.class);

    private final OtpVerificationResendAction otpVerificationResendAction;

    private final OtpVerificationSendAction otpVerificationSendAction;

    private final PresenceCheckInitAction presenceCheckInitAction;

    private final PresenceCheckNotInitializedAction presenceCheckNotInitializedAction;

    private final PresenceCheckVerificationAction presenceCheckVerificationAction;

    private final VerificationCheckIdentityDocumentsAction verificationCheckIdentityDocumentsAction;

    private final VerificationDocumentStartAction verificationDocumentStartAction;

    private final VerificationInitAction verificationInitAction;

    private final VerificationProcessResultAction verificationProcessResultAction;

    private final DocumentUploadVerificationInProgressGuard documentUploadVerificationInProgressGuard;

    private final DocumentUploadVerificationPendingGuard documentUploadVerificationPendingGuard;

    private final OtpVerificationEnabledGuard otpVerificationEnabledGuard;

    private final OtpVerificationPreconditionsGuard otpVerificationPreconditionsGuard;

    private final OtpVerifiedGuard otpVerifiedGuard;

    private final PresenceCheckEnabledGuard presenceCheckEnabledGuard;

    private final ProcessIdentifierGuard processIdentifierGuard;

    private final StatusFailedGuard statusFailedGuard;

    private final StatusInProgressGuard statusInProgressGuard;

    private final StatusRejectedGuard statusRejectedGuard;

    private final StatusVerificationPendingGuard statusVerificationPendingGuard;

    public StateMachineConfig(
            final OtpVerificationResendAction otpVerificationResendAction,
            final OtpVerificationSendAction otpVerificationSendAction,
            final PresenceCheckInitAction presenceCheckInitAction,
            final PresenceCheckNotInitializedAction presenceCheckNotInitializedAction,
            final PresenceCheckVerificationAction presenceCheckVerificationAction,
            final VerificationCheckIdentityDocumentsAction verificationCheckIdentityDocumentsAction,
            final VerificationDocumentStartAction verificationDocumentStartAction,
            final VerificationInitAction verificationInitAction,
            final VerificationProcessResultAction verificationProcessResultAction,
            final DocumentUploadVerificationInProgressGuard documentUploadVerificationInProgressGuard,
            final DocumentUploadVerificationPendingGuard documentUploadVerificationPendingGuard,
            final OtpVerificationEnabledGuard otpVerificationEnabledGuard,
            final OtpVerificationPreconditionsGuard otpVerificationPreconditionsGuard,
            final OtpVerifiedGuard otpVerifiedGuard,
            final PresenceCheckEnabledGuard presenceCheckEnabledGuard,
            final ProcessIdentifierGuard processIdentifierGuard,
            final StatusFailedGuard statusFailedGuard,
            final StatusInProgressGuard statusInProgressGuard,
            final StatusRejectedGuard statusRejectedGuard,
            final StatusVerificationPendingGuard statusVerificationPendingGuard) {
        this.otpVerificationResendAction = otpVerificationResendAction;
        this.otpVerificationSendAction = otpVerificationSendAction;

        this.presenceCheckInitAction = presenceCheckInitAction;
        this.presenceCheckNotInitializedAction = presenceCheckNotInitializedAction;
        this.presenceCheckVerificationAction = presenceCheckVerificationAction;

        this.verificationCheckIdentityDocumentsAction = verificationCheckIdentityDocumentsAction;
        this.verificationDocumentStartAction = verificationDocumentStartAction;
        this.verificationInitAction = verificationInitAction;
        this.verificationProcessResultAction = verificationProcessResultAction;

        this.documentUploadVerificationInProgressGuard = documentUploadVerificationInProgressGuard;
        this.documentUploadVerificationPendingGuard = documentUploadVerificationPendingGuard;

        this.otpVerificationEnabledGuard = otpVerificationEnabledGuard;
        this.otpVerificationPreconditionsGuard = otpVerificationPreconditionsGuard;
        this.otpVerifiedGuard = otpVerifiedGuard;

        this.presenceCheckEnabledGuard = presenceCheckEnabledGuard;

        this.processIdentifierGuard = processIdentifierGuard;

        this.statusFailedGuard = statusFailedGuard;
        this.statusInProgressGuard = statusInProgressGuard;
        this.statusRejectedGuard = statusRejectedGuard;
        this.statusVerificationPendingGuard = statusVerificationPendingGuard;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<EnrollmentState, EnrollmentEvent> config) throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<EnrollmentState, EnrollmentEvent> states) throws Exception {
        states
                .withStates()
                .initial(EnrollmentState.INITIAL)
                .choice(EnrollmentState.CHOICE_DOCUMENT_UPLOAD)
                .choice(EnrollmentState.CHOICE_DOCUMENT_VERIFICATION_ACCEPTED)
                .choice(EnrollmentState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)
                .choice(EnrollmentState.CHOICE_OTP_VERIFICATION)
                .choice(EnrollmentState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .choice(EnrollmentState.CHOICE_VERIFICATION_PROCESSING)
                .choice(EnrollmentState.PRESENCE_CHECK_VERIFICATION_PENDING)
                .end(EnrollmentState.COMPLETED_ACCEPTED)
                .end(EnrollmentState.COMPLETED_FAILED)
                .end(EnrollmentState.COMPLETED_REJECTED)
                .states(EnumSet.allOf(EnrollmentState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<EnrollmentState, EnrollmentEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(EnrollmentState.INITIAL)
                .event(EnrollmentEvent.IDENTITY_VERIFICATION_INIT)
                .guard(processIdentifierGuard)
                .action(verificationInitAction)
                .target(EnrollmentState.DOCUMENT_UPLOAD_IN_PROGRESS)


                .and()
                .withExternal()
                .source(EnrollmentState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                .event(EnrollmentEvent.EVENT_NEXT_STATE)
                .guard(context ->
                        processIdentifierGuard.evaluate(context)
                )
                .action(verificationDocumentStartAction)
                .target(EnrollmentState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)

                .and()
                .withChoice()
                .source(EnrollmentState.CHOICE_DOCUMENT_VERIFICATION_PROCESSING)
                .first(EnrollmentState.DOCUMENT_VERIFICATION_IN_PROGRESS,  documentUploadVerificationInProgressGuard)
                .last(EnrollmentState.DOCUMENT_VERIFICATION_FAILED)

                .and()
                .withExternal()
                .source(EnrollmentState.DOCUMENT_VERIFICATION_ACCEPTED)
                .event(EnrollmentEvent.EVENT_NEXT_STATE)
                .target(EnrollmentState.CHOICE_DOCUMENT_VERIFICATION_ACCEPTED)

                .and()
                .withChoice()
                .source(EnrollmentState.CHOICE_DOCUMENT_VERIFICATION_ACCEPTED)
                .first(EnrollmentState.PRESENCE_CHECK_NOT_INITIALIZED, presenceCheckEnabledGuard, presenceCheckNotInitializedAction)
                .last(EnrollmentState.PRESENCE_CHECK_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(EnrollmentState.DOCUMENT_UPLOAD_IN_PROGRESS)
                .event(EnrollmentEvent.EVENT_NEXT_STATE)
                .action(verificationCheckIdentityDocumentsAction)
                .target(EnrollmentState.CHOICE_DOCUMENT_UPLOAD)

                .and()
                .withChoice()
                .source(EnrollmentState.CHOICE_DOCUMENT_UPLOAD)
                .first(EnrollmentState.DOCUMENT_UPLOAD_VERIFICATION_PENDING, documentUploadVerificationPendingGuard)
                .last(EnrollmentState.DOCUMENT_UPLOAD_IN_PROGRESS)

                .and()
                .withExternal()
                .source(EnrollmentState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                .event(EnrollmentEvent.PRESENCE_CHECK_INIT)
                .guard(context ->
                        processIdentifierGuard.evaluate(context) &&
                        presenceCheckEnabledGuard.evaluate(context)
                )
                .action(presenceCheckInitAction)
                .target(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)

                .and()
                .withExternal()
                .source(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                .event(EnrollmentEvent.EVENT_NEXT_STATE)
                .action(presenceCheckVerificationAction)
                .target(EnrollmentState.CHOICE_PRESENCE_CHECK_PROCESSING)

                .and()
                .withChoice()
                .source(EnrollmentState.CHOICE_PRESENCE_CHECK_PROCESSING)
                .first(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS, statusInProgressGuard)
                .then(EnrollmentState.PRESENCE_CHECK_VERIFICATION_PENDING, statusVerificationPendingGuard)
                .then(EnrollmentState.PRESENCE_CHECK_REJECTED, statusRejectedGuard)
                .last(EnrollmentState.PRESENCE_CHECK_FAILED)

                .and()
                .withExternal()
                .source(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                .event(EnrollmentEvent.PRESENCE_CHECK_ACCEPTED)
                .guard(context ->
                        presenceCheckEnabledGuard.evaluate(context) &&
                        otpVerificationEnabledGuard.evaluate(context)
                )
                .target(EnrollmentState.OTP_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                .event(EnrollmentEvent.PRESENCE_CHECK_ACCEPTED)
                .guard(context ->
                        presenceCheckEnabledGuard.evaluate(context) &&
                        !otpVerificationEnabledGuard.evaluate(context)
                )
                .target(EnrollmentState.COMPLETED_ACCEPTED)

                .and()
                .withExternal()
                .source(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                .event(EnrollmentEvent.PRESENCE_CHECK_FAILED)
                .target(EnrollmentState.PRESENCE_CHECK_FAILED)

                .and()
                .withExternal()
                .source(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)
                .event(EnrollmentEvent.PRESENCE_CHECK_REJECTED)
                .target(EnrollmentState.PRESENCE_CHECK_REJECTED)

                .and()
                .withChoice()
                .source(EnrollmentState.PRESENCE_CHECK_VERIFICATION_PENDING)
                .first(EnrollmentState.OTP_VERIFICATION_PENDING, otpVerificationEnabledGuard, otpVerificationSendAction)
                .last(EnrollmentState.CHOICE_VERIFICATION_PROCESSING, verificationProcessResultAction)

                .and()
                .withExternal()
                .source(EnrollmentState.OTP_VERIFICATION_PENDING)
                .event(EnrollmentEvent.OTP_VERIFICATION_RESEND)
                .guard(context ->
                        processIdentifierGuard.evaluate(context) &&
                        otpVerificationEnabledGuard.evaluate(context) &&
                        otpVerificationPreconditionsGuard.evaluate(context)
                )
                .action(otpVerificationResendAction)
                .target(EnrollmentState.OTP_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(EnrollmentState.OTP_VERIFICATION_PENDING)
                .event(EnrollmentEvent.EVENT_NEXT_STATE)
                .target(EnrollmentState.CHOICE_OTP_VERIFICATION)

                .and()
                .withChoice()
                .source(EnrollmentState.CHOICE_OTP_VERIFICATION)
                .first(EnrollmentState.CHOICE_VERIFICATION_PROCESSING, otpVerifiedGuard, verificationProcessResultAction)
                .last(EnrollmentState.OTP_VERIFICATION_PENDING);

    }

    @Bean
    public StateMachineListener<EnrollmentState, EnrollmentEvent> listener() {
        return new StateMachineListenerAdapter<>() {

            @Override
            public void eventNotAccepted(Message<EnrollmentEvent> event) {
                logger.warn("Not accepted event {}", event.getPayload());
                // TODO
                // throw new OnboardingProcessException("Unexpected state of identity verification");
            }

            @Override
            public void stateChanged(State<EnrollmentState, EnrollmentEvent> from, State<EnrollmentState, EnrollmentEvent> to) {
                logger.debug("State changed from {} to {}", from.getId(), to.getId());
            }

        };
    }

}
