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
 *
 */
package com.wultra.app.onboardingserver.provider.model.request;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.core.annotations.PublicApi;
import lombok.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Request object for {@link OnboardingProvider#evaluateClient(EvaluateClientRequest)}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Getter
@ToString
@PublicApi
@EqualsAndHashCode
public final class EvaluateClientRequest {

    @NonNull
    private String processId;

    @NonNull
    private String processType;

    @NonNull
    private String userId;

    @NonNull
    private String identityVerificationId;

    @NonNull
    private String verificationId;

    private String provider;

    private Status status;

    private DocumentCheckResult documentCheckResult;

    public enum Status {
        SUCCESS,
        FAILURE
    }

    @Builder
    public record DocumentCheckResult(
            List<Document> documents,
            Person person
    ) {}

    @Builder
    public record Person(
            String surname,
            String givenNames,
            LocalDate dateOfBirth
    ) {}

    @Builder
    public record Document(
            DocumentType type,
            String country,
            Status status,
            Integer score,
            DocumentData data,
            List<Image> images,
            String rawData
    ) {
    }

    @Builder
    public record DocumentData(
            String givenNames,
            String surname,
            LocalDate dateOfBirth,
            String placeOfBirth,
            String sex,
            String nationality,
            String personalNumber,
            String documentNumber,
            LocalDate dateOfIssue,
            LocalDate dateOfExpiry,
            String authority
    ) {}

    @Builder
    public record Image(
            ProcessedDocumentDataType type,
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
}
