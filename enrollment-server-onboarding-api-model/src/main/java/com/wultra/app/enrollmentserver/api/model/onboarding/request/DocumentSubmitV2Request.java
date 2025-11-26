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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;

/**
 * REST API request body of submit documents V2.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Schema(description = "REST API request body of submit documents V2")
@Builder
public record DocumentSubmitV2Request(
        @Schema(description = "Process ID", example = "c3c6a4f4-9c53-4f4b-bc89-0c6c8e4dfb4a")
        String processId,

        @Schema(description = "List of documents to be submitted")
        List<Document> documents,

        @Schema(description = "Indicates whether documents in this request are re-submitted", example = "true", defaultValue = "false")
        boolean resubmit
) {

    @Schema(description = "Document to be submitted")
    @Builder
    public record Document(
            @Schema(description = "Original document ID. Required in case of re-submit", example = "f3b0c6c2-0f8a-4f2c-9e2c-b0d6c0b84e52")
            String originalDocumentId,

            @Schema(description = "Upload ID", example = "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6")
            String uploadId,

            @Schema(description = "Document image filename", example = "id_card_front.jpg")
            String filename,

            @Schema(description = "Document type", example = "ID_CARD")
            DocumentType type,

            @Schema(description = "Card side", example = "FRONT")
            CardSide side,

            @Schema(description = "Document image", example = "iVBORw0KGgoAAAANSUhEUgAA...", format = "byte")
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
