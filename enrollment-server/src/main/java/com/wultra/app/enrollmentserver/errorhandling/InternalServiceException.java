/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2024 Wultra s.r.o.
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

import java.io.Serial;

/**
 * Exception raised when call to an upstream service fails with unexpected error.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
public class InternalServiceException extends Exception {

    @Serial
    private static final long serialVersionUID = 3539063915259282763L;

    /**
     * Constructor with a specified message.
     * @param message Error message.
     */
    public InternalServiceException(String message) {
        super(message);
    }

    /**
     * Constructor with a specified message and cause.
     * @param message Message.
     * @param cause Cause.
     */
    public InternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
