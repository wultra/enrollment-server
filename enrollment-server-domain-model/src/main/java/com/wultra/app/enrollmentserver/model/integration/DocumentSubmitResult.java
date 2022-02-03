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
package com.wultra.app.enrollmentserver.model.integration;

import lombok.Data;

/**
 * Result of submission of a single identity-related document.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Data
public class DocumentSubmitResult {

    /**
     * Simple JSON to cover the case of no extracted data.
     */
    public static final String NO_DATA_EXTRACTED = "{}";

    /**
     * Identification of the document in our database
     */
    private String documentId;

    /**
     * Remotely generated document upload identifier
     */
    private String uploadId;

    /**
     * A reason why document was rejected in case the document was not accepted
     */
    private String rejectReason;

    /**
     * Validation result in JSON format
     */
    private String validationResult;

    /**
     * Error detail used in case the document processing failed
     */
    private String errorDetail;

    /**
     * Extracted data from document in JSON format. A document submit in progress contains null extracted data.
     */
    private String extractedData;

}
