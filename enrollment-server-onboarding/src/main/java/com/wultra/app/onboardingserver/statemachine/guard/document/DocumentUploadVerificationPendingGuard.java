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
package com.wultra.app.onboardingserver.statemachine.guard.document;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard to ensure ({@link IdentityVerificationPhase#DOCUMENT_UPLOAD}, {@link IdentityVerificationStatus#VERIFICATION_PENDING}) status
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class DocumentUploadVerificationPendingGuard implements Guard<EnrollmentState, EnrollmentEvent> {

    @Override
    public boolean evaluate(StateContext<EnrollmentState, EnrollmentEvent> context) {
        IdentityVerificationEntity identityVerification = (IdentityVerificationEntity) context.getMessageHeader(EventHeaderName.IDENTITY_VERIFICATION);
        return IdentityVerificationPhase.DOCUMENT_UPLOAD == identityVerification.getPhase() && IdentityVerificationStatus.VERIFICATION_PENDING == identityVerification.getStatus();
    }

}
