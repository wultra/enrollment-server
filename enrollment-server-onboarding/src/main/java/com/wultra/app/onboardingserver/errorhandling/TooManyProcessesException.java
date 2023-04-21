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
package com.wultra.app.onboardingserver.errorhandling;

import java.io.Serial;

/**
 * Exception thrown in case too many onboarding processes were created by user.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class TooManyProcessesException extends Exception {

    @Serial
    private static final long serialVersionUID = 6611918579148298666L;

    /**
     * Constructs a new exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to initCause.
     *
     * @param message – the detail message. The detail message is saved for later retrieval by the getMessage() method.
     */
    public TooManyProcessesException(final String message) {
        super(message);
    }

}