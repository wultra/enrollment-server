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
package com.wultra.app.onboardingserver.common.database.entity;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Represent JSON with the configuration stored in {@link OnboardingProcessConfigurationEntity#getConfiguration()}.
 *
 * @param enabled                    Whether the process type is enabled.
 * @param otpForIdentification       Whether the OTP is required for the initial identification of the user.
 * @param otpForIdentityVerification Whether the OTP is required for identity verification - request OTP for the next process step.
 * @param documents                  List of documents.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Jacksonized
@Builder
public record OnboardingProcessConfigurationValue(
        boolean enabled,
        boolean otpForIdentification,
        boolean otpForIdentityVerification,
        Documents documents,
        ActivationType activationType
) {
    public static class OnboardingProcessConfigurationValueBuilder {
        OnboardingProcessConfigurationValueBuilder() {
            enabled = false;
            otpForIdentification = false;
            otpForIdentityVerification = false;
            documents = new Documents((byte) 0, List.of());
            activationType = ActivationType.IDENTITY;
        }
    }

    /**
     * @param requiredDocumentsCount Number of required documents to submit.
     */
    public record Documents(
            byte requiredDocumentsCount,
            List<Document> items
    ) {
    }

    /**
     *
     * @param type      document type
     * @param mandatory whether the document is mandatory
     * @param sideCount info if the document contains one or two sides
     */
    public record Document(
            DocumentType type,
            boolean mandatory,
            byte sideCount
    ) {
    }

    public enum DocumentType {
        ID_CARD,
        PASSPORT,
        DRIVING_LICENCE
    }

    public enum ActivationType {
        /**
         * Activation is initialized by the onboarding server and the activation code is returned when the process starts.
         */
        CODE,

        /**
         * Activation is initialized by SDK.
         */
        IDENTITY
    }
}
