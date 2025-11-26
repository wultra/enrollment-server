/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.api.model.onboarding.request;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;

/**
 * REST API request body of submit documents V2.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Builder
public record DocumentSubmitV2Request(
        String processId,
        List<Document> documents,
        boolean resubmit
) {

    @Builder
    public record Document(
            String originalDocumentId,
            String uploadId,
            String filename,
            DocumentType type,
            CardSide side,
            String data
    ) {

        @Override
        @NonNull
        public String toString() {
            return "Document[originalDocumentId=%s, uploadId=%s, filename=%s, type=%s, side=%s, data=%s]".formatted(
                    originalDocumentId,
                    uploadId,
                    filename,
                    type,
                    side,
                    data != null ? "length:" + data.length() : "null"
            );
        }
    }
}
