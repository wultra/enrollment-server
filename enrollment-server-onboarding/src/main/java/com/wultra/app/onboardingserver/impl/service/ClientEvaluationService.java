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
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Consumer;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS;

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

    private final IdentityVerificationService identityVerificationService;

    private final TransactionTemplate transactionTemplate;

    private final CommonOnboardingService commonOnboardingService;

    private final AuditService auditService;

    /**
     * All-arg constructor.
     * @param onboardingProvider Onboarding provider.
     * @param config Identity verification config.
     * @param identityVerificationService Identity verification repository.
     * @param transactionTemplate Transaction template.
     * @param commonOnboardingService Common onboarding service.
     * @param auditService Audit service.
     */
    @Autowired
    public ClientEvaluationService(
            final OnboardingProvider onboardingProvider,
            final IdentityVerificationConfig config,
            final IdentityVerificationService identityVerificationService,
            final TransactionTemplate transactionTemplate,
            final CommonOnboardingService commonOnboardingService,
            final AuditService auditService) {
        this.onboardingProvider = onboardingProvider;
        this.config = config;
        this.identityVerificationService = identityVerificationService;
        this.transactionTemplate = transactionTemplate;
        this.commonOnboardingService = commonOnboardingService;
        this.auditService = auditService;
    }

    /**
     * Set phase and status of the given identity verification to {@code CLIENT_EVALUATION / IN_PROGRESS}.
     *
     * @param ownerId owner ID to set timestampLastUpdated
     * @param idVerification identity verification to change
     */
    public void initClientEvaluation(final OwnerId ownerId, final IdentityVerificationEntity idVerification) {
        identityVerificationService.moveToPhaseAndStatus(idVerification, CLIENT_EVALUATION, IN_PROGRESS, ownerId);
    }

    /**
     * Process client evaluation of the given identity verification initialized in {@link #initClientEvaluation(OwnerId, IdentityVerificationEntity)}.
     *
     * @param identityVerification identity verification to process
     * @param ownerId Owner identification.
     */
    public void processClientEvaluation(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        logger.debug("Evaluating client for {}", identityVerification);

        final EvaluateClientRequest request = EvaluateClientRequest.builder()
                .processId(identityVerification.getProcessId())
                .userId(identityVerification.getUserId())
                .identityVerificationId(identityVerification.getId())
                .verificationId(getVerificationId(identityVerification))
                .build();

        final Consumer<EvaluateClientResponse> successConsumer = createSuccessConsumer(identityVerification, ownerId);
        final Consumer<Throwable> errorConsumer = createErrorConsumer(identityVerification, ownerId);
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

    private Consumer<EvaluateClientResponse> createSuccessConsumer(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        return response -> saveInANewTransaction(status -> {
            try {
                commonOnboardingService.findProcessWithLock(identityVerification.getProcessId());
            } catch (OnboardingProcessException ex) {
                logger.error(ex.getMessage(), ex);
                return;
            }
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
        });
    }

    private Consumer<Throwable> createErrorConsumer(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        return t -> saveInANewTransaction(status -> {
            try {
                commonOnboardingService.findProcessWithLock(identityVerification.getProcessId());
            } catch (OnboardingProcessException ex) {
                logger.error(ex.getMessage(), ex);
                return;
            }
            logger.warn("Client evaluation failed for {} - {}", identityVerification, t.getMessage());
            logger.debug("Client evaluation failed for {}", identityVerification, t);
            identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION);
            identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
            final IdentityVerificationPhase phase = identityVerification.getPhase();
            identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, IdentityVerificationStatus.FAILED, ownerId);
        });
    }

    private void saveInANewTransaction(final Consumer<TransactionStatus> consumer) {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(consumer);
    }

}
