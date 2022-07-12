/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
 *
 */

package com.wultra.app.onboardingserver.common.enumeration;

/**
 * Enumeration with onboarding process errors with error scores.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum OnboardingProcessError {

    ERROR_ACTIVATION_OTP_FAILED(1),

    ERROR_DOCUMENT_VERIFICATION_FAILED(1),

    ERROR_DOCUMENT_VERIFICATION_REJECTED(2),

    ERROR_USER_VERIFICATION_OTP_FAILED(2),

    ERROR_IDENTITY_VERIFICATION_RESET(3);

    private final int errorScore;

    OnboardingProcessError(int errorScore) {
        this.errorScore = errorScore;
    }

    public int getErrorScore() {
        return errorScore;
    }

}