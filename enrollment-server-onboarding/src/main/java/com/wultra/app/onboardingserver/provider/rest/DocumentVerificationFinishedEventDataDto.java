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
package com.wultra.app.onboardingserver.provider.rest;

import lombok.Builder;

import java.util.List;

/**
 * {@link EventDataDto} for {@link EventTypeDto#DOCUMENT_VERIFICATION_FINISHED}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
record DocumentVerificationFinishedEventDataDto(DocumentVerification documentVerification) implements EventDataDto {

    @Builder
    public record DocumentVerification(
            String documentVerificationId,
            String documentId,
            String status,
            String rejectReason,
            String errorDetail,
            String provider,
            Integer score,
            DocumentVerificationResult documentVerificationResult
    ) {}

    @Builder
    public record DocumentVerificationResult(
            String type,
            String country,
            DocumentData data,
            List<Image> images,
            Object rawData
    ) {}

    @Builder
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
    public record Image(
            String type,
            String data
    ) {}
}
