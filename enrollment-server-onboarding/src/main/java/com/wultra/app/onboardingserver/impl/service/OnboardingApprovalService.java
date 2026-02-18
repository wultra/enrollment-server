/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.SelfieRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.database.entity.ScaResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.SelfieEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.ApproveClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.ApproveClientResponse;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

/**
 * Onboarding approval service.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingApprovalService {

    private static final int MAX_ATTEMPTS = 1;

    private final OnboardingServiceImpl onboardingService;

    private final IdentityVerificationConfig config;

    private final OnboardingProvider onboardingProvider;

    private final ScaResultRepository scaResultRepository;

    private final SelfieRepository selfieRepository;

    private final AuditService auditService;

    private final RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_ATTEMPTS)
                .exponentialBackoff(200, 2.0, 2_000)
                .build();

    /**
     * Check if onboarding approval is enabled for the given process ID.
     *
     * @param processId Process ID.
     * @return {@code true} if the onboarding approval feature is enabled in the process configuration,
     *         {@code false} otherwise
     * @throws OnboardingProcessException if the onboarding process cannot be found or its configuration cannot be read.
     */
    @Transactional(readOnly = true)
    public boolean isOnboardingApprovalEnabled(final String processId) throws OnboardingProcessException {
        return onboardingService.findProcess(processId)
                .getProcessConfiguration()
                .getConfiguration()
                .approvalEnabled();
    }

    /**
     * Call the onboarding provider to approve the client.
     *
     * @param identityVerification identity verification to process
     * @return approval result; {@code null} is returned if the approval failed and represents a FAILED evaluation state
     */
    @Transactional(readOnly = true)
    public @Nullable ApproveClientResponse.ApprovalResult approve(final IdentityVerificationEntity identityVerification) {
        try {
            final OnboardingProcessEntity process = onboardingService.findProcess(identityVerification.getProcessId());

            final ScaResultEntity.Result presenceCheckResult = scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(identityVerification)
                    .orElseThrow(() -> new OnboardingProviderException("No SCA result found for identity verificationId: " + identityVerification.getId()))
                    .getPresenceCheckResult();

            final ApproveClientRequest request = ApproveClientRequest.builder()
                    .processId(identityVerification.getProcessId())
                    .processType(process.getProcessConfiguration().getProcessType())
                    .provider(config.getPresenceCheckProvider())
                    .userId(identityVerification.getUserId())
                    .identityVerificationId(identityVerification.getId())
                    .status(convert(presenceCheckResult))
                    .score(10) // so far sending constant 10 as 100 percent confidence, possible future extension point
                    .image(loadImage(identityVerification))
                    .build();

            final ApproveClientResponse response = retryTemplate.execute(context -> callApproveClient(request, context));

            final ApproveClientResponse.ApprovalResult approvalResult = response.result();
            auditService.audit(identityVerification, "Onboarding approval result: {}", approvalResult);
            return approvalResult;
        } catch (final OnboardingProviderException | OnboardingProcessException | RuntimeException e) {
            logger.warn("Failed to approve client: {}", e.getMessage(), e);
            auditService.audit(identityVerification, "Onboarding approval result: FAILED");
            return null;
        }
    }

    private String loadImage(final IdentityVerificationEntity identityVerification) {
        return selfieRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(identityVerification)
                .map(SelfieEntity::getImage)
                .map(Base64.getEncoder()::encodeToString)
                .orElse(null);
    }

    private ApproveClientResponse callApproveClient(final ApproveClientRequest request, final RetryContext context) throws OnboardingProviderException {
        final int attempt = context.getRetryCount() + 1;

        final Throwable lastThrowable = context.getLastThrowable();
        if (lastThrowable != null) {
            logger.info("action: callApproveClient, state: initiated, attempt {}/{}, previous failure: {}", attempt, MAX_ATTEMPTS, lastThrowable.getMessage());
        } else {
            logger.info("action: callApproveClient, state: initiated, attempt {}/{}", attempt, MAX_ATTEMPTS);
        }

        final ApproveClientResponse response = onboardingProvider.approveClient(request);
        logger.info("action: callApproveClient, state: succeeded");
        return response;
    }

    private static ApproveClientRequest.Status convert(final ScaResultEntity.Result source) {
        return switch (source) {
            case SUCCESS -> ApproveClientRequest.Status.SUCCESS;
            case FAILED -> ApproveClientRequest.Status.FAILURE;
        };
    }
}
