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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.impl.service.ActivationService;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.kv;

/**
 * Service to cleaning activations.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
class ActivationCleaningService {

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final ActivationService activationService;

    private final ActivationFlagService activationFlagService;

    private final AuditService auditService;

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

        try {
            if (isExistingActivation(process)) {
                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(activationId);
                ownerId.setUserId(process.getUserId());
                activationFlagService.removeActivationFlag(ownerId, process.getProcessConfiguration().getConfiguration().existingActivationFlag());
                if (!process.getProcessConfiguration().getConfiguration().invalidateExistingActivationOnFailure()) {
                    logger.info("Keeping existing activation of failed process", kv("activationId", activationId), kv("processId", process.getId()));
                    // TODO Lubos double-check this, it looks strange
                    process.setActivationRemoved(true);
                    onboardingProcessRepository.save(process);
                    auditService.auditActivation(process, activationId, "Keep activation of failed existing activation process for user: {}", process.getUserId());
                    return;
                }
            }

            logger.info("Removing activation", kv("activationId", activationId), kv("processId", process.getId()));
            removeActivation(activationId);
            process.setActivationRemoved(true);
            onboardingProcessRepository.save(process);
            auditService.auditActivation(process, activationId, "Remove activation of failed process for user: {}", process.getUserId());
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

    private static boolean isExistingActivation(final OnboardingProcessEntity process) {
        return process.getProcessConfiguration() != null && process.getProcessConfiguration().getConfiguration().existingActivation();
    }

}
