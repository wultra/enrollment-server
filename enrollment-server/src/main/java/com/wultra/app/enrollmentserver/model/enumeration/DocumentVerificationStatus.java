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
 * Document verification status enumeration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum DocumentVerificationStatus {

    /**
     * Document has been verified, and it is accepted as a valid document.
     */
    ACCEPTED,

    /**
     * Document upload is in progress into the identity verification system.
     */
    UPLOAD_IN_PROGRESS,

    /**
     * Document is currently being verified in the identity verification system.
     */
    VERIFICATION_IN_PROGRESS,

    /**
     * Document has been rejected.
     */
    REJECTED,

    /**
     * An unrecoverable error occurred during document analysis.
     */
    FAILED

}