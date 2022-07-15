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
package com.wultra.app.onboardingserver.statemachine.guard.otp;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard to ensure OTP verification preconditions
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class OtpVerificationPreconditionsGuard implements Guard<EnrollmentState, EnrollmentEvent> {

    private static final Logger logger = LoggerFactory.getLogger(OtpVerificationPreconditionsGuard.class);

    @Override
    public boolean evaluate(StateContext<EnrollmentState, EnrollmentEvent> context) {
        IdentityVerificationEntity identityVerification = (IdentityVerificationEntity) context.getMessageHeader(EventHeaderName.IDENTITY_VERIFICATION);
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);

        if (!IdentityVerificationPhase.OTP_VERIFICATION.equals(identityVerification.getPhase())) {
            logger.warn("Invalid identity verification phase {}, but expected {}, {}",
                    identityVerification.getPhase(), IdentityVerificationPhase.OTP_VERIFICATION, ownerId);
            fail(context, "Unexpected state of identity verification");
            return false;
        }
        if (!IdentityVerificationStatus.OTP_VERIFICATION_PENDING.equals(identityVerification.getStatus())) {
            logger.warn("Invalid identity verification status {}, but expected {}, {}",
                    identityVerification.getStatus(), IdentityVerificationStatus.OTP_VERIFICATION_PENDING, ownerId);
            fail(context, "Unexpected state of identity verification");
            return false;
        }
        return true;
    }

    private void fail(StateContext<EnrollmentState, EnrollmentEvent> context, String message) {
        context.getStateMachine().setStateMachineError(new OnboardingProcessException(message));
    }

}
