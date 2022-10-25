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
package com.wultra.app.enrollmentserver.model.enumeration;

/**
 * Origin of an error enumeration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum ErrorOrigin {

    DOCUMENT_VERIFICATION,

    PRESENCE_CHECK,

    CLIENT_EVALUATION,

    OTP_VERIFICATION,

    PROCESS_LIMIT_CHECK,

    USER_REQUEST,

    FINAL_VALIDATION,

    CLEANUP
}
