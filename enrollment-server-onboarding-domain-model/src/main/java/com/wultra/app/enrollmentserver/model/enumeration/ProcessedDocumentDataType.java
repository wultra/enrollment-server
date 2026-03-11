/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
 * Type of processed document data type.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
public enum ProcessedDocumentDataType {

    /**
     * Face image extracted from the document.
     */
    FACE_IMAGE,

    /**
     * Front side of a provided document.
     */
    DOCUMENT_FRONT_SIDE,

    /**
     * Back side of a provided document.
     */
    DOCUMENT_BACK_SIDE
}
