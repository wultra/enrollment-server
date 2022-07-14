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
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.EnrollmentStateProvider;
import com.wultra.app.onboardingserver.statemachine.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import com.wultra.app.onboardingserver.statemachine.interceptor.CustomStateMachineInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * State machine service
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class StateMachineService {

    private final EnrollmentStateProvider enrollmentStateProvider;

    private final StateMachineFactory<EnrollmentState, EnrollmentEvent> stateMachineFactory;

    private final CustomStateMachineInterceptor stateMachineInterceptor;

    private final IdentityVerificationService identityVerificationService;

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
            final StateMachineFactory<EnrollmentState, EnrollmentEvent> stateMachineFactory,
            final CustomStateMachineInterceptor stateMachineInterceptor,
            IdentityVerificationService identityVerificationService
    ) {
        this.enrollmentStateProvider = enrollmentStateProvider;
        this.stateMachineFactory = stateMachineFactory;
        this.stateMachineInterceptor = stateMachineInterceptor;
        this.identityVerificationService = identityVerificationService;
    }

    public StateMachine<EnrollmentState, EnrollmentEvent> processStateMachineEvent(OwnerId ownerId, String processId, EnrollmentEvent event)
            throws IdentityVerificationException {
        final StateMachine<EnrollmentState, EnrollmentEvent> stateMachine =
                EnrollmentEvent.IDENTITY_VERIFICATION_INIT == event ?
                prepareStateMachine(processId, EnrollmentState.INITIAL) :
                fetchStateMachine(ownerId, processId);
        final Message<EnrollmentEvent> message = createMessage(ownerId, processId, event);
        sendEventMessage(stateMachine, message);

        return stateMachine;
    }

    private StateMachine<EnrollmentState, EnrollmentEvent> prepareStateMachine(
            String processId,
            EnrollmentState enrollmentState
    ) {
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = stateMachineFactory.getStateMachine(processId);

        stateMachine.stopReactively().block();
        stateMachine.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(stateMachineInterceptor);
            sma.resetStateMachineReactively(
                    new DefaultStateMachineContext<>(enrollmentState, null, null, null) // stateMachine.getExtendedState()
            );
        });
        stateMachine.startReactively().block();

        return stateMachine;
    }

    private Message<EnrollmentEvent> createMessage(OwnerId ownerId, String processId, EnrollmentEvent event) {
        return MessageBuilder.withPayload(event)
                .setHeader(EventHeaderName.OWNER_ID, ownerId)
                .setHeader(EventHeaderName.PROCESS_ID, processId)
                .build();
    }

    private StateMachineEventResult<EnrollmentState, EnrollmentEvent> sendEventMessage(
            StateMachine<EnrollmentState, EnrollmentEvent> stateMachine,
            Message<EnrollmentEvent> message) {
        return stateMachine.sendEvent(Mono.just(message)).blockLast();
    }

    private StateMachine<EnrollmentState, EnrollmentEvent> fetchStateMachine(
            OwnerId ownerId,
            String processId
    ) throws IdentityVerificationException {
        IdentityVerificationEntity identityVerification = identityVerificationService.findBy(ownerId);
        EnrollmentState enrollmentState = enrollmentStateProvider.findByPhaseAndStatus(identityVerification.getPhase(), identityVerification.getStatus());

        return prepareStateMachine(processId, enrollmentState);
    }

}
