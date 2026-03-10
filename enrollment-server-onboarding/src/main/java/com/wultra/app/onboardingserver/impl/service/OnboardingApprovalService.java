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

import com.wultra.app.enrollmentserver.model.enumeration.RejectOrigin;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Date;

/**
 * Onboarding approval service.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingApprovalService {

    private final OnboardingServiceImpl onboardingService;

    private final IdentityVerificationService identityVerificationService;

    private final IdentityVerificationConfig config;

    private final OnboardingProvider onboardingProvider;

    private final ScaResultRepository scaResultRepository;

    private final SelfieRepository selfieRepository;

    private final AuditService auditService;

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
     * Side-effect: May update the {@link IdentityVerificationEntity#setRejectReason(String)} with the approval result reason.
     *
     * @param identityVerification identity verification to process
     * @return approval result; {@code null} is returned if the approval failed and represents a FAILED evaluation state
     */
    @Transactional
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

            logger.info("action: callApproveClient, state: initiated");
            final ApproveClientResponse response = onboardingProvider.approveClient(request);
            final ApproveClientResponse.ApprovalResult approvalResult = response.result();
            final String resultReason = response.resultReason();
            logger.info("action: callApproveClient, state: succeeded, approvalResult: {}, resultReason: {}", approvalResult, resultReason);
            persistRejectReason(response, identityVerification);

            auditService.audit(identityVerification, "Onboarding approval result: {}, resultReason: {}", approvalResult, resultReason);
            return approvalResult;
        } catch (final OnboardingProviderException | OnboardingProcessException | RuntimeException e) {
            logger.warn("action: callApproveClient, state: failed, exceptionMessage: {}", e.getMessage(), e);
            auditService.audit(identityVerification, "Onboarding approval result: FAILED");
            return null;
        }
    }

    private void persistRejectReason(final ApproveClientResponse response, final IdentityVerificationEntity identityVerification) {
        if (response.result() != ApproveClientResponse.ApprovalResult.NOK) {
            logger.debug("No reject reason to store");
            return;
        }

        identityVerification.setRejectOrigin(RejectOrigin.CLIENT_APPROVAL);
        identityVerification.setRejectReason(response.resultReason());
        identityVerification.setTimestampLastUpdated(new Date());

        identityVerificationService.updateIdentityVerification(identityVerification);
    }

    private String loadImage(final IdentityVerificationEntity identityVerification) {
        return selfieRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(identityVerification)
                .map(SelfieEntity::getImage)
                .map(Base64.getEncoder()::encodeToString)
                .orElse(null);
    }

    private static ApproveClientRequest.Status convert(final ScaResultEntity.Result source) {
        return switch (source) {
            case SUCCESS -> ApproveClientRequest.Status.SUCCESS;
            case FAILED -> ApproveClientRequest.Status.FAILURE;
        };
    }
}
