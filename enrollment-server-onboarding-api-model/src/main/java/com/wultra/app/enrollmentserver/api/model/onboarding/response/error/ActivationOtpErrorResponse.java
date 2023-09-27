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
 */
package com.wultra.app.enrollmentserver.api.model.onboarding.response.error;

import io.getlime.core.rest.model.base.response.ErrorResponse;
import jakarta.validation.constraints.NotBlank;

/**
 * Response class used when OTP code verification fails during activation (soft fail).
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class ActivationOtpErrorResponse extends ErrorResponse {

    private Integer remainingAttempts;

    /**
     * Default constructor.
     */
    public ActivationOtpErrorResponse() {
        super();
    }

    /**
     * Create a new error response with response object with provided code, error message and remaining attempts.
     *
     * @param code Error code.
     * @param message Error message.
     */
    public ActivationOtpErrorResponse(@NotBlank String code, String message, Integer remainingAttempts) {
        super(code, message);
        this.remainingAttempts = remainingAttempts;
    }

    /**
     * Get remaining attempts for OTP verification during activation.
     * @return Remaining attempts for OTP verification during activation.
     */
    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    /**
     * Set remaining attempts for OTP verification during activation.
     * @param remainingAttempts Remaining attempts for OTP verification during activation.
     */
    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

}
