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
package com.wultra.app.enrollmentserver.statemachine.enums;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import lombok.Getter;

/**
 * States defined for the state machine
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Getter
public enum EnrollmentState {

    INITIAL(null, IdentityVerificationStatus.NOT_INITIALIZED),
    DOCUMENT_UPLOAD_IN_PROGRESS(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS),
    DOCUMENT_UPLOAD_VERIFICATION_PENDING(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.VERIFICATION_PENDING),
    DOCUMENT_VERIFICATION_IN_PROGRESS(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.IN_PROGRESS),

    PRESENCE_CHECK_IN_PROGRESS(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS),
    PRESENCE_CHECK_FAILED(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.FAILED),
    PRESENCE_CHECK_REJECTED(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.REJECTED),

    OTP_VERIFICATION_PENDING(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.OTP_VERIFICATION_PENDING),

    COMPLETED_ACCEPTED(IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.ACCEPTED),
    COMPLETED_FAILED(IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED),
    COMPLETED_REJECTED(IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.REJECTED);

    private final IdentityVerificationPhase phase;

    private final IdentityVerificationStatus status;

    EnrollmentState(IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        this.phase = phase;
        this.status = status;
    }

}
