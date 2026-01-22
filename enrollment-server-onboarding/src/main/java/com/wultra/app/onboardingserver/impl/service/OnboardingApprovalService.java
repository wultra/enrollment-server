/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.ApproveClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.ApproveClientResponse;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onboarding approval service.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
public class OnboardingApprovalService {

    private final OnboardingServiceImpl onboardingService;

    private final OnboardingProvider onboardingProvider;

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
     * @param ownerId Owner identification.
     * @return approval result, may be {@code null} if the approval failed
     */
    @Transactional
    public @Nullable ApproveClientResponse.EvaluationResult approve(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        // TODO Lubos fill request
        /*
        @NonNull String processType,
        @NonNull String provider,
        @NonNull Status status,
        @NonNull Integer score,
        @NonNull String image
         */
        final ApproveClientRequest request = ApproveClientRequest.builder()
                .processId(identityVerification.getProcessId())
                .userId(identityVerification.getUserId())
                .identityVerificationId(identityVerification.getId())
                .build();

        try {
            final ApproveClientResponse response = onboardingProvider.approveClient(request);
            // TODO Lubos audit
            return response.result();
        } catch (OnboardingProviderException e) {
            logger.warn("Failed to approve client: {}", e.getMessage(), e);
            // TODO Lubos audit
            return null;
        }
    }
}
