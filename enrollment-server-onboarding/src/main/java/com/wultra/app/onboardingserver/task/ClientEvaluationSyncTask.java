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

import com.wultra.app.onboardingserver.impl.service.ClientEvaluationService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task to process client evaluation result with the provider.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
public class ClientEvaluationSyncTask {

    private final ClientEvaluationService clientEvaluationService;

    public ClientEvaluationSyncTask(final ClientEvaluationService clientEvaluationService) {
        this.clientEvaluationService = clientEvaluationService;
    }

    /**
     * Scheduled task to process client evaluations at the target provider.
     */
    @Scheduled(cron = "${enrollment-server-onboarding.client-evaluation.processClientEvaluations.cron:0/5 * * * * *}", zone = "UTC")
    @SchedulerLock(name = "processClientEvaluations", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void processClientEvaluations() {
        clientEvaluationService.processClientEvaluations();
    }
}
