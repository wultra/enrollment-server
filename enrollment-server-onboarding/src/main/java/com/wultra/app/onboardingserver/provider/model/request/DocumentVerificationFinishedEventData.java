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
 *
 */
package com.wultra.app.onboardingserver.provider.model.request;

import com.wultra.core.annotations.PublicApi;
import lombok.Builder;
import lombok.NonNull;

import java.util.List;

/**
 * {@link EventData} for {@link EventType#DOCUMENT_VERIFICATION_FINISHED}.
 * Contains the result for a single document returned by the verification provider.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@PublicApi
public record DocumentVerificationFinishedEventData(
        @NonNull String documentVerificationId,
        @NonNull String documentId,
        @NonNull EventStatus status,
        String rejectReason,
        String errorDetail,
        @NonNull String provider,
        @NonNull Integer score,
        DocumentVerificationResult documentVerificationResult
) implements EventData {

    /**
     * Optional details about the document and extracted data.
     * Present only when {@code status} is {@code ACCEPTED} or {@code REJECTED}.
     */
    @Builder
    @PublicApi
    public record DocumentVerificationResult(
            String type,
            String country,
            DocumentData data,
            List<DocumentImage> images,
            Object rawData
    ) {}

    @Builder
    @PublicApi
    public record DocumentData(
            String surname,
            String givenNames,
            String dateOfBirth,
            String placeOfBirth,
            String sex,
            String nationality,
            String personalNumber,
            String documentNumber,
            String dateOfIssue,
            String dateOfExpiry,
            String authority
    ) {}

    @Builder
    @PublicApi
    public record DocumentImage(
            @NonNull String type,
            @NonNull String data
    ) {}
}
