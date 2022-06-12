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
package com.wultra.app.onboardingserver.activation;

import com.wultra.app.onboardingserver.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.impl.service.OnboardingService;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service used for updating the onboarding process status during activation.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class ActivationProcessService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationProcessService.class);

    private final OnboardingService onboardingService;

    /**
     * Service constructor.
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
        OnboardingProcessEntity process = onboardingService.findProcess(processId);
        checkUserIdForProcess(process, userId);
        process.setActivationId(activationId);
        process.setStatus(status);
        process.setTimestampLastUpdated(new Date());
        onboardingService.updateProcess(process);
    }

    /**
     * Get user identifier for an onboarding process.
     * @param processId Onboarding process identifier.
     * @return User identifier.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public String getUserId(String processId) throws OnboardingProcessException {
        OnboardingProcessEntity process = onboardingService.findProcess(processId);
        return process.getUserId();
    }

    /**
     * Check user identifier for an onboarding process.
     * @param process Onboarding process.
     * @param userId User identifier.
     * @throws OnboardingProcessException Thrown when onboarding process is not found or the user ID does not match the process.
     */
    private void checkUserIdForProcess(OnboardingProcessEntity process, String userId) throws OnboardingProcessException {
        if (!process.getUserId().equals(userId)) {
            logger.warn("User ID does not match to onboarding process: {}, {} ", process.getId(), userId);
            throw new OnboardingProcessException();
        }
    }

}