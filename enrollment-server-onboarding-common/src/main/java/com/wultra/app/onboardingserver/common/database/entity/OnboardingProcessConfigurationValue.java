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

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Represent JSON with the configuration stored in {@link OnboardingProcessConfigurationEntity#getConfiguration()}.
 *
 * @param enabled                    Whether the process type is enabled.
 * @param otpForIdentification       Whether the OTP is required for the initial identification of the user.
 * @param otpForIdentityVerification Whether the OTP is required for identity verification - request OTP for the next process step.
 * @param useTemporaryActivation     Whether the onboarding process should use two activations, and exchange the temporary one for the permanent one.
 * @param documents                  List of documents.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Jacksonized
@Builder
public record OnboardingProcessConfigurationValue(
        boolean enabled,
        boolean otpForIdentification,
        boolean otpForIdentityVerification,
        boolean useTemporaryActivation,
        Documents documents,
        ActivationType activationType
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1995035336074261422L;

    public static class OnboardingProcessConfigurationValueBuilder {
        OnboardingProcessConfigurationValueBuilder() {
            enabled = false;
            otpForIdentification = false;
            otpForIdentityVerification = false;
            useTemporaryActivation = false;
            documents = Documents.builder().build();
            activationType = ActivationType.IDENTITY;
        }
    }

    /**
     * Configuration of required documents.
     *
     * @param totalRequiredDocumentsCount Number of required documents to submit.
     * @param groups                      Set of document groups.
     */
    @Jacksonized
    @Builder
    public record Documents(
            byte totalRequiredDocumentsCount,
            Set<Group> groups
    ) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1968756136278137531L;

        public static class DocumentsBuilder {
            DocumentsBuilder() {
                totalRequiredDocumentsCount = 0;
                groups = Set.of();
            }
        }
    }

    /**
     * Configuration of a document group.
     *
     * @param requiredDocumentsCount Number of required documents from this group.
     * @param items                  Set of document configurations.
     */
    @Jacksonized
    @Builder
    public record Group(
            byte requiredDocumentsCount,
            Set<Document> items
    ) implements Serializable {

        @Serial
        private static final long serialVersionUID = 5873241902384756123L;

        public static class GroupBuilder {
            GroupBuilder() {
                requiredDocumentsCount = 0;
                items = Set.of();
            }
        }
    }

    /**
     * Configuration of a single documentation type.
     *
     * @param type      document type
     * @param sideCount info if the document contains one or two sides
     */
    @Jacksonized
    @Builder
    public record Document(
            DocumentType type,
            byte sideCount
    ) implements Serializable {

        @Serial
        private static final long serialVersionUID = 191805503079489928L;

        public static class DocumentBuilder {
            DocumentBuilder() {
                sideCount = 1;
            }
        }
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
