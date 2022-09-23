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
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Service for client evaluation features.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
public class ClientEvaluationService {

    private final OnboardingProvider onboardingProvider;

    private final IdentityVerificationConfig config;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final TransactionTemplate transactionTemplate;

    /**
     * All-arg constructor.
     *
     * @param onboardingProvider Onboarding provider.
     * @param config Identity verification config.
     * @param identityVerificationRepository Identity verification repository.
     * @param transactionTemplate Transaction template.
     */
    @Autowired
    public ClientEvaluationService(
            final OnboardingProvider onboardingProvider,
            final IdentityVerificationConfig config,
            final IdentityVerificationRepository identityVerificationRepository,
            final TransactionTemplate transactionTemplate) {
        this.onboardingProvider = onboardingProvider;
        this.config = config;
        this.identityVerificationRepository = identityVerificationRepository;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Set phase and status of the given identity verification to {@code CLIENT_EVALUATION / IN_PROGRESS}.
     *
     * @param ownerId owner ID to set timestampLastUpdated
     * @param idVerification identity verification to change
     */
    @Transactional
    public void initClientEvaluation(final OwnerId ownerId, final IdentityVerificationEntity idVerification) {
        idVerification.setPhase(IdentityVerificationPhase.CLIENT_EVALUATION);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        logger.info("Switched to CLIENT_EVALUATION/IN_PROGRESS; process ID: {}, {}", idVerification.getProcessId(), ownerId);
    }

    /**
     * Process client evaluation of the given identity verification initialized in {@link #initClientEvaluation(OwnerId, IdentityVerificationEntity)}.
     *
     * @param identityVerification identity verification to process
     */
    public void processClientEvaluation(final IdentityVerificationEntity identityVerification) {
        logger.debug("Evaluating client for {}", identityVerification);

        final EvaluateClientRequest request = EvaluateClientRequest.builder()
                .processId(identityVerification.getProcessId())
                .userId(identityVerification.getUserId())
                .identityVerificationId(identityVerification.getId())
                .verificationId(getVerificationId(identityVerification))
                .build();

        final Consumer<EvaluateClientResponse> successConsumer = createSuccessConsumer(identityVerification);
        final Consumer<Throwable> errorConsumer = createErrorConsumer(identityVerification);
        final int maxFailedAttempts = config.getClientEvaluationMaxFailedAttempts();
        onboardingProvider.evaluateClient(request)
                .retryWhen(Retry.backoff(maxFailedAttempts, Duration.ofSeconds(2)))
                .subscribe(successConsumer, errorConsumer);
    }

    private static String getVerificationId(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .findAny()
                .map(DocumentVerificationEntity::getVerificationId)
                .orElseThrow(() -> new IllegalStateException("No document verification for " + identityVerification));
    }

    private Consumer<EvaluateClientResponse> createSuccessConsumer(final IdentityVerificationEntity identityVerification) {
        return response -> {
            final Date now = new Date();
            identityVerification.setTimestampLastUpdated(now);
            // The timestampFinished parameter is not set yet, there may be other steps ahead
            if (response.isAccepted()) {
                logger.info("Client evaluation accepted for {}", identityVerification);
                identityVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
                logger.info("Switched to {}/ACCEPTED; process ID: {}", identityVerification.getPhase(), identityVerification.getProcessId());
            } else {
                logger.info("Client evaluation rejected for {}", identityVerification);
                identityVerification.setStatus(IdentityVerificationStatus.REJECTED);
                identityVerification.getDocumentVerifications()
                        .forEach(it -> it.setStatus(DocumentStatus.REJECTED));
                logger.info("Switched to {}/REJECTED; process ID: {}", identityVerification.getPhase(), identityVerification.getProcessId());
                identityVerification.setTimestampFailed(now);
            }
            if (response.isErrorOccurred()) {
                logger.warn("Business logic error occurred during client evaluation, identity verification ID: {}, error detail: {}", identityVerification.getId(), response.getErrorDetail());
                identityVerification.setErrorOrigin(ErrorOrigin.CLIENT_EVALUATION);
                identityVerification.setErrorDetail(response.getErrorDetail());
            }
            saveInTransaction(identityVerification);
        };
    }

    private Consumer<Throwable> createErrorConsumer(final IdentityVerificationEntity identityVerification) {
        return t -> {
            logger.warn("Client evaluation failed for {} - {}", identityVerification, t.getMessage());
            logger.debug("Client evaluation failed for {}", identityVerification, t);
            identityVerification.setStatus(IdentityVerificationStatus.FAILED);
            identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION);
            identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            final Date now = new Date();
            identityVerification.setTimestampLastUpdated(now);
            identityVerification.setTimestampFailed(now);
            logger.info("Switched to {}/FAILED; process ID: {}", identityVerification.getPhase(), identityVerification.getProcessId());
            saveInTransaction(identityVerification);
        };
    }

    private void saveInTransaction(final IdentityVerificationEntity identityVerification) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> identityVerificationRepository.save(identityVerification));
    }
}
