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
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.provider.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.EvaluateClientResponse;
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
import java.util.stream.Stream;

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
     * Process client evaluations.
     */
    @Transactional(readOnly = true)
    public void processClientEvaluations() {
        try (Stream<IdentityVerificationEntity> stream = identityVerificationRepository.streamAllInProgressClientEvaluations()) {
            stream.forEach(this::processClientEvaluation);
        }
    }

    @Transactional
    public void initClientEvaluation(final OwnerId ownerId, final IdentityVerificationEntity idVerification) {
        idVerification.setPhase(IdentityVerificationPhase.CLIENT_EVALUATION);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        logger.info("Switched to CLIENT_EVALUATION/IN_PROGRESS; {}, process ID: {}", ownerId, idVerification.getProcessId());
    }

    private void processClientEvaluation(final IdentityVerificationEntity identityVerification) {
        logger.debug("Evaluating client for {}", identityVerification);

        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(identityVerification.getActivationId());
        ownerId.setUserId("server-task-client-evaluations");

        final EvaluateClientRequest request = EvaluateClientRequest.builder()
                .processId(UUID.fromString(identityVerification.getProcessId()))
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
            identityVerification.setTimestampFinished(now);
            if (response.isSuccessful()) {
                logger.info("Client evaluation successful for {}", identityVerification);
                identityVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
            } else {
                logger.info("Client evaluation rejected for {}", identityVerification);
                identityVerification.setStatus(IdentityVerificationStatus.REJECTED);
                identityVerification.getDocumentVerifications()
                        .forEach(it -> it.setStatus(DocumentStatus.REJECTED));
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
            saveInTransaction(identityVerification);
        };
    }

    private void saveInTransaction(final IdentityVerificationEntity identityVerification) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> identityVerificationRepository.save(identityVerification));
    }
}
