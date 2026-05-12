/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wultra.app.onboardingserver.statemachine.service;

import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for changing state machine states in batch.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(value = "enrollment-server-onboarding.identity-verification.enabled", havingValue = "true")
public class StateMachineBatchService {

    private final StateMachineService stateMachineService;

    private final IdentityVerificationService identityVerificationService;

    /**
     * Change machine states in batch.
     */
    public void changeMachineStatesInBatch() {
        final var countFinished = identityVerificationService.findAllIdentityVerificationsToChangeState()
                .parallelStream()
                .map(stateMachineService::changeMachineState)
                .filter(result -> result)
                .count();

        if (countFinished > 0) {
            logger.debug("Changed state of {} identity verifications", countFinished);
        }
    }
}
