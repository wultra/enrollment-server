/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.model.enumeration;

import java.util.Set;

/**
 * Enumeration representing onboarding process status.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum OnboardingStatus {

    /**
     * Activation is in progress.
     */
    ACTIVATION_IN_PROGRESS,

    /**
     * User verification after successful activation is in progress.
     */
    VERIFICATION_IN_PROGRESS,

    /**
     * Onboarding process is finished.
     */
    FINISHED,

    /**
     * Onboarding process is failed.
     */
    FAILED;

    /**
     * A constant representing the set of onboarding statuses where the process is still in progress.
     * This includes activation and verification steps that have not yet been completed.
     */
    public static final Set<OnboardingStatus> NOT_YET_COMPLETED = Set.of(ACTIVATION_IN_PROGRESS, VERIFICATION_IN_PROGRESS);

    /**
     * Set of statuses representing completed onboarding process.
     */
    public static final Set<OnboardingStatus> COMPLETED = Set.of(FINISHED, FAILED);

}
