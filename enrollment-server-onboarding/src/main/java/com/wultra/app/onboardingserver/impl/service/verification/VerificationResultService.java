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

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationFinishService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing verification result features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class VerificationResultService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationResultService.class);

    private final IdentityVerificationService identityVerificationService;

    private final IdentityVerificationFinishService identityVerificationFinishService;

    @Autowired
    public VerificationResultService(
            final IdentityVerificationService identityVerificationService,
            final IdentityVerificationFinishService identityVerificationFinishService) {
        this.identityVerificationService = identityVerificationService;
        this.identityVerificationFinishService = identityVerificationFinishService;
    }

    /**
     * Checks documents verification result and when accepted finishes the verification
     *
     * @param ownerId Owner identification.
     * @param identityVerification Identity verification entity.
     * @throws IdentityVerificationException Thrown when identity verification is already finished.
     * @throws OnboardingProcessException Thrown when onboarding process termination fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    @Transactional
    public void checkVerificationResult(OwnerId ownerId, IdentityVerificationEntity identityVerification)
            throws IdentityVerificationException, OnboardingProcessException, RemoteCommunicationException{
        identityVerificationService.processDocumentVerificationResult(ownerId, identityVerification, IdentityVerificationPhase.COMPLETED);
        if (identityVerification.getStatus() == IdentityVerificationStatus.ACCEPTED) {
                identityVerificationFinishService.finishIdentityVerification(ownerId);
        }
    }

}
