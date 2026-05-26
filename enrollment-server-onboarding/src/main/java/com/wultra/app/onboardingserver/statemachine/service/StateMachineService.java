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
package com.wultra.app.onboardingserver.statemachine.service;

import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.EnrollmentStateProvider;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.interceptor.CustomStateMachineInterceptor;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * State machine service
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(value = "enrollment-server-onboarding.identity-verification.enabled", havingValue = "true")
public class StateMachineService {

    private final EnrollmentStateProvider enrollmentStateProvider;

    private final StateMachineFactory<OnboardingState, OnboardingEvent> stateMachineFactory;

    private final CustomStateMachineInterceptor stateMachineInterceptor;

    private final IdentityVerificationService identityVerificationService;

    private final OnboardingProcessRepository onboardingProcessRepository;

    @Transactional
    public StateMachine<OnboardingState, OnboardingEvent> processStateMachineEvent(OwnerId ownerId, String processId, OnboardingEvent event)
            throws IdentityVerificationException {
        return processStateMachineEventInternal(ownerId, processId, event);
    }

    private StateMachine<OnboardingState, OnboardingEvent> processStateMachineEventInternal(
            OwnerId ownerId,
            String processId,
            OnboardingEvent event
    ) throws IdentityVerificationException {
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine =
                OnboardingEvent.IDENTITY_VERIFICATION_INIT == event ?
                prepareStateMachine(processId, OnboardingState.INITIAL, null) :
                fetchStateMachine(ownerId, processId);
        final Message<OnboardingEvent> message = createMessage(ownerId, processId, event);
        sendEventMessage(stateMachine, message);

        return stateMachine;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean changeMachineState(final IdentityVerificationEntity identityVerification) {
        final var processId = identityVerification.getProcessId();
        logger.info("", kv("action", "changeMachineState"), kv("state", "initiated"), kv("processId", processId));

        final var ownerId = new OwnerId();
        ownerId.setActivationId(identityVerification.getActivationId());
        ownerId.setUserId(identityVerification.getUserId());

        try {
            lockAndVerifyProcess(processId);
            processStateMachineEventInternal(ownerId, processId, OnboardingEvent.EVENT_NEXT_STATE);
            logger.info("", kv("action", "changeMachineState"), kv("state", "succeeded"));
            return true;
        } catch (IdentityVerificationException e) {
            logger.warn("", kv("action", "changeMachineState"), kv("state", "failed"), kv("errorMessage", "Unable to change state for process"), e);
        } catch (final OnboardingProcessException e) {
            logger.warn("", kv("action", "changeMachineState"), kv("state", "failed"), kv("errorMessage", "Process not found"), e);
        } catch (final RuntimeException e) {
            logger.warn("", kv("action", "changeMachineState"), kv("state", "failed"), kv("errorMessage", "Exception when changing state of process"), e);
        }

        return false;
    }

    public StateMachine<OnboardingState, OnboardingEvent> prepareStateMachine(
            String processId,
            OnboardingState onboardingState,
            @Nullable IdentityVerificationEntity identityVerification
    ) {
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = stateMachineFactory.getStateMachine(processId);

        ExtendedState extendedState = new DefaultExtendedState();
        if (identityVerification != null) {
            extendedState.getVariables().put(ExtendedStateVariable.IDENTITY_VERIFICATION, identityVerification);
        }

        stateMachine.stopReactively().block();
        stateMachine.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(stateMachineInterceptor);
            sma.resetStateMachineReactively(
                    new DefaultStateMachineContext<>(
                            onboardingState,
                            null,
                            null,
                            extendedState
                    )
            ).block();
        });
        stateMachine.startReactively().block();

        return stateMachine;
    }

    public Message<OnboardingEvent> createMessage(OwnerId ownerId, String processId, OnboardingEvent event) {
        return MessageBuilder.withPayload(event)
                .setHeader(EventHeaderName.OWNER_ID, ownerId)
                .setHeader(EventHeaderName.PROCESS_ID, processId)
                .build();
    }

    private void sendEventMessage(
            StateMachine<OnboardingState, OnboardingEvent> stateMachine,
            Message<OnboardingEvent> message) {
        stateMachine.sendEvent(Mono.just(message)).blockLast();
    }

    private StateMachine<OnboardingState, OnboardingEvent> fetchStateMachine(
            OwnerId ownerId,
            String processId
    ) throws IdentityVerificationException {
        IdentityVerificationEntity identityVerification = identityVerificationService.findBy(ownerId);
        OnboardingState onboardingState = enrollmentStateProvider.findByPhaseAndStatus(identityVerification.getPhase(), identityVerification.getStatus());

        return prepareStateMachine(processId, onboardingState, identityVerification);
    }

    private void lockAndVerifyProcess(final String processId) throws OnboardingProcessException {
        onboardingProcessRepository.findByIdWithLock(processId)
                .filter(p -> OnboardingStatus.NOT_YET_COMPLETED.contains(p.getStatus()))
                .orElseThrow(() -> new OnboardingProcessException("Onboarding process not found for process ID: " + processId));
    }
}
