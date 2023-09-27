/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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
 * Exception used when invalid request object is received on the server side.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public class InvalidRequestObjectException extends Exception {
    @Serial
    private static final long serialVersionUID = 1383969189713398388L;

    /**
     * No-arg constructor.
     */
    public InvalidRequestObjectException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message
     */
    public InvalidRequestObjectException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public InvalidRequestObjectException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public InvalidRequestObjectException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
