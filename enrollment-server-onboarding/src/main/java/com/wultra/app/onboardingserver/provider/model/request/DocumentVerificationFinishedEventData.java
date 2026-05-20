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
import lombok.*;

import java.util.List;

/**
 * {@link EventData} for {@link EventType#DOCUMENT_VERIFICATION_FINISHED}.
 * Contains the result for a single document returned by the verification provider.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Getter
@ToString
@PublicApi
@EqualsAndHashCode
public final class DocumentVerificationFinishedEventData implements EventData {

    @NonNull
    private String documentVerificationId;

    @NonNull
    private String documentId;

    @NonNull
    private EventStatus status;

    private String rejectReason;

    private String errorDetail;

    @NonNull
    private String provider;

    @NonNull
    private Integer score;

    /**
     * Optional details about the document and extracted data.
     * Present only when {@code status} is {@code ACCEPTED} or {@code REJECTED}.
     */
    private DocumentVerificationResult documentVerificationResult;

    @Builder
    @Getter
    @ToString
    @PublicApi
    @EqualsAndHashCode
    public static class DocumentVerificationResult {

        private String type;

        private String country;

        private DocumentData data;

        private List<Image> images;

        private Object rawData;
    }

    @Builder
    @Getter
    @ToString
    @PublicApi
    @EqualsAndHashCode
    public static class DocumentData {

        private String surname;
        private String givenNames;
        private String dateOfBirth;
        private String placeOfBirth;
        private String sex;
        private String nationality;
        private String personalNumber;
        private String documentNumber;
        private String dateOfIssue;
        private String dateOfExpiry;
        private String authority;
    }

    @Builder
    @Getter
    @ToString
    @PublicApi
    @EqualsAndHashCode
    public static class Image {

        @NonNull
        private String type;

        @NonNull
        private String data;
    }
}
