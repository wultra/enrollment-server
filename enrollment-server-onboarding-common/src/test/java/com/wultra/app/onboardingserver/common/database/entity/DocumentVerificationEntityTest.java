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
package com.wultra.app.onboardingserver.common.database.entity;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link DocumentVerificationEntity}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class DocumentVerificationEntityTest {

    @Test
    void testFilterPreferredDocumentWithPhoto() {
        final List<DocumentVerificationEntity> input = List.of(
                createDocumentVerification(DocumentType.UNKNOWN),
                createDocumentVerification(DocumentType.DRIVING_LICENSE),
                createDocumentVerification(DocumentType.PASSPORT)
        );

        final Optional<DocumentVerificationEntity> result = DocumentVerificationEntity.filterPreferredDocumentWithPhoto(input);

        assertTrue(result.isPresent());
        assertEquals(DocumentType.PASSPORT, result.get().getType());
    }

    @Test
    void testFilterPreferredDocumentWithPhoto_empty() {
        final List<DocumentVerificationEntity> input = List.of(
                createDocumentVerification(DocumentType.UNKNOWN)
        );

        final Optional<DocumentVerificationEntity> result = DocumentVerificationEntity.filterPreferredDocumentWithPhoto(input);

        assertFalse(result.isPresent());
    }

    private static DocumentVerificationEntity createDocumentVerification(final DocumentType documentType) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setType(documentType);
        return documentVerification;
    }
}
