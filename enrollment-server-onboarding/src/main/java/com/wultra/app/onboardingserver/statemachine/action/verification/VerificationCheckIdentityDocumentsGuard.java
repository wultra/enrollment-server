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
package com.wultra.app.onboardingserver.statemachine.action.verification;

import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard to check identity documents for verification.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class VerificationCheckIdentityDocumentsGuard implements Guard<OnboardingState, OnboardingEvent> {

    private final IdentityVerificationService identityVerificationService;

    @Autowired
    public VerificationCheckIdentityDocumentsGuard(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    public boolean evaluate(StateContext<OnboardingState, OnboardingEvent> context) {
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
        boolean result = identityVerificationService.isIdentityDocumentsForVerification(identityVerification);

        if (result) {
            logger.info("All documents of Identity Verification ID: {} are in status VERIFICATION_PENDING", identityVerification.getId());
        } else {
            logger.info("Not all documents of Identity Verification ID: {} are in status VERIFICATION_PENDING", identityVerification.getId());
        }

        return result;
    }
}
