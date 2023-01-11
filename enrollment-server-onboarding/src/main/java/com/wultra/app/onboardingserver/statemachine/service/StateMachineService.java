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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.EnrollmentStateProvider;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.interceptor.CustomStateMachineInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * State machine service
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
@Slf4j
@ConditionalOnProperty(value = "enrollment-server-onboarding.identity-verification.enabled", havingValue = "true")
public class StateMachineService {

    private final EnrollmentStateProvider enrollmentStateProvider;

    private final StateMachineFactory<OnboardingState, OnboardingEvent> stateMachineFactory;

    private final CustomStateMachineInterceptor stateMachineInterceptor;

    private final IdentityVerificationService identityVerificationService;

    private final TransactionTemplate transactionTemplate;

    /**
     * Constructor.
     *
     * @param enrollmentStateProvider     Enrollment state provider.
     * @param stateMachineFactory         State machine factory.
     * @param stateMachineInterceptor     State machine interceptor.
     * @param identityVerificationService Identity verification service.
     */
    public StateMachineService(
            final EnrollmentStateProvider enrollmentStateProvider,
            final StateMachineFactory<OnboardingState, OnboardingEvent> stateMachineFactory,
            final CustomStateMachineInterceptor stateMachineInterceptor,
            final IdentityVerificationService identityVerificationService,
            final TransactionTemplate transactionTemplate
    ) {
        this.enrollmentStateProvider = enrollmentStateProvider;
        this.stateMachineFactory = stateMachineFactory;
        this.stateMachineInterceptor = stateMachineInterceptor;
        this.identityVerificationService = identityVerificationService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public StateMachine<OnboardingState, OnboardingEvent> processStateMachineEvent(OwnerId ownerId, String processId, OnboardingEvent event)
            throws IdentityVerificationException {
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine =
                OnboardingEvent.IDENTITY_VERIFICATION_INIT == event ?
                prepareStateMachine(processId, OnboardingState.INITIAL, null) :
                fetchStateMachine(ownerId, processId);
        final Message<OnboardingEvent> message = createMessage(ownerId, processId, event);
        sendEventMessage(stateMachine, message);

        return stateMachine;
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

    /**
     * Change machine states in batch.
     */
    @Transactional(readOnly = true)
    public void changeMachineStatesInBatch() {
        final AtomicInteger countFinished = new AtomicInteger(0);
        try (Stream<IdentityVerificationEntity> stream = identityVerificationService.streamAllIdentityVerificationsToChangeState().parallel()) {
            stream.forEach(identityVerification -> {
                final String processId = identityVerification.getProcessId();
                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(identityVerification.getActivationId());
                ownerId.setUserId(identityVerification.getUserId());
                logger.debug("Changing state of machine for process ID: {}", processId);

                transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transactionTemplate.executeWithoutResult(status -> {
                    try {
                        processStateMachineEvent(ownerId, processId, OnboardingEvent.EVENT_NEXT_STATE);
                        countFinished.incrementAndGet();
                    } catch (IdentityVerificationException e) {
                        logger.warn("Unable to change state for process ID: {}", processId, e);
                    }
                });
            });
        }
        if (countFinished.get() > 0) {
            logger.debug("Changed state of {} identity verifications", countFinished.get());
        }
    }

    private StateMachineEventResult<OnboardingState, OnboardingEvent> sendEventMessage(
            StateMachine<OnboardingState, OnboardingEvent> stateMachine,
            Message<OnboardingEvent> message) {
        return stateMachine.sendEvent(Mono.just(message)).blockLast();
    }

    private StateMachine<OnboardingState, OnboardingEvent> fetchStateMachine(
            OwnerId ownerId,
            String processId
    ) throws IdentityVerificationException {
        IdentityVerificationEntity identityVerification = identityVerificationService.findBy(ownerId);
        OnboardingState onboardingState = enrollmentStateProvider.findByPhaseAndStatus(identityVerification.getPhase(), identityVerification.getStatus());

        return prepareStateMachine(processId, onboardingState, identityVerification);
    }

}
