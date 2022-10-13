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

package com.wultra.app.onboardingserver.common.service;

import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.configuration.CommonOnboardingConfig;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for checking identity verification limits.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationLimitService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationLimitService.class);

    private final IdentityVerificationRepository identityVerificationRepository;
    private final DocumentVerificationRepository documentVerificationRepository;
    private final CommonOnboardingConfig config;
    private final OnboardingProcessRepository onboardingProcessRepository;
    private final ActivationFlagService activationFlagService;
    private final OnboardingProcessLimitService processLimitService;

    private final AuditService auditService;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentVerificationRepository Document verification repository.
     * @param config Configuration.
     * @param onboardingProcessRepository Onboarding process repository.
     * @param activationFlagService Activation flag service.
     * @param processLimitService Onboarding process limit service.
     * @param auditService audit service.
     */
    @Autowired
    public IdentityVerificationLimitService(
            final IdentityVerificationRepository identityVerificationRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final CommonOnboardingConfig config,
            final OnboardingProcessRepository onboardingProcessRepository,
            final ActivationFlagService activationFlagService,
            final OnboardingProcessLimitService processLimitService,
            final AuditService auditService) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.config = config;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.activationFlagService = activationFlagService;
        this.processLimitService = processLimitService;
        this.auditService = auditService;
    }

    /**
     * Check attempt limit of failed identity verifications. Fail onboarding process in the attempt count has exceeded the limit.
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when activation flag request fails.
     * @throws IdentityVerificationException Thrown in case identity verification is invalid.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    public void checkIdentityVerificationLimit(OwnerId ownerId) throws RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException {
        // Make sure that the maximum attempt number of identity verifications is not exceeded based on count of database rows.
        final List<IdentityVerificationEntity> identityVerifications = identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());
        if (identityVerifications.size() >= config.getVerificationMaxFailedAttempts()) {
            final OnboardingProcessEntity process = onboardingProcessRepository.findByActivationIdAndStatus(ownerId.getActivationId(), OnboardingStatus.VERIFICATION_IN_PROGRESS)
                    .orElseThrow(() -> new IdentityVerificationException("Onboarding process not found, activation ID: " + ownerId.getActivationId()));

            // In case any of the identity verifications is not FAILED or REJECTED yet, fail it.
            identityVerifications.stream()
                    .filter(verification -> verification.getStatus() != IdentityVerificationStatus.FAILED
                            && verification.getStatus() != IdentityVerificationStatus.REJECTED)
                    .forEach(verification -> {
                        verification.setStatus(IdentityVerificationStatus.FAILED);
                        logger.info("Switched to {}/FAILED; {}", verification.getPhase(), ownerId);
                        auditService.audit(verification, "Switched to {}/FAILED; user ID: {}", verification.getPhase(), ownerId.getUserId());
                    });
            identityVerificationRepository.saveAll(identityVerifications);

            process.setErrorDetail(OnboardingProcessEntity.ERROR_MAX_FAILED_ATTEMPTS_IDENTITY_VERIFICATION);
            process.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            process.setTimestampLastUpdated(ownerId.getTimestamp());
            process.setTimestampFailed(ownerId.getTimestamp());
            process.setStatus(OnboardingStatus.FAILED);
            onboardingProcessRepository.save(process);
            auditService.audit(process, "Max failed attempts reached for identity verification for user: {}", process.getUserId());

            // Remove flag VERIFICATION_IN_PROGRESS and add VERIFICATION_PENDING flag
            activationFlagService.updateActivationFlagsForFailedIdentityVerification(ownerId);
            logger.warn("Max failed attempts reached for identity verification, {}.", ownerId);
            throw new OnboardingProcessLimitException("Max failed attempts reached for identity verification");
        }
    }

    /**
     * Check the limit for maximum number of document uploads.
     * @param ownerId Owner identifier.
     * @throws IdentityVerificationLimitException Thrown when document upload limit is reached.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    public void checkDocumentUploadLimit(OwnerId ownerId, IdentityVerificationEntity identityVerification) throws IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException, OnboardingProcessException {
        final List<DocumentVerificationEntity> documentVerificationsFailed = documentVerificationRepository.findAllDocumentVerifications(identityVerification, DocumentStatus.ALL_FAILED);
        if (documentVerificationsFailed.size() > config.getDocumentUploadMaxFailedAttempts()) {
            final IdentityVerificationPhase phase = identityVerification.getPhase();

            identityVerification.setStatus(IdentityVerificationStatus.FAILED);
            identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_DOCUMENT_UPLOAD);
            identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            identityVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
            identityVerificationRepository.save(identityVerification);
            logger.info("Switched to {}/FAILED; {}", phase, ownerId);
            auditService.audit(identityVerification, "Switched to {}/FAILED; user ID: {}", phase, ownerId.getUserId());
            resetIdentityVerification(ownerId);
            logger.warn("Max failed attempts reached for document upload, {}.", ownerId);
            throw new IdentityVerificationLimitException("Max failed attempts reached for document upload");
        }
    }

    /**
     * Reset identity verification by setting activation flag to VERIFICATION_PENDING.
     *
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when identity verification reset fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    public void resetIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException, OnboardingProcessException {
        OnboardingProcessEntity process = onboardingProcessRepository.findByActivationIdAndStatus(ownerId.getActivationId(), OnboardingStatus.VERIFICATION_IN_PROGRESS)
                .orElseThrow(() -> new OnboardingProcessException("Onboarding process not found, activation ID: " + ownerId.getActivationId()));

        // Increase process error score
        processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_IDENTITY_VERIFICATION_RESET);

        // Check process error limits
        process = processLimitService.checkOnboardingProcessErrorLimits(process);
        if (process.getStatus() == OnboardingStatus.FAILED && OnboardingProcessEntity.ERROR_MAX_PROCESS_ERROR_SCORE_EXCEEDED.equals(process.getErrorDetail())) {
            handleFailedProcess(process, ownerId);
        }

        // Check limits on identity verifications (error handling is handled by service)
        checkIdentityVerificationLimit(ownerId);

        // Update activation flags for reset of identity verification
        activationFlagService.updateActivationFlagsForFailedIdentityVerification(ownerId);
    }

    /**
     * Handle a process which just failed due to error score limit.
     * @param process Onboarding process entity.
     * @param ownerId Owner identification.
     * @throws OnboardingProcessLimitException Exception thrown due to failed process.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    private void handleFailedProcess(OnboardingProcessEntity process, OwnerId ownerId) throws OnboardingProcessLimitException, RemoteCommunicationException {
        // Remove flag VERIFICATION_IN_PROGRESS and add VERIFICATION_PENDING flag
        activationFlagService.updateActivationFlagsForFailedIdentityVerification(ownerId);

        throw new OnboardingProcessLimitException("Max error score reached for onboarding process, process ID: " + process.getId() +", owner ID: " + ownerId);
    }

}