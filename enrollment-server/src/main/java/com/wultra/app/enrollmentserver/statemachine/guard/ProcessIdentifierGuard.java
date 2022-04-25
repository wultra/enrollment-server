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
package com.wultra.app.enrollmentserver.statemachine.guard;

import com.wultra.app.enrollmentserver.database.OnboardingProcessRepository;
import com.wultra.app.enrollmentserver.database.entity.OnboardingProcessEntity;
import com.wultra.app.enrollmentserver.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.statemachine.EventHeaderName;
import com.wultra.app.enrollmentserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.enrollmentserver.statemachine.enums.EnrollmentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Guard to ensure valid process identifier
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class ProcessIdentifierGuard implements Guard<EnrollmentState, EnrollmentEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessIdentifierGuard.class);

    private final OnboardingProcessRepository onboardingProcessRepository;

    public ProcessIdentifierGuard(OnboardingProcessRepository onboardingProcessRepository) {
        this.onboardingProcessRepository = onboardingProcessRepository;
    }

    /**
     * Verifies process identifier.
     */
    @Override
    public boolean evaluate(StateContext<EnrollmentState, EnrollmentEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        Optional<OnboardingProcessEntity> processOptional = onboardingProcessRepository.findProcessByActivationId(ownerId.getActivationId());
        if (!processOptional.isPresent()) {
            logger.error("Onboarding process not found, {}", ownerId);
            context.getStateMachine().setStateMachineError(new OnboardingProcessException());
            return false;
        }
        String expectedProcessId = processOptional.get().getId();

        if (!expectedProcessId.equals(processId)) {
            logger.warn("Invalid process ID received in request: {}, {}", processId, ownerId);
            context.getStateMachine().setStateMachineError(new OnboardingProcessException());
            return false;
        }
        return true;
    }

}
