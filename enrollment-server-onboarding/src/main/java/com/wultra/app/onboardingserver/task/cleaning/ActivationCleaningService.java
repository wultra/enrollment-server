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

import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.impl.service.ActivationService;
import com.wultra.security.powerauth.client.v3.ActivationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to cleaning activations.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
class ActivationCleaningService {

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final ActivationService activationService;

    private final AuditService auditService;

    @Autowired
    public ActivationCleaningService(
            final OnboardingProcessRepository onboardingProcessRepository,
            final ActivationService activationService,
            final AuditService auditService) {

        this.onboardingProcessRepository = onboardingProcessRepository;
        this.activationService = activationService;
        this.auditService = auditService;
    }

    /**
     * Cleanup activations of failed onboarding processes.
     */
    @Transactional
    public void cleanupActivations() {
        onboardingProcessRepository.findAllToRemoveActivationWithLock()
                .forEach(this::cleanupActivation);
    }

    private void cleanupActivation(final OnboardingProcessEntity process) {
        final String activationId = process.getActivationId();
        logger.info("Removing activation ID: {} of process ID: {}", activationId, process.getId());

        try {
            removeActivation(activationId);
            process.setActivationRemoved(true);
            onboardingProcessRepository.save(process);
            auditService.auditActivation(process, "Remove activation of failed process for user: {}", process.getUserId());
        } catch (RemoteCommunicationException e) {
            logger.error("Unable to remove activation ID: {}", activationId, e);
        }
    }

    private void removeActivation(String activationId) throws RemoteCommunicationException {
        final ActivationStatus activationStatus = activationService.fetchActivationStatus(activationId);
        if (activationStatus == ActivationStatus.REMOVED) {
            logger.debug("Activation ID: {} has been already removed", activationId);
            return;
        }

        activationService.removeActivation(activationId);
    }

}
