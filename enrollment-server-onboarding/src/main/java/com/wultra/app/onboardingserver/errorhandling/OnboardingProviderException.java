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

/**
 * Exception thrown in case onboarding provider fails.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class OnboardingProviderException extends Exception {

    private static final long serialVersionUID = 787256528155796393L;

    /**
     * No-arg constructor.
     */
    public OnboardingProviderException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to initCause.
     *
     * @param message – the detail message. The detail message is saved for later retrieval by the getMessage() method.
     */
    public OnboardingProviderException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param  message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public OnboardingProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
