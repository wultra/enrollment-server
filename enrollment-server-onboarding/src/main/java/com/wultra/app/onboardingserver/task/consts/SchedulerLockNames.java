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
 *
 */

package com.wultra.app.onboardingserver.task.consts;

/**
 * Lock names for @SchedulerLock.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public final class SchedulerLockNames {

    private SchedulerLockNames() {
        throw new IllegalStateException("Class with constants");
    }

    public static final String ONBOARDING_PROCESS_LOCK = "onboardingProcessLock";

    public static final String ONBOARDING_OTP_LOCK = "onboardingOtpLock";

    public static final String DOCUMENT_SUBMIT_SYNC_LOCK = "documentSubmitCheckLock";

    public static final String DOCUMENT_SUBMIT_VERIFICATION_LOCK = "documentSubmitVerificationsLock";

    public static final String DOCUMENT_VERIFICATION_LOCK = "documentVerificationsLock";

    public static final String LARGE_DOCUMENT_DATA_LOCK = "largeDocumentDataLock";

    public static final String EXPIRE_DOCUMENT_VERIFICATION_LOCK = "expireDocumentVerificationLock";

    public static final String CLEANUP_ACTIVATIONS_LOCK = "cleanupActivationsLock";

}
