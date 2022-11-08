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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static java.util.stream.Collectors.toSet;

/**
 * Service for client evaluation features.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
public class ClientEvaluationService {

    private static final String ERROR_VERIFICATION_ID = "unableToGetDocumentVerificationId";

    private final OnboardingProvider onboardingProvider;

    private final IdentityVerificationConfig config;

    private final IdentityVerificationService identityVerificationService;

    private final AuditService auditService;

    /**
     * All-arg constructor.
     *
     * @param onboardingProvider Onboarding provider.
     * @param config Identity verification config.
     * @param identityVerificationService Identity verification repository.
     * @param auditService Audit service.
     */
    @Autowired
    public ClientEvaluationService(
            final OnboardingProvider onboardingProvider,
            final IdentityVerificationConfig config,
            final IdentityVerificationService identityVerificationService,
            final AuditService auditService) {
        this.onboardingProvider = onboardingProvider;
        this.config = config;
        this.identityVerificationService = identityVerificationService;
        this.auditService = auditService;
    }

    /**
     * Process client evaluation of the given identity verification.
     *
     * @param identityVerification identity verification to process
     * @param ownerId Owner identification.
     */
    public void processClientEvaluation(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        logger.debug("Client evaluation started for {}", identityVerification);

        final String verificationId;
        try {
            verificationId = getVerificationId(identityVerification);
        } catch (Exception e) {
            processVerificationIdError(identityVerification, ownerId, e);
            return;
        }

        final EvaluateClientRequest request = EvaluateClientRequest.builder()
                .processId(identityVerification.getProcessId())
                .userId(identityVerification.getUserId())
                .identityVerificationId(identityVerification.getId())
                .verificationId(verificationId)
                .build();

        final int maxFailedAttempts = config.getClientEvaluationMaxFailedAttempts();
        for (int i = 0; i < maxFailedAttempts; i++) {
            final int attempt = i + 1;
            try {
                final EvaluateClientResponse response = onboardingProvider.evaluateClient(request);
                processEvaluationSuccess(identityVerification, ownerId, response);
                logger.debug("Client evaluation finished for {}, attempt: {}", identityVerification, attempt);
                return;
            } catch (Exception e) {
                logger.warn("Client evaluation failed for {}, attempt: {}, {}, {}", identityVerification, attempt, ownerId, e.getMessage());
                logger.debug("Client evaluation failed for {} - attempt: {}, {}", identityVerification, attempt, ownerId, e);
            }
        }
        processTooManyEvaluationError(identityVerification, ownerId);
    }

    private static String getVerificationId(final IdentityVerificationEntity identityVerification) {
        final Set<String> verificationIds = identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .map(DocumentVerificationEntity::getVerificationId)
                .collect(toSet());

        if (verificationIds.size() == 1) {
            return verificationIds.iterator().next();
        } else {
            throw new IllegalStateException(
                    String.format("Expected just one document verificationId for %s but got %s", identityVerification, verificationIds));
        }
    }

    private void processEvaluationSuccess(final IdentityVerificationEntity identityVerification, final OwnerId ownerId, final EvaluateClientResponse response) {
        auditService.auditOnboardingProvider(identityVerification, "Client evaluated for user: {}", ownerId.getUserId());
        // The timestampFinished parameter is not set yet, there may be other steps ahead
        if (response.isErrorOccurred()) {
            logger.warn("Business logic error occurred during client evaluation, identity verification ID: {}, error detail: {}", identityVerification.getId(), response.getErrorDetail());
            identityVerification.setErrorOrigin(ErrorOrigin.CLIENT_EVALUATION);
            identityVerification.setErrorDetail(response.getErrorDetail());
            auditService.auditOnboardingProvider(identityVerification, "Error to evaluate client for user: {}, {}", ownerId.getUserId(), response.getErrorDetail());
        }

        final IdentityVerificationPhase phase = identityVerification.getPhase();
        if (response.isAccepted()) {
            logger.info("Client evaluation accepted for {}", identityVerification);
            identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, ACCEPTED, ownerId);
        } else {
            logger.info("Client evaluation rejected for {}", identityVerification);
            identityVerification.getDocumentVerifications()
                    .forEach(document -> {
                        document.setStatus(DocumentStatus.REJECTED);
                        auditService.audit(document, "Document rejected because of client evaluation for user: {}", identityVerification.getUserId());
                    });
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
            identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, IdentityVerificationStatus.REJECTED, ownerId);
        }
    }

    private void processTooManyEvaluationError(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        logger.warn("Client evaluation too many attempts for {} - {}", identityVerification, ownerId);
        identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION);
        identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
        final IdentityVerificationPhase phase = identityVerification.getPhase();
        identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, IdentityVerificationStatus.FAILED, ownerId);
    }

    private void processVerificationIdError(final IdentityVerificationEntity identityVerification, final OwnerId ownerId, final Exception e) {
        logger.warn("Client evaluation failed to get verificationId for {}, {} - {}", identityVerification, ownerId, e.getMessage());
        logger.debug("Client evaluation failed to get verificationId for {}, {}", identityVerification, ownerId, e);
        identityVerification.setErrorDetail(ERROR_VERIFICATION_ID);
        identityVerification.setErrorOrigin(ErrorOrigin.CLIENT_EVALUATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
        final IdentityVerificationPhase phase = identityVerification.getPhase();
        identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, IdentityVerificationStatus.FAILED, ownerId);
    }
}
