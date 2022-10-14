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

import com.wultra.app.onboardingserver.impl.service.verification.VerificationProcessingBatchService;
import com.wultra.app.onboardingserver.task.consts.SchedulerLockNames;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task to check document submit verification result with the provider.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
@Slf4j
public class DocumentSubmitVerificationSyncTask {

    private final VerificationProcessingBatchService verificationProcessingBatchService;

    public DocumentSubmitVerificationSyncTask(VerificationProcessingBatchService verificationProcessingBatchService) {
        this.verificationProcessingBatchService = verificationProcessingBatchService;
    }

    /**
     * Scheduled task to check document submit verifications at the target provider
     */
    @Scheduled(cron = "${enrollment-server-onboarding.document-verification.checkDocumentSubmitVerifications.cron:0/5 * * * * *}", zone = "UTC")
    @SchedulerLock(name = SchedulerLockNames.DOCUMENT_SUBMIT_VERIFICATION_LOCK, lockAtMostFor = "5m")
    public void checkDocumentSubmitVerifications() {
        LockAssert.assertLocked();
        logger.debug("checkDocumentSubmitVerifications");
        verificationProcessingBatchService.checkDocumentSubmitVerifications();
    }

}
