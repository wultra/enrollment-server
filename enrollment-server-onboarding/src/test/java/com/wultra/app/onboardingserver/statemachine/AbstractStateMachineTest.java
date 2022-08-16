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

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;

import javax.annotation.Nullable;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
abstract class AbstractStateMachineTest {

    public static final String ACTIVATION_ID = "activationId";

    public static final OnboardingProcessEntity ONBOARDING_PROCESS_ENTITY = createOnboardingProcessEntity();

    public static final OwnerId OWNER_ID = createOwnerId();

    public static final String PROCESS_ID = "processId";

    @Autowired
    private EnrollmentStateProvider enrollmentStateProvider;

    @Autowired
    protected StateMachineService stateMachineService;

    protected AbstractStateMachineTest() {

    }

    protected StateMachineTestPlanBuilder<OnboardingState, OnboardingEvent>.StateMachineTestPlanStepBuilder prepareTest(StateMachine<OnboardingState, OnboardingEvent> stateMachine) {
        return StateMachineTestPlanBuilder.<OnboardingState, OnboardingEvent>builder()
                        .stateMachine(stateMachine)
                        .step();
    }

    protected StateMachine<OnboardingState, OnboardingEvent> createStateMachine(IdentityVerificationEntity entity) throws Exception {
        OnboardingState state = enrollmentStateProvider.findByPhaseAndStatus(entity.getPhase(), entity.getStatus());
        return stateMachineService.prepareStateMachine(entity.getProcessId(), state, entity);
    }

    protected IdentityVerificationEntity createIdentityVerification(
            @Nullable IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ACTIVATION_ID);
        entity.setProcessId(PROCESS_ID);
        entity.setPhase(phase);
        entity.setStatus(status);

        return entity;
    }

    protected static OnboardingProcessEntity createOnboardingProcessEntity() {
        OnboardingProcessEntity entity = new OnboardingProcessEntity();
        entity.setId(PROCESS_ID);
        return entity;
    }

    protected static OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        return ownerId;
    }

}
