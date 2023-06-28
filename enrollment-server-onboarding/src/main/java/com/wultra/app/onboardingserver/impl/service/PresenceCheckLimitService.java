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

package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingOtpRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckLimitException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;

/**
 * Service for checking presence check limits.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
public class PresenceCheckLimitService {

    private final IdentityVerificationConfig identityVerificationConfig;
    private final OnboardingOtpRepository otpRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final OnboardingProcessRepository onboardingProcessRepository;
    private final ScaResultRepository scaResultRepository;
    private final ActivationFlagService activationFlagService;

    private final IdentityVerificationService identityVerificationService;

    private final AuditService auditService;

    /**
     * Check limit for maximum number of attempts for SCA (presence check and OTP verification).
     * @param ownerId Owner identification.
     * @param processId Process identifier.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws PresenceCheckLimitException Thrown when presence check limit is exceeded.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void checkPresenceCheckMaxAttemptLimit(OwnerId ownerId, String processId) throws IdentityVerificationException, PresenceCheckLimitException, RemoteCommunicationException {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId())
                .orElseThrow(() ->
                        new IdentityVerificationException("Identity verification was not found, " + ownerId));

        final int count = scaResultRepository.countByIdentityVerification(identityVerification);
        logger.debug("SCA attempts count so far {}, {}", count, ownerId);

        if (count >= identityVerificationConfig.getPresenceCheckMaxFailedAttempts()) {

            if (!identityVerification.getProcessId().equals(processId)) {
                throw new IdentityVerificationException(String.format("Process identifier mismatch for owner %s: %s", ownerId, processId));
            }
            final OnboardingProcessEntity onboardingProcess = onboardingProcessRepository.findById(processId).orElseThrow(() ->
                new IdentityVerificationException("Onboarding process not found, " + ownerId));

            final IdentityVerificationPhase phase = identityVerification.getPhase();

            identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK);
            identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
            identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, FAILED, ownerId);

            onboardingProcess.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_PRESENCE_CHECK);
            onboardingProcess.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            onboardingProcess.setTimestampLastUpdated(ownerId.getTimestamp());
            onboardingProcess.setTimestampFailed(ownerId.getTimestamp());
            onboardingProcess.setStatus(OnboardingStatus.FAILED);
            onboardingProcessRepository.save(onboardingProcess);

            auditService.audit(onboardingProcess, identityVerification, "Presence check max failed attempts reached for user: {}", onboardingProcess.getUserId());

            // Remove flag VERIFICATION_IN_PROGRESS and add VERIFICATION_PENDING flag
            activationFlagService.updateActivationFlagsForFailedIdentityVerification(ownerId);

            throw new PresenceCheckLimitException("Max failed attempts reached for presence check, " + ownerId);
        }
    }
}
