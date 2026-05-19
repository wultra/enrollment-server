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
package com.wultra.app.onboardingserver.task.cleaning;

import com.wultra.app.onboardingserver.impl.util.ConditionalOnPropertyNotEmpty;
import com.wultra.app.onboardingserver.task.consts.SchedulerLockNames;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Task to clean personal data stored during an onboarding process.
 */
@Component
@Slf4j
@AllArgsConstructor
@ConditionalOnPropertyNotEmpty("enrollment-server-onboarding.identity-verification.data-retention")
public class PersonalDataCleaningTask {

	private final CleaningService cleaningService;

	/**
	 * Clean selfie images.
	 */
	@Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT20S")
	@SchedulerLock(name = SchedulerLockNames.CLEANUP_SELFIES_LOCK, lockAtMostFor = "5m")
	public void cleanupSelfies() {
		logger.info("", kv("action", "cleanupSelfies"), kv("state", "initiated"));
		LockAssert.assertLocked();
		final int count = cleaningService.cleanSelfies();
		logger.info("", kv("action", "cleanupSelfies"), kv("state", "succeeded"), kv("count", count));
	}

	/**
	 * Cleanup of document data older than retention time.
	 */
	@Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT15S")
	@SchedulerLock(name = SchedulerLockNames.DOCUMENT_DATA_LOCK, lockAtMostFor = "5m")
	public void cleanupDocumentData() {
		LockAssert.assertLocked();
		logger.debug("", kv("action", "cleanupDocumentData"), kv("state", "initiated"));
		final var count = cleaningService.cleanupDocumentData();
		logger.debug("", kv("action", "cleanupDocumentData"), kv("state", "succeeded"), kv("cleanedRecords", count));
	}

	/**
	 * Clean personal data from document results older than retention time.
	 */
	@Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT15S")
	@SchedulerLock(name = SchedulerLockNames.DOCUMENT_RESULT_PERSONAL_DATA_LOCK, lockAtMostFor = "5m")
	public void cleanupDocumentResultPersonalData() {
		LockAssert.assertLocked();
		logger.debug("", kv("action", "cleanupDocumentResultPersonalData"), kv("state", "initiated"));
		final var count = cleaningService.cleanupDocumentResultPersonalData();
		logger.debug("", kv("action", "cleanupDocumentResultPersonalData"), kv("state", "succeeded"), kv("cleanedRecords", count));
	}

	/**
	 * Cleanup of processed document data older than retention time.
	 */
	@Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT15S")
	@SchedulerLock(name = SchedulerLockNames.PROCESSED_DOCUMENT_DATA_LOCK, lockAtMostFor = "5m")
	public void cleanupProcessedDocumentData() {
		LockAssert.assertLocked();
		logger.debug("", kv("action", "cleanupProcessedDocumentData"), kv("state", "initiated"));
		final var count = cleaningService.cleanupProcessedDocumentData();
		logger.debug("", kv("action", "cleanupProcessedDocumentData"), kv("state", "succeeded"), kv("cleanedRecords", count));
	}
}

