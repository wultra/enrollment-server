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
package com.wultra.app.onboardingserver.impl.service.userdatastore;

import com.wultra.core.rest.client.base.RestClientConfiguration;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for user data store.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConfigurationProperties(prefix = "enrollment-server-onboarding.user-data-store")
@Getter
@Setter
@Validated
class UserDataStoreConfigProperties {

    /**
     * REST client configuration.
     */
    @NotNull
    private RestClientConfiguration restClientConfig;

    /**
     * Type of documents to store.
     */
    @NotNull
    private DocumentTypeFilter documentType;

    /**
     * Whether to store extracted data from documents.
     */
    private boolean storeExtractedData;

    /**
     * Whether to store document image scans.
     */
    private boolean storeDocumentImageScan;

    /**
     * Maximum number of attempts to store a document via the user data store client.
     */
    private int maxAttempts;

    /**
     * Document type filtering.
     */
    enum DocumentTypeFilter {

        /**
         * Store only documents with trusted images.
         */
        WITH_TRUSTED_IMAGE,

        /**
         * Store all documents.
         */
        ALL
    }
}
