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

import com.wultra.app.onboardingserver.statemachine.action.InitPresenceCheckAction;
import com.wultra.app.onboardingserver.statemachine.action.InitVerificationAction;
import com.wultra.app.onboardingserver.statemachine.action.otp.OtpVerificationSendAction;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import com.wultra.app.onboardingserver.statemachine.guard.PresenceCheckEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.ProcessIdentifierGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationEnabledGuard;
import com.wultra.app.onboardingserver.statemachine.guard.otp.OtpVerificationPreconditionsGuard;
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

    private final InitPresenceCheckAction initPresenceCheckAction;

    private final InitVerificationAction initVerificationAction;

    private final OtpVerificationSendAction otpVerificationSendAction;

    private final OtpVerificationEnabledGuard otpVerificationEnabledGuard;

    private final OtpVerificationPreconditionsGuard otpVerificationPreconditionsGuard;

    private final PresenceCheckEnabledGuard presenceCheckEnabledGuard;

    private final ProcessIdentifierGuard processIdentifierGuard;

    public StateMachineConfig(
            final InitPresenceCheckAction initPresenceCheckAction,
            final InitVerificationAction initVerificationAction,
            final OtpVerificationSendAction otpVerificationSendAction,
            final OtpVerificationEnabledGuard otpVerificationEnabledGuard,
            final OtpVerificationPreconditionsGuard otpVerificationPreconditionsGuard,
            final PresenceCheckEnabledGuard presenceCheckEnabledGuard,
            final ProcessIdentifierGuard processIdentifierGuard) {
        this.initPresenceCheckAction = initPresenceCheckAction;
        this.initVerificationAction = initVerificationAction;
        this.otpVerificationSendAction = otpVerificationSendAction;

        this.otpVerificationEnabledGuard = otpVerificationEnabledGuard;
        this.otpVerificationPreconditionsGuard = otpVerificationPreconditionsGuard;
        this.presenceCheckEnabledGuard = presenceCheckEnabledGuard;
        this.processIdentifierGuard = processIdentifierGuard;
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
                .action(initVerificationAction)
                .target(EnrollmentState.DOCUMENT_UPLOAD_IN_PROGRESS)

                .and()
                .withExternal()
                .source(EnrollmentState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                .event(EnrollmentEvent.PRESENCE_CHECK_INIT)
                .guard(context ->
                        processIdentifierGuard.evaluate(context) &&
                        presenceCheckEnabledGuard.evaluate(context)
                )
                .action(initPresenceCheckAction)
                .target(EnrollmentState.PRESENCE_CHECK_IN_PROGRESS)

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
                .withExternal()
                .source(EnrollmentState.OTP_VERIFICATION_PENDING)
                .event(EnrollmentEvent.OTP_VERIFICATION_SEND)
                .guard(context ->
                        processIdentifierGuard.evaluate(context) &&
                        otpVerificationEnabledGuard.evaluate(context) &&
                        otpVerificationPreconditionsGuard.evaluate(context)
                )
                .action(otpVerificationSendAction)
                .target(EnrollmentState.OTP_VERIFICATION_PENDING)

                .and()
                .withExternal()
                .source(EnrollmentState.OTP_VERIFICATION_PENDING)
                .event(EnrollmentEvent.OTP_VERIFICATION_RESEND)
                .guard(context ->
                        processIdentifierGuard.evaluate(context) &&
                        otpVerificationEnabledGuard.evaluate(context) &&
                        otpVerificationPreconditionsGuard.evaluate(context)
                )
                .action(otpVerificationSendAction)
                .target(EnrollmentState.OTP_VERIFICATION_PENDING);
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
                logger.info("State changed to {}", to.getId());
            }

        };
    }

}
