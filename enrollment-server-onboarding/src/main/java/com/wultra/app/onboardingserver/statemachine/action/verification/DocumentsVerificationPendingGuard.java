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
package com.wultra.app.onboardingserver.statemachine.action.verification;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
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
 * Guard that all documents for verification of the given identity verification are in status {@code VERIFICATION_PENDING} or {@code ACCEPTED}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class DocumentsVerificationPendingGuard implements Guard<OnboardingState, OnboardingEvent> {

    private final DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    public DocumentsVerificationPendingGuard(final DocumentVerificationRepository documentVerificationRepository) {
        this.documentVerificationRepository = documentVerificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean evaluate(final StateContext<OnboardingState, OnboardingEvent> context) {
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        final List<DocumentVerificationEntity> documentVerifications = documentVerificationRepository.findAllUsedForVerification(identityVerification);
        if (documentVerifications.isEmpty()) {
            logger.debug("No document uploaded yet for {}", identityVerification);
            return false;
        }

        final boolean allDocumentsPendingVerification = documentVerifications.stream()
                .map(DocumentVerificationEntity::getStatus)
                .allMatch(it -> it == DocumentStatus.VERIFICATION_PENDING || it == DocumentStatus.ACCEPTED);

        if (allDocumentsPendingVerification) {
            logger.info("All documents for verification of {} are pending verification or accepted", identityVerification);
        } else {
            logger.debug("Not all documents for verification of {} are pending verification or accepted", identityVerification);
        }

        return allDocumentsPendingVerification;
    }
}
