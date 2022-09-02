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

import com.wultra.app.onboardingserver.impl.service.OnboardingServiceImpl;
import com.wultra.app.onboardingserver.impl.service.document.DocumentStatusService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task to clean expired processes, identity verifications and documents.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class CleaningTask {

    private final OnboardingServiceImpl onboardingService;

    private final DocumentStatusService documentStatusService;

    @Autowired
    public CleaningTask(final OnboardingServiceImpl onboardingService, final DocumentStatusService documentStatusService) {
        this.onboardingService = onboardingService;
        this.documentStatusService = documentStatusService;
    }

    /**
     * Terminate inactive process.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    @SchedulerLock(name = "terminateInactiveProcesses", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void terminateInactiveProcesses() {
        LockAssert.assertLocked();
        logger.debug("terminateInactiveProcesses");
        onboardingService.terminateInactiveProcesses();
    }

    /**
     * Terminate processes with verifications in progress.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    @SchedulerLock(name = "terminateProcessesWithVerificationsInProgress", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void terminateProcessesWithVerificationsInProgress() {
        LockAssert.assertLocked();
        logger.debug("terminateProcessesWithVerificationsInProgress");
        onboardingService.terminateProcessesWithVerificationsInProgress();
    }

    /**
     * Terminate OTP codes for all processes.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    @SchedulerLock(name = "terminateOtpCodesForAllProcesses", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void terminateOtpCodesForAllProcesses() {
        LockAssert.assertLocked();
        logger.debug("terminateOtpCodesForAllProcesses");
        onboardingService.terminateOtpCodesForAllProcesses();
    }

    /**
     * Terminate expired processes.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    @SchedulerLock(name = "terminateExpiredProcesses", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void terminateExpiredProcesses() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredProcesses");
        onboardingService.terminateExpiredProcesses();
    }

    /**
     * Cleanup of large documents older than retention time.
     */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    @SchedulerLock(name = "cleanupLargeDocuments", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void cleanupLargeDocuments() {
        LockAssert.assertLocked();
        logger.debug("cleanupLargeDocuments");
        documentStatusService.cleanupLargeDocuments();
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    @SchedulerLock(name = "cleanupExpiredVerificationProcesses", lockAtLeastFor = "1s", lockAtMostFor = "5m")
    public void cleanupExpiredVerificationProcesses() {
        LockAssert.assertLocked();
        logger.debug("cleanupExpiredVerificationProcesses");
        documentStatusService.cleanupExpiredVerificationProcesses();
    }

}
