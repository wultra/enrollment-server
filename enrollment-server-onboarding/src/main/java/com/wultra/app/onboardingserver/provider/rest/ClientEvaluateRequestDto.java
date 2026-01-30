/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * Request object for client evaluation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
class ClientEvaluateRequestDto {

    private String processId;

    private String processType;

    /**
     * Identifier of verification in identity verification system under which the uploaded documents were verified.
     */
    private String identityVerificationId;

    private String userId;

    private String verificationId;

    private String provider;

    private Status status;

    private Integer score;

    private DocumentCheckResult documentCheckResult;

    /**
     * Data extracted from each document/page. Format is defined by the document verification provider used.
     *
     * @deprecated
     */
    @Deprecated(forRemoval = true, since = "2.1.0")
    private List<String> extractedData;

    public enum Status {
        SUCCESS,
        FAILURE
    }

    public record DocumentCheckResult(
            List<Document> documents
    ) {}

    @Builder
    public record Document(
            DocumentType type,
            Status status,
            DocumentData data,
            List<Image> images,
            String rawData
    ) {}

    @Builder
    public record DocumentData(
            String givenNames,
            String surname,
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
            ImageType type,
            byte[] data
    ) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Image other)) return false;
            return type == other.type && Arrays.equals(data, other.data);
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + Arrays.hashCode(data);
            return result;
        }
    }

    public enum ImageType {
        FACE
    }

    public enum DocumentType {
        ID_CARD,
        DRIVING_LICENCE,
        PASSPORT
    }
}
