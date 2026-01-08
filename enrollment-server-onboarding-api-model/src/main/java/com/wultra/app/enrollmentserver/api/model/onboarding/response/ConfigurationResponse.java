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

            @Schema(description = "Minimum number of different documents required for successful verification.", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
            int totalRequiredDocumentsCount,

            @Schema(description = "Groups defining per-group document requirements.", requiredMode = Schema.RequiredMode.REQUIRED)
            Set<Group> groups
    ) {}

    @Builder
    public record Group(

            @Schema(description = "Minimum number of documents required from this group", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
            int requiredDocumentsCount,

            @Schema(description = "List of documents specification.", requiredMode = Schema.RequiredMode.REQUIRED)
            Set<Document> items
    ) {}

    @Builder
    public record Document(

            @Schema(description = "Document type.", requiredMode = Schema.RequiredMode.REQUIRED)
            DocumentType type,

            @Schema(description = "Number of document sides.", requiredMode = Schema.RequiredMode.REQUIRED)
            byte sideCount
    ) {
    }

    public enum DocumentType {
        ID_CARD,
        PASSPORT,
        DRIVING_LICENCE
    }
}
