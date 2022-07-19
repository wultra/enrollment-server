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
package com.wultra.app.onboardingserver.common.activation;

import com.wultra.app.onboardingserver.common.api.OnboardingService;
import com.wultra.app.onboardingserver.common.api.model.UpdateProcessRequest;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Service used for updating the onboarding process status during activation.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class ActivationProcessService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationProcessService.class);

    private final OnboardingService onboardingService;

    /**
     * Service constructor.
     *
     * @param onboardingService Onboarding service.
     */
    public ActivationProcessService(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * Update an onboarding process during activation.
     * @param processId Process identifier.
     * @param userId User identifier.
     * @param activationId Activation identifier.
     * @param status Onboarding process status to set.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public void updateProcess(String processId, String userId, String activationId, OnboardingStatus status) throws OnboardingProcessException {
        final String processUserId = getUserId(processId);
        if (!processUserId.equals(userId)) {
            logger.warn("User ID does not match to onboarding process: {}, {} ", processId, userId);
            throw new OnboardingProcessException();
        }

        final UpdateProcessRequest request = UpdateProcessRequest.builder()
                .processId(processId)
                .activationId(activationId)
                .status(status)
                .timestampLastUpdated(new Date())
                .build();
        onboardingService.updateProcess(request);
    }

    /**
     * Get user identifier for an onboarding process.
     * @param processId Onboarding process identifier.
     * @return User identifier.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public String getUserId(String processId) throws OnboardingProcessException {
        return onboardingService.findUserIdByProcessId(processId);
    }

    /**
     * Get process status for an onboarding process.
     * @param processId Onboarding process identifier.
     * @return Onboarding process status.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public String getProcessStatus(String processId) throws OnboardingProcessException {
        return onboardingService.findUserIdByProcessId(processId);
    }

}