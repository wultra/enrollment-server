/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.impl.service.verification;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationFinishService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing verification result features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class VerificationResultService {

    private final IdentityVerificationService identityVerificationService;

    private final IdentityVerificationFinishService identityVerificationFinishService;

    /**
     * Process verification result.
     * When accepted, finishes the verification.
     *
     * @param ownerId Owner identification.
     * @param identityVerification Identity verification entity.
     * @return final verification result
     */
    @Transactional
    public IdentityVerificationService.FinalVerificationResult processVerificationResult(OwnerId ownerId, IdentityVerificationEntity identityVerification) {
        logger.info("action: processVerificationResult, state: initiated, identityVerificationId: {}", identityVerification.getId());
        try {
            final var result = identityVerificationService.processVerificationResult(ownerId, identityVerification);
            if (result == IdentityVerificationService.FinalVerificationResult.OK) {
                identityVerificationFinishService.finishIdentityVerification(ownerId);
            }
            logger.info("action: processVerificationResult, state: succeeded, identityVerificationId: {}, result: {}", identityVerification.getId(), result);
            return result;
        } catch (IdentityVerificationException | OnboardingProcessException | RemoteCommunicationException e) {
            logger.error("action: processVerificationResult, state: failed, exceptionMessage: {}", e.getMessage(), e);
            return IdentityVerificationService.FinalVerificationResult.FAILED;
        }
    }

}
