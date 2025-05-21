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
package com.wultra.app.onboardingserver.common.errorhandling;

import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthActivationException;

import java.io.Serial;

/**
 * Exception thrown in case activation using OTP code fails (soft fail).
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class PowerAuthActivationOtpException extends PowerAuthActivationException {

    @Serial
    private static final long serialVersionUID = -1587779018046975797L;

    private final Integer remainingAttempts;

    /**
     * Basic constructor.
     * @param remainingAttempts Remaining attempts for OTP verification during activation.
     */
    public PowerAuthActivationOtpException(Integer remainingAttempts) {
        super("POWER_AUTH_ACTIVATION_INVALID");
        this.remainingAttempts = remainingAttempts;
    }

    /**
     * Constructor with exception message.
     * @param message Exception message.
     * @param remainingAttempts Remaining attempts for OTP verification during activation.
     */
    public PowerAuthActivationOtpException(String message, Integer remainingAttempts) {
        super(message);
        this.remainingAttempts = remainingAttempts;
    }

    /**
     * Constructor with exception cause.
     * @param cause Exception cause.
     * @param remainingAttempts Remaining attempts for OTP verification during activation.
     */
    public PowerAuthActivationOtpException(Throwable cause, Integer remainingAttempts) {
        super(cause);
        this.remainingAttempts = remainingAttempts;
    }

    /**
     * Get remaining attempts for OTP code verification during activation.
     * @return Remaining attempts for OTP code verification during activation.
     */
    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

}
