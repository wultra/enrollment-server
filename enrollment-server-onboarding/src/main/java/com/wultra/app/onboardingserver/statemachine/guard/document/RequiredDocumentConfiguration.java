/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine.guard.document;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;
import java.util.List;

/**
 * Configuration for {@link RequiredDocumentTypesCheck}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConfigurationProperties(prefix = "enrollment-server-onboarding.document-verification.required")
@Getter
@Setter
class RequiredDocumentConfiguration {

    /**
     * Required primary document types to be present. Possible values: {@code ID_CARD, PASSPORT}.
     */
    private Collection<DocumentType> primaryDocuments = List.of(DocumentType.ID_CARD);

    /**
     * Required count of documents to be present.
     */
    private int count = 2;
}
