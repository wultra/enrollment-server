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
package com.wultra.app.onboardingserver.statemachine.enums;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import lombok.Getter;
import lombok.ToString;

/**
 * States defined for the state machine
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Getter @ToString(of = {"phase", "status"})
public enum OnboardingState {

    INITIAL(null, IdentityVerificationStatus.NOT_INITIALIZED),

    DOCUMENT_UPLOAD_IN_PROGRESS(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS),
    DOCUMENT_UPLOAD_VERIFICATION_PENDING(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.VERIFICATION_PENDING),

    DOCUMENT_VERIFICATION_ACCEPTED(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.ACCEPTED),

    DOCUMENT_VERIFICATION_REJECTED(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.REJECTED),

    DOCUMENT_VERIFICATION_FAILED(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.FAILED),

    DOCUMENT_VERIFICATION_IN_PROGRESS(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.IN_PROGRESS),

    DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS(IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL, IdentityVerificationStatus.IN_PROGRESS),

    DOCUMENT_VERIFICATION_FINAL_IN_ACCEPTED(IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL, IdentityVerificationStatus.ACCEPTED),

    DOCUMENT_VERIFICATION_FINAL_FAILED(IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL, IdentityVerificationStatus.FAILED),

    DOCUMENT_VERIFICATION_FINAL_REJECTED(IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL, IdentityVerificationStatus.REJECTED),

    CLIENT_EVALUATION_ACCEPTED(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.ACCEPTED),

    CLIENT_EVALUATION_IN_PROGRESS(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.IN_PROGRESS),

    CLIENT_EVALUATION_FAILED(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.FAILED),

    CLIENT_EVALUATION_REJECTED(IdentityVerificationPhase.CLIENT_EVALUATION, IdentityVerificationStatus.REJECTED),

    PRESENCE_CHECK_IN_PROGRESS(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS),
    PRESENCE_CHECK_FAILED(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.FAILED),

    PRESENCE_CHECK_NOT_INITIALIZED(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.NOT_INITIALIZED),

    PRESENCE_CHECK_REJECTED(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.REJECTED),

    PRESENCE_CHECK_VERIFICATION_PENDING(IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.VERIFICATION_PENDING),

    OTP_VERIFICATION_PENDING(IdentityVerificationPhase.OTP_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING),

    COMPLETED_ACCEPTED(IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.ACCEPTED),
    COMPLETED_FAILED(IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED),
    COMPLETED_REJECTED(IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.REJECTED),

    CHOICE_CLIENT_EVALUATION_PROCESSING,

    CHOICE_DOCUMENT_UPLOAD,

    CHOICE_CLIENT_EVALUATION_ACCEPTED,

    CHOICE_DOCUMENT_VERIFICATION_PROCESSING,

    CHOICE_OTP_VERIFICATION,

    CHOICE_PRESENCE_CHECK_PROCESSING,

    CHOICE_VERIFICATION_PROCESSING,

    UNEXPECTED_STATE;

    private boolean choiceState = false;

    private IdentityVerificationPhase phase;

    private IdentityVerificationStatus status;

    OnboardingState() {
        this.choiceState = true;
    }

    OnboardingState(IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        this.phase = phase;
        this.status = status;
    }

    public boolean isChoiceState() {
        return choiceState;
    }

}
