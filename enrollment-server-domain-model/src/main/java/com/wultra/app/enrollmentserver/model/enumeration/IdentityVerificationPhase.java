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
package com.wultra.app.enrollmentserver.model.enumeration;

/**
 * Identity verification phase enumeration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum IdentityVerificationPhase {

    /**
     * Documents upload is in progress.
     */
    DOCUMENT_UPLOAD,

    /**
     * User presence is being verified.
     */
    PRESENCE_CHECK,

    /**
     * Document verification is in progress.
     */
    DOCUMENT_VERIFICATION,

    /**
     * OTP code verification is in progress.
     */
    OTP_VERIFICATION,

    /**
     * The identity verification is in the final state.
     */
    COMPLETED

}