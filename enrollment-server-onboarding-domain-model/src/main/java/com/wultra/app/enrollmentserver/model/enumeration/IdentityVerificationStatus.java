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
 * Identity verification status enumeration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum IdentityVerificationStatus {

    /**
     * Default state before initialization of identity verification.
     */
    NOT_INITIALIZED,

    /**
     * Upload or verification of submitted documents is in progress.
     */
    IN_PROGRESS,

    /**
     * All submitted documents are waiting for verification.
     */
    VERIFICATION_PENDING,

    /**
     * All submitted documents have been verified and accepted as valid documents and OTP has been verified.
     */
    ACCEPTED,

    /**
     * One or more documents have been rejected.
     */
    REJECTED,

    /**
     * An unrecoverable error occurred during document analysis.
     */
    FAILED

}