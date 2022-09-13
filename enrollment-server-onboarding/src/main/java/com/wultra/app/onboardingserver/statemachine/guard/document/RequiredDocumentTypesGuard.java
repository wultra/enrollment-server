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
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Guard for presence of all required documents.
 * <p>
 * It means ID card or travel passport, and driving licence.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO (racansky, 2022-09-09) should be Guard for Spring State Machine, but called from job so far
@Component
@Slf4j
public class RequiredDocumentTypesGuard {

    /**
     * Evaluate all required document types.
     *
     * @param documentVerifications document verifications to evaluate
     * @param identityVerificationId identity verification ID to log
     * @return true when all required document types present
     */
    public boolean evaluate(final Collection<DocumentVerificationEntity> documentVerifications, final String identityVerificationId) {
        if (documentVerifications.isEmpty()) {
            logger.debug("There is no document uploaded yet for identity verification ID: {}", identityVerificationId);
            return false;
        } else if (!containsIdOrPassport(documentVerifications)) {
            logger.debug("There is no ID card or travel passport uploaded yet for identity verification ID: {}", identityVerificationId);
            return false;
        } else if (!containsDrivingLicence(documentVerifications)) {
            logger.debug("There is no driving licence uploaded yet for identity verification ID: {}", identityVerificationId);
            return false;
        } else {
            logger.debug("All required documents uploaded for identity verification ID: {}", identityVerificationId);
            return true;
        }
    }

    private static boolean containsIdOrPassport(final Collection<DocumentVerificationEntity> documentVerifications) {
        return documentVerifications.stream()
                .map(DocumentVerificationEntity::getType)
                .anyMatch(it -> it == DocumentType.ID_CARD || it == DocumentType.PASSPORT);
    }

    private static boolean containsDrivingLicence(final Collection<DocumentVerificationEntity> documentVerifications) {
        return documentVerifications.stream()
                .map(DocumentVerificationEntity::getType)
                .anyMatch(it -> it == DocumentType.DRIVING_LICENSE);
    }
}
