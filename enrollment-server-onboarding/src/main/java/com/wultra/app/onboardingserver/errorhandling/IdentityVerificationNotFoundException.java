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
package com.wultra.app.onboardingserver.errorhandling;

import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;

import java.io.Serial;

/**
 * Exception thrown in case an identity verification cannot be found.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
public class IdentityVerificationNotFoundException extends IdentityVerificationException {

    @Serial
    private static final long serialVersionUID = -7599680135511121879L;

    public IdentityVerificationNotFoundException(String message) {
        super(message);
    }

}