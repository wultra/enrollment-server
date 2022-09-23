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

import java.util.Arrays;
import java.util.List;

/**
 * Verified document type.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public enum DocumentType {

    /**
     * Identity card.
     */
    ID_CARD {

        /**
         * Identifies if the document is supposed to be two-sided. For ID_CARD document, the document is two-sided.
         *
         * @return True.
         */
        @Override
        public boolean isTwoSided() {
            return true;
        }

    },

    /**
     * Passport.
     */
    PASSPORT,

    /**
     * Driving license.
     */
    DRIVING_LICENSE,

    /**
     * Selfie photo.
     */
    SELFIE_PHOTO,

    /**
     * Selfie video.
     */
    SELFIE_VIDEO,

    /**
     * Unknown document.
     */
    UNKNOWN;

    /**
     * Document types ordered by the preference to provide a person photo (the most preferred type is the first)
     */
    public static final List<DocumentType> PREFERRED_SOURCE_OF_PERSON_PHOTO = Arrays.asList(
            DocumentType.ID_CARD,
            DocumentType.PASSPORT,
            DocumentType.DRIVING_LICENSE
    );

    /**
     * Identifies if the document is supposed to be two-sided. Override the value for a specific document type.
     *
     * @return True if the document is two-sided, false otherwise.
     */
    public boolean isTwoSided() {
        return false;
    }

}
