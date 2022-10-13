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

import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.api.OnboardingService;
import com.wultra.app.onboardingserver.common.api.model.UpdateProcessRequest;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link OnboardingService} which is shared both for enrollment and onboarding.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
public class CommonOnboardingService implements OnboardingService {

    protected final OnboardingProcessRepository onboardingProcessRepository;

    protected final AuditService auditService;

    /**
     * Service constructor.
     *
     * @param onboardingProcessRepository Onboarding process repository.
     * @param auditService Audit service.
     */
    public CommonOnboardingService(final OnboardingProcessRepository onboardingProcessRepository, final AuditService auditService) {
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.auditService = auditService;
    }

    /**
     * Find an onboarding process.
     * @param processId Process identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OnboardingProcessEntity findProcess(String processId) throws OnboardingProcessException {
        return onboardingProcessRepository.findById(processId).orElseThrow(() ->
            new OnboardingProcessException("Onboarding process not found, process ID: " + processId));
    }

    /**
     * Find an onboarding process. Lock the process until the end of the transaction.
     * @param processId Process identifier.
     * @return Onboarding process.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    public OnboardingProcessEntity findProcessWithLock(String processId) throws OnboardingProcessException {
        return onboardingProcessRepository.findByIdWithLock(processId).orElseThrow(() ->
                new OnboardingProcessException("Onboarding process not found, process ID: " + processId));
    }

    @Override
    public String findUserIdByProcessId(final String processId) throws OnboardingProcessException {
        return findProcessWithLock(processId).getUserId();
    }

    @Override
    public OnboardingStatus getProcessStatus(String processId) throws OnboardingProcessException {
        return findProcessWithLock(processId).getStatus();
    }

    /**
     * Update a process entity in database.
     * @param process Onboarding process entity.
     * @return Updated onboarding process entity.
     */
    public OnboardingProcessEntity updateProcess(OnboardingProcessEntity process) {
        return onboardingProcessRepository.save(process);
    }

    @Override
    public void updateProcess(final UpdateProcessRequest request) throws OnboardingProcessException {
        // The onboarding process is locked until the end of the transaction
        final OnboardingProcessEntity process = findProcessWithLock(request.getProcessId());
        process.setStatus(request.getStatus());
        process.setActivationId(request.getActivationId());
        process.setTimestampLastUpdated(request.getTimestampLastUpdated());
        updateProcess(process);
        auditService.audit(process, "Update process of status {} for user: {}", process.getStatus(), process.getUserId());
    }
}
