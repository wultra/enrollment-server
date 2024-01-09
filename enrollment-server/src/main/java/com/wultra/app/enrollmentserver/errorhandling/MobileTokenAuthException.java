/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.errorhandling;

import com.wultra.security.powerauth.lib.mtoken.model.enumeration.ErrorCode;

import java.io.Serial;

/**
 * Exception related to mobile token app authentication failures.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public class MobileTokenAuthException extends MobileTokenException {

    @Serial
    private static final long serialVersionUID = -4602362062047233809L;

    public MobileTokenAuthException() {
        super(ErrorCode.POWERAUTH_AUTH_FAIL, "Authentication failed");
    }

    public MobileTokenAuthException(final String code, final String message) {
        super(code, message);
    }
}
