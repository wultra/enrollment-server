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
package com.wultra.app.enrollmentserver.api.model.onboarding.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Set;

/**
 * Configuration response.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
public record ConfigurationResponse(

        @Schema(description = "Whether the process type is enabled.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean enabled,

        @Schema(description = "Whether the OTP is required for the initial identification of the user.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean otpForIdentification,

        @Schema(description = "Whether the OTP is required for identity verification - request OTP for the next process step.", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean otpForIdentityVerification,

        @Schema(description = "List of documents.")
        Documents documents
) {

    @Builder
    public record Documents(

            @Schema(description = "Number of required documents to submit.", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
            int requiredTotalDocumentsCount,

            @Schema(description = "Number of required primary documents to submit.", requiredMode = Schema.RequiredMode.REQUIRED)
            int requiredPrimaryDocumentsCount,

            List<Document> items
    ) {
    }

    @Builder
    public record Document(

            @Schema(description = "Document type.", requiredMode = Schema.RequiredMode.REQUIRED)
            DocumentType type,

            @Schema(description = "Set of obligation of document", requiredMode = Schema.RequiredMode.REQUIRED)
            Set<DocumentObligation> obligation,

            @Schema(description = "Number of document sides.", requiredMode = Schema.RequiredMode.REQUIRED)
            byte sideCount
    ) {
    }

    public enum DocumentType {
        ID_CARD,
        PASSPORT,
        DRIVING_LICENCE
    }

    @Schema(description = "Obligation of document")
    public enum DocumentObligation {

        @Schema(description = "Document is mandatory and must be present for successful verification.")
        MANDATORY,

        @Schema(description = "Document is one of primary. For successful verification min count of primary documents specified in `` must be present.")
        PRIMARY
    }
}
