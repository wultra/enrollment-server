/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.task;

import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task to change machine state.
 * <p>
 * Implemented as polling. Ideally, signaling should be done in the state machine itself.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
@ConditionalOnProperty(value = "enrollment-server-onboarding.identity-verification.enabled", havingValue = "true")
public class StateMachineTask {

    private final StateMachineService stateMachineService;

    public StateMachineTask(final StateMachineService stateMachineService) {
        this.stateMachineService = stateMachineService;
    }

    /**
     * Scheduled task to change machine state.
     */
    @Scheduled(cron = "${enrollment-server-onboarding.state-machine.changeMachineState.cron:0/3 * * * * *}", zone = "UTC")
    @SchedulerLock(name = "onboardingProcessLock", lockAtLeastFor = "100ms", lockAtMostFor = "5m")
    public void changeMachineState() {
        LockAssert.assertLocked();
        logger.debug("Changing machine states in batch");
        stateMachineService.changeMachineStatesInBatch();
    }
}
