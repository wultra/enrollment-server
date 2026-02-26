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

package com.wultra.app.onboardingserver.provider.microblink;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Name of the field whose value is to be extracted from Microblink response.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Getter
public enum ExtractedDataField {
    GIVEN_NAMES("FirstName"),
    SURNAME("LastName"),
    DATE_OF_BIRTH("DateOfBirth"),
    PLACE_OF_BIRTH("PlaceOfBirth"),
    SEX("Sex"),
    NATIONALITY("Nationality"),
    PERSONAL_NUMBER("PersonalIdNumber"),
    DOCUMENT_NUMBER("DocumentNumber"),
    DATE_OF_ISSUE("DateOfIssue"),
    DATE_OF_EXPIRY("DateOfExpiry"),
    AUTHORITY("IssuingAuthority");

    private final String microblinkField;

    ExtractedDataField(String microblinkField) {
        this.microblinkField = microblinkField;
    }

    private static final Map<String, ExtractedDataField> VALUES_BY_MICROBLINK_FIELD =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            ExtractedDataField::getMicroblinkField,
                            Function.identity()
                    ));

    public static ExtractedDataField fromMicroblinkField(final String microblinkField) {
        return Optional.ofNullable(microblinkField)
                .map(it -> VALUES_BY_MICROBLINK_FIELD.getOrDefault(it, null))
                .orElse(null);
    }
}
