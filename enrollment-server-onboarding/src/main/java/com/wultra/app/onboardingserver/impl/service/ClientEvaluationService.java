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
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    private final AuditService auditService;

    private final CommonOnboardingService onboardingService;

    private final ClientEvaluationDocumentCheckResultFactory clientEvaluationDocumentCheckResultFactory;

    private final RetryTemplate retryTemplate;

    public ClientEvaluationService(OnboardingProvider onboardingProvider,
                                   IdentityVerificationConfig config,
                                   AuditService auditService,
                                   CommonOnboardingService onboardingService,
                                   ClientEvaluationDocumentCheckResultFactory clientEvaluationDocumentCheckResultFactory) {
        this.onboardingProvider = onboardingProvider;
        this.config = config;
        this.auditService = auditService;
        this.onboardingService = onboardingService;
        this.clientEvaluationDocumentCheckResultFactory = clientEvaluationDocumentCheckResultFactory;

        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(config.getClientEvaluationMaxFailedAttempts())
                .exponentialBackoff(200, 2.0, 2_000)
                .build();
    }

    /**
     * Checks whether the client evaluation phase is enabled in the onboarding process.
     *
     * @param processId the onboarding process ID
     * @return whether the client evaluation phase is enabled
     * @throws OnboardingProcessException if the onboarding process configuration is not found
     */
    @Transactional(readOnly = true)
    public boolean isClientEvaluationEnabled(final String processId) throws OnboardingProcessException {
        return onboardingService.findProcess(processId)
                .getProcessConfiguration()
                .getConfiguration()
                .clientEvaluationEnabled();
    }

    /**
     * Process client evaluation of the given identity verification.
     *
     * @param identityVerification identity verification to process
     * @param ownerId Owner identification.
     */
    public @Nullable EvaluateClientResponse.EvaluationResult processClientEvaluation(
            final IdentityVerificationEntity identityVerification,
            final OwnerId ownerId
    ) {
        final OnboardingProcessEntity process;
        final EvaluateClientRequest.DocumentCheckResult documentCheckResult;
        final String verificationId;
        try {
            process = onboardingService.findProcess(identityVerification.getProcessId());
            final var acceptedDocuments = selectAcceptedDocuments(identityVerification);
            documentCheckResult = clientEvaluationDocumentCheckResultFactory.create(acceptedDocuments, config.isSendingExtractedDataEnabled());
            verificationId = fetchVerificationId(identityVerification, acceptedDocuments);
        } catch (final OnboardingProcessException | RuntimeException e) {
            processVerificationIdError(identityVerification, ownerId, e);
            return null;
        }

        final var request = EvaluateClientRequest.builder()
                .processId(process.getId())
                .processType(process.getProcessConfiguration().getProcessType())
                .userId(identityVerification.getUserId())
                .identityVerificationId(identityVerification.getId())
                .verificationId(verificationId)
                .provider(config.getDocumentVerificationProvider())
                .status(EvaluateClientRequest.Status.SUCCESS)
                .documentCheckResult(documentCheckResult)
                .build();

        try {
            final EvaluateClientResponse response = retryTemplate.execute(context -> callEvaluateClient(request, context));
            processEvaluationResponse(identityVerification, ownerId, response);
            return response.getEvaluationResult();
        } catch (final OnboardingProviderException | RuntimeException e) {
            processTooManyEvaluationError(identityVerification, ownerId);
            return null;
        }
    }

    private EvaluateClientResponse callEvaluateClient(final EvaluateClientRequest request, final RetryContext context) throws OnboardingProviderException {
        final var maxAttempts = config.getClientEvaluationMaxFailedAttempts();
        final int attempt = context.getRetryCount() + 1;

        logger.info("action: callEvaluateClient, state: initiated, attempt {}/{}", attempt, maxAttempts);

        try {
            final EvaluateClientResponse response = onboardingProvider.evaluateClient(request);
            logger.info("action: callEvaluateClient, state: succeeded, evaluationResult: {}, resultReason: {}", response.getEvaluationResult(), response.getResultReason());
            return response;
        } catch (final Exception e) {
            logger.warn("action: callEvaluateClient, state: failed, attempt {}/{}, exceptionMessage: {}", attempt, maxAttempts, e.getMessage(), e);
            throw e;
        }
    }

    private static Set<DocumentVerificationEntity> selectAcceptedDocuments(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(toSet());
    }

    private static String fetchVerificationId(final IdentityVerificationEntity identityVerification, final Set<DocumentVerificationEntity> documents) {
        final Set<String> verificationIds = documents.stream()
                .map(DocumentVerificationEntity::getVerificationId)
                .collect(toSet());

        if (verificationIds.size() == 1) {
            return verificationIds.iterator().next();
        } else {
            throw new IllegalStateException(
                    String.format("Expected just one document verificationId for %s but got %s", identityVerification, verificationIds));
        }
    }

    private void processTooManyEvaluationError(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        logger.warn("Client evaluation too many attempts for {} - {}", identityVerification, ownerId);
        identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION);
        identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
    }

    private void processVerificationIdError(final IdentityVerificationEntity identityVerification, final OwnerId ownerId, final Exception e) {
        logger.warn("Client evaluation failed to get verificationId for {}, {} - {}", identityVerification, ownerId, e.getMessage());
        logger.debug("Client evaluation failed to get verificationId for {}, {}", identityVerification, ownerId, e);
        identityVerification.setErrorDetail(ERROR_VERIFICATION_ID);
        identityVerification.setErrorOrigin(ErrorOrigin.CLIENT_EVALUATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
    }

    private void processEvaluationResponse(final IdentityVerificationEntity identityVerification, final OwnerId ownerId, final EvaluateClientResponse response) {
        auditService.auditOnboardingProvider(identityVerification, "Client evaluated for user: {}", ownerId.getUserId());
        final var identityVerificationId = identityVerification.getId();

        if (EvaluateClientResponse.EvaluationResult.OK == response.getEvaluationResult()) {
            logger.info("Client evaluation accepted for identity verification id: {}", identityVerificationId);
        } else if (EvaluateClientResponse.EvaluationResult.NOK == response.getEvaluationResult()) {
            logger.info("Client evaluation rejected for identity verification id: {}", identityVerificationId);
            identityVerification.getDocumentVerifications()
                    .forEach(document -> {
                        document.setStatus(DocumentStatus.REJECTED);
                        auditService.audit(document, "Document rejected because of client evaluation for user: {}", identityVerification.getUserId());
                    });
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
        } else { // WAIT
            logger.info("Client evaluation waiting for identity verification id: {}", identityVerificationId);
        }
    }
}
