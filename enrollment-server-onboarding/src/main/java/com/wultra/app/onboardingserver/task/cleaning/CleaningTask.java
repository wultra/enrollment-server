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
package com.wultra.app.onboardingserver.task.cleaning;

import com.wultra.app.onboardingserver.task.consts.SchedulerLockNames;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task to clean expired processes, identity verifications, documents, activations and OTPs.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class CleaningTask {

    private final CleaningService cleaningService;

    private final ActivationCleaningService activationCleaningService;

    @Autowired
    public CleaningTask(final CleaningService cleaningService, final ActivationCleaningService activationCleaningService) {
        this.cleaningService = cleaningService;
        this.activationCleaningService = activationCleaningService;
    }

    /**
     * Terminate processes with activation in progress.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT10S")
    @SchedulerLock(name = SchedulerLockNames.ONBOARDING_PROCESS_LOCK, lockAtMostFor = "5m")
    public void terminateExpiredProcessActivations() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredProcessActivations");
        cleaningService.terminateExpiredProcessActivations();
    }

    /**
     * Terminate processes with verifications in progress.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT15S")
    @SchedulerLock(name = SchedulerLockNames.ONBOARDING_PROCESS_LOCK, lockAtMostFor = "5m")
    public void terminateProcessesWithVerificationsInProgress() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredProcessVerifications");
        cleaningService.terminateExpiredProcessVerifications();
    }

    /**
     * Terminate expired OTP codes.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT10S")
    @SchedulerLock(name = SchedulerLockNames.ONBOARDING_OTP_LOCK, lockAtMostFor = "5m")
    public void terminateExpiredOtpCodes() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredOtpCodes");
        cleaningService.terminateExpiredOtpCodes();
    }

    /**
     * Terminate expired processes.
     */
    @Scheduled(fixedDelayString = "PT15S", initialDelayString = "PT20S")
    @SchedulerLock(name = SchedulerLockNames.ONBOARDING_PROCESS_LOCK, lockAtMostFor = "5m")
    public void terminateExpiredProcesses() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredProcesses");
        cleaningService.terminateExpiredProcesses();
    }

    /**
     * Cleanup of large documents older than retention time.
     */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT15S")
    @SchedulerLock(name = SchedulerLockNames.LARGE_DOCUMENT_DATA_LOCK, lockAtMostFor = "5m")
    public void cleanupLargeDocuments() {
        LockAssert.assertLocked();
        logger.debug("cleanupLargeDocuments");
        cleaningService.cleanupLargeDocuments();
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT20S")
    @SchedulerLock(name = SchedulerLockNames.EXPIRE_DOCUMENT_VERIFICATION_LOCK, lockAtMostFor = "5m")
    public void terminateExpiredDocumentVerifications() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredDocumentVerifications");
        cleaningService.terminateExpiredDocumentVerifications();
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT25S")
    @SchedulerLock(name = SchedulerLockNames.ONBOARDING_PROCESS_LOCK, lockAtMostFor = "5m")
    public void terminateExpiredIdentityVerifications() {
        LockAssert.assertLocked();
        logger.debug("terminateExpiredIdentityVerifications");
        cleaningService.terminateExpiredIdentityVerifications();
    }

    /**
     * Cleanup activations of failed onboarding processes.
     */
    @Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT30S")
    @SchedulerLock(name = SchedulerLockNames.CLEANUP_ACTIVATIONS_LOCK, lockAtMostFor = "5m")
    public void cleanupActivations() {
        LockAssert.assertLocked();
        logger.debug("cleanupActivations");
        activationCleaningService.cleanupActivations();
    }
}
