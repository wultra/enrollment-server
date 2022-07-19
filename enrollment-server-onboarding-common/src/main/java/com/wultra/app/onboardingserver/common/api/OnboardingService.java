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
package com.wultra.app.onboardingserver.common.api;

import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.annotation.PublicApi;
import com.wultra.app.onboardingserver.common.api.model.UpdateProcessRequest;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;

/**
 * Service implementing the onboarding process.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@PublicApi
public interface OnboardingService {

    /**
     * Find a user identifier by the given onboarding process.
     *
     * @param processId Process identifier.
     * @return user identifier
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    String findUserIdByProcessId(String processId) throws OnboardingProcessException;

    /**
     * Get process status for an onboarding process.
     * @param processId Onboarding process identifier.
     * @return Onboarding process status.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    OnboardingStatus getProcessStatus(String processId) throws OnboardingProcessException;

    /**
     * Update the process.
     *
     * @param request request object
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    void updateProcess(UpdateProcessRequest request) throws OnboardingProcessException;
}
