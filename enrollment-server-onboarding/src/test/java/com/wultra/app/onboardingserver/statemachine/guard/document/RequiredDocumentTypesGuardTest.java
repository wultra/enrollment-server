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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for {@link RequiredDocumentTypesGuard}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class RequiredDocumentTypesGuardTest {

    private final RequiredDocumentTypesGuard tested = new RequiredDocumentTypesGuard();

    @Test
    void testOnlyDrivingLicence() {
        final var documentVerifications = List.of(createDocumentVerification(DocumentType.DRIVING_LICENSE));

        boolean result = tested.evaluate(documentVerifications, "1");
        assertFalse(result);
    }

    @Test
    void testOnlyIdCard() {
        final var documentVerifications = List.of(createDocumentVerification(DocumentType.ID_CARD));

        boolean result = tested.evaluate(documentVerifications, "1");
        assertFalse(result);
    }

    @Test
    void testIdCardAndDrivingLicence() {
        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        boolean result = tested.evaluate(documentVerifications, "1");
        assertTrue(result);
    }

    @Test
    void testTravelPassportAndDrivingLicence() {
        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.PASSPORT),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        boolean result = tested.evaluate(documentVerifications, "1");
        assertTrue(result);
    }

    private DocumentVerificationEntity createDocumentVerification(final DocumentType type) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setType(type);
        documentVerification.setStatus(DocumentStatus.ACCEPTED);
        return documentVerification;
    }
}
