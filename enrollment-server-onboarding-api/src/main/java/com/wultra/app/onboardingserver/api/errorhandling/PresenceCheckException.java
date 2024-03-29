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
package com.wultra.app.onboardingserver.api.errorhandling;

import java.io.Serial;

/**
 * Exception thrown in case presence check fails.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class PresenceCheckException extends Exception {

    @Serial
    private static final long serialVersionUID = 3977949988982066411L;

    public PresenceCheckException() {
    }

    public PresenceCheckException(String message) {
        super(message);
    }

    public PresenceCheckException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
