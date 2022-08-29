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
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.provider.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.EvaluateClientResponse;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusAcceptedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusFailedGuard;
import com.wultra.app.onboardingserver.statemachine.guard.status.StatusRejectedGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

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

    /**
     * All-arg constructor.
     *
     * @param onboardingProvider Onboarding provider.
     * @param config Identity verification config.
     * @param identityVerificationService Identity verification service.
     */
    @Autowired
    public ClientEvaluationService(
            final OnboardingProvider onboardingProvider,
            final IdentityVerificationConfig config,
            final IdentityVerificationService identityVerificationService) {
        this.onboardingProvider = onboardingProvider;
        this.config = config;
        this.identityVerificationService = identityVerificationService;
    }

    public void processClientEvaluation(final IdentityVerificationEntity identityVerification, StateContext<OnboardingState, OnboardingEvent> context) {
        logger.debug("Evaluating client for {}", identityVerification);

        final EvaluateClientRequest request = EvaluateClientRequest.builder()
                .processId(UUID.fromString(identityVerification.getProcessId()))
                .userId(identityVerification.getUserId())
                .identityVerificationId(identityVerification.getId())
                .verificationId(getVerificationId(identityVerification))
                .build();

        final Map<Object, Object> variables = context.getExtendedState().getVariables();
        final int maxFailedAttempts = config.getClientEvaluationMaxFailedAttempts();
        final EvaluateClientResponse response;

        try {
            // TODO (racansky, 2022-08-29) try to refactor it to reactive non-blocking way, action and even guards
            response = onboardingProvider.evaluateClient(request)
                    .retryWhen(Retry.backoff(maxFailedAttempts, Duration.ofSeconds(2))).block();
            if (response == null) {
                throw new IllegalStateException(String.format("EvaluateClientResponse for %s is null", request));
            }
        } catch (RuntimeException e) {
            logger.warn("Client evaluation failed for {} - {}", identityVerification, e.getMessage());
            logger.debug("Client evaluation failed for {}", identityVerification, e);
            variables.put(StatusFailedGuard.KEY_FAILED, true);
            identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION);
            identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
            identityVerificationService.save(identityVerification);
            return;
        }

        if (response.isSuccessful()) {
            logger.info("Client evaluation successful for {}", identityVerification);
            variables.put(StatusAcceptedGuard.KEY_ACCEPTED, true);
        } else {
            logger.info("Client evaluation rejected for {}", identityVerification);
            variables.put(StatusRejectedGuard.KEY_REJECTED, true);
            identityVerification.getDocumentVerifications()
                    .forEach(it -> it.setStatus(DocumentStatus.REJECTED));
            identityVerificationService.save(identityVerification);
        }
    }

    private static String getVerificationId(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .findAny()
                .map(DocumentVerificationEntity::getVerificationId)
                .orElseThrow(() -> new IllegalStateException("No document verification for " + identityVerification));
    }
}
