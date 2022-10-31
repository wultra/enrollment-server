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
package com.wultra.app.onboardingserver.common.service;

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.configuration.CommonOnboardingConfig;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service for handling onboarding process limits.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OnboardingProcessLimitService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingProcessLimitService.class);

    private final CommonOnboardingConfig config;
    private final OnboardingProcessRepository onboardingProcessRepository;

    private final AuditService auditService;

    /**
     * Service constructor.
     *
     * @param config Onboarding process configuration.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param auditService Audit service.
     */
    @Autowired
    public OnboardingProcessLimitService(
            final CommonOnboardingConfig config,
            final OnboardingProcessRepository onboardingProcessRepository,
            final AuditService auditService) {

        this.config = config;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.auditService = auditService;
    }

    /**
     * Increment error score for an onboarding process.
     * @param process Onboarding process entity.
     * @param error Onboarding process error.
     * @param ownerId Owner identification.
     */
    public OnboardingProcessEntity incrementErrorScore(final OnboardingProcessEntity process, final OnboardingProcessError error, final OwnerId ownerId) {
        final int errorScoreIncrement = error.getErrorScore();
        final int errorScoreTotal = process.getErrorScore() + errorScoreIncrement;
        logger.info("Incrementing error score by {} to total {}, {}", errorScoreIncrement, errorScoreTotal, ownerId);
        process.setErrorScore(errorScoreTotal);
        return onboardingProcessRepository.save(process);
    }
    
    /**
     * Check the onboarding process error score and fail the process in case the error score exceeds the limit.
     * @param process Onboarding process entity.
     */
    public OnboardingProcessEntity checkOnboardingProcessErrorLimits(OnboardingProcessEntity process) {
        int maxProcessErrorScore = config.getMaxProcessErrorScore();
        if (process.getErrorScore() > maxProcessErrorScore) {
            // Onboarding process is failed, update it and persist the change
            process = failProcess(process, OnboardingProcessEntity.ERROR_MAX_PROCESS_ERROR_SCORE_EXCEEDED, ErrorOrigin.PROCESS_LIMIT_CHECK);
        }
        return process;
    }

    /**
     * Mark the given onboarding process entity as failed with the given error detail.
     * Does nothing for already failed onboarding process entity.
     *
     * @param entity Onboarding process entity to update.
     * @param errorDetail Error detail.
     * @param errorOrigin Error origin.
     * @return Updated onboarding process entity.
     */
    public OnboardingProcessEntity failProcess(OnboardingProcessEntity entity, String errorDetail, ErrorOrigin errorOrigin) {
        if (OnboardingStatus.FAILED == entity.getStatus()) {
            logger.debug("Not failing already failed onboarding process entity ID: {}", entity.getId());
            return entity;
        }
        entity.setStatus(OnboardingStatus.FAILED);
        entity.setErrorDetail(errorDetail);
        entity.setErrorOrigin(errorOrigin);
        final Date now = new Date();
        entity.setTimestampLastUpdated(now);
        entity.setTimestampFailed(now);
        auditService.audit(entity, "Process failed: {}, for user: {}", errorDetail, entity.getUserId());
        return onboardingProcessRepository.save(entity);
    }

}