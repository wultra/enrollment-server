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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import lombok.Data;

import java.util.List;

/**
 * Result of verification of multiple identity-related documents.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
public class DocumentsVerificationResult {

    private String verificationId;
    private DocumentVerificationStatus status;
    private List<DocumentVerificationResult> results;
    private Integer verificationScore;
    private String rejectReason;
    private String errorDetail;

    /**
     * Identify if the document status is accepted.
     *
     * @return True if the document is accepted, false otherwise.
     */
    public boolean isAccepted() {
        return DocumentVerificationStatus.ACCEPTED.equals(status);
    }

}
