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
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Guard for presence of all required documents.
 * <p>
 * It means ID card or travel passport, and driving licence.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class RequiredDocumentTypesGuard implements Guard<OnboardingState, OnboardingEvent> {

    private final DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    public RequiredDocumentTypesGuard(final DocumentVerificationRepository documentVerificationRepository) {
        this.documentVerificationRepository = documentVerificationRepository;
    }

    @Override
    @Transactional
    public boolean evaluate(final StateContext<OnboardingState, OnboardingEvent> context) {
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
        final List<DocumentVerificationEntity> documentVerifications =
                documentVerificationRepository.findAllUsedForVerification(identityVerification);
        final String id = identityVerification.getId();
        if (documentVerifications.isEmpty()) {
            logger.debug("There is no document uploaded yet for identity verification ID: {}", id);
            return false;
        } else if (!containsIdOrPassport(documentVerifications)) {
            logger.debug("There is no ID card or travel passport uploaded yet for identity verification ID: {}", id);
            return false;
        } else if (!containsDrivingLicence(documentVerifications)) {
            logger.debug("There is no driving licence uploaded yet for identity verification ID: {}", id);
            return false;
        } else {
            logger.debug("All required documents uploaded for identity verification ID: {}", id);
            return true;
        }
    }

    private static boolean containsIdOrPassport(final List<DocumentVerificationEntity> documentVerifications) {
        return documentVerifications.stream()
                .filter(it -> it.getStatus() == DocumentStatus.VERIFICATION_PENDING)
                .map(DocumentVerificationEntity::getType)
                .anyMatch(it -> it == DocumentType.ID_CARD || it == DocumentType.PASSPORT);
    }

    private static boolean containsDrivingLicence(final List<DocumentVerificationEntity> documentVerifications) {
        return documentVerifications.stream()
                .filter(it -> it.getStatus() == DocumentStatus.VERIFICATION_PENDING)
                .map(DocumentVerificationEntity::getType)
                .anyMatch(it -> it == DocumentType.DRIVING_LICENSE);
    }
}
