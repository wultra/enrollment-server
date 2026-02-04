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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.AcknowledgeApproveClientRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.AcknowledgeApproveClientResponse;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * Service for acknowledging async actions
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class AcknowledgeService {

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final StateMachineService stateMachineService;

    private final AuditService auditService;

    /**
     * Acknowledge client approval.
     *
     * @param request request
     * @return an acknowledgement result
     */
    public AcknowledgeApproveClientResponse acknowledgeApproveClient(final AcknowledgeApproveClientRequest request) {
        final OnboardingProcessEntity process = onboardingProcessRepository.findByIdWithLock(request.processId())
                .filter(it -> it.getStatus() == OnboardingStatus.VERIFICATION_IN_PROGRESS)
                .orElse(null);

        if (process == null) {
            return AcknowledgeApproveClientResponse.builder()
                    .result(AcknowledgeApproveClientResponse.Result.NOK)
                    .resultReason("Acknowledgement failed. Process not found or in invalid state.")
                    .build();
        }

        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById(request.identityVerificationId()).orElse(null);
        final Optional<String> validationError = validate(identityVerification, request);

        if (validationError.isPresent()) {
            return AcknowledgeApproveClientResponse.builder()
                    .result(AcknowledgeApproveClientResponse.Result.NOK)
                    .resultReason("Acknowledgement validation failed. %s".formatted(validationError.get()))
                    .build();
        }

        try {
            final OwnerId ownerId = convert(identityVerification);
            final OnboardingEvent event = convert(request.approvalResult());
            stateMachineService.processStateMachineEvent(ownerId, process.getId(), event);
            auditService.audit(identityVerification, "Acknowledged onboarding approval result: {}", request.approvalResult());
        } catch (IdentityVerificationException e) {
            logger.warn("Acknowledgement failed. Verification not found or in invalid state. {}", e.getMessage(), e);
            return AcknowledgeApproveClientResponse.builder()
                    .result(AcknowledgeApproveClientResponse.Result.NOK)
                    .resultReason("Acknowledgement failed. Verification not found or in invalid state.")
                    .build();
        }

        return AcknowledgeApproveClientResponse.builder()
                .result(AcknowledgeApproveClientResponse.Result.OK)
                .build();
    }

    private static Optional<String> validate(final IdentityVerificationEntity identityVerification, final AcknowledgeApproveClientRequest request) {
        if (identityVerification == null) {
            return Optional.of("Identity verification not found.");
        } else if (!identityVerification.getProcessId().equals(request.processId())) {
            return Optional.of("Identity verification does not belong to the process.");
        } else if (identityVerification.getPhase() != IdentityVerificationPhase.ONBOARDING_APPROVAL) {
            return Optional.of("Identity verification is not in ONBOARDING_APPROVAL phase.");
        } else if (identityVerification.getStatus() != IdentityVerificationStatus.IN_PROGRESS) {
            return Optional.of("Identity verification is not in IN_PROGRESS state.");
        } else if (!identityVerification.getUserId().equals(request.userId())) {
            return Optional.of("Identity verification does not belong to the user.");
        }
        return Optional.empty();
    }

    private static OnboardingEvent convert(final AcknowledgeApproveClientRequest.ApprovalResult source) {
        return switch (source) {
            case OK -> OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_APPROVE;
            case NOK -> OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_REJECT;
            case WAIT -> throw new IllegalArgumentException("WAIT result should be handled at the controller level and must not reach AcknowledgeService");
        };
    }

    private static OwnerId convert(final IdentityVerificationEntity source) {
        Assert.notNull(source, "Identity verification cannot be null.");

        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(source.getActivationId());
        ownerId.setUserId(source.getUserId());
        return ownerId;
    }
}
