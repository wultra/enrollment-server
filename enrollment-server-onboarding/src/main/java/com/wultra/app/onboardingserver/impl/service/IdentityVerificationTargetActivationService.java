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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.CreateTargetActivationRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.CreateTargetActivationResponse;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.impl.util.PowerAuthUtil;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating target activation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class IdentityVerificationTargetActivationService {

    private final OnboardingServiceImpl onboardingService;

    private final IdentityVerificationService identityVerificationService;

    private final LookupUserService lookupUserService;

    /**
     * Create target activation.
     *
     * @param request request
     * @param apiAuthentication API authentication.
     * @return response
     * @throws OnboardingProcessException in case of any business error
     */
    @Transactional
    public CreateTargetActivationResponse createTargetActivation(final CreateTargetActivationRequest request, final PowerAuthApiAuthentication apiAuthentication) throws OnboardingProcessException {
        final OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        final String processId = request.processId();

        final OnboardingProcessEntity process = onboardingService.verifyProcessIdAndLock(ownerId, processId, OnboardingStatus.VERIFICATION_IN_PROGRESS);
        validateIdentityVerificationPhase(ownerId);

        final String userId = lookupUserService.lookupUser(process, request.identification())
                .orElseThrow(() -> new OnboardingProcessException("Unable to lookup user for " + ownerId));

        logger.info("Updating processId: {}, current userId: {} to a new userId: {}", processId, process.getUserId(), userId);
        process.setUserId(userId);
        onboardingService.updateProcess(process);

        // TODO Lubos - create activation

        return CreateTargetActivationResponse.builder()
                .activationCode("TODO") // TODO Lubos
                .build();
    }

    private void validateIdentityVerificationPhase(final OwnerId ownerId) throws OnboardingProcessException {
        final IdentityVerificationEntity identityVerification = identityVerificationService.findByOptional(ownerId)
                .orElseThrow(() -> new OnboardingProcessException("Unable to find identity verification for " + ownerId));

        if (identityVerification.getPhase() != IdentityVerificationPhase.ACTIVATION_FINISH) {
            throw new OnboardingProcessException("Identity verification ID: %s is phase of %s but expected ACTIVATION_FINISH"
                    .formatted(identityVerification.getId(), identityVerification.getPhase()));
        }
    }
}
