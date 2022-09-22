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

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Action to move the given identity verification to {@code DOCUMENT_UPLOAD / VERIFICATION_PENDING}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class MoveToDocumentUploadVerificationPendingAction implements Action<OnboardingState, OnboardingEvent> {

    private final IdentityVerificationRepository identityVerificationRepository;

    @Autowired
    public MoveToDocumentUploadVerificationPendingAction(final IdentityVerificationRepository identityVerificationRepository) {
        this.identityVerificationRepository = identityVerificationRepository;
    }

    @Override
    @Transactional
    public void execute(StateContext <OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
        moveToDocumentUploadVerificationPending(ownerId, identityVerification);
    }

    private void moveToDocumentUploadVerificationPending(final OwnerId ownerId, final IdentityVerificationEntity idVerification) {
        idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
        idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        identityVerificationRepository.save(idVerification);
        logger.info("Switched to DOCUMENT_UPLOAD/VERIFICATION_PENDING; process ID: {}", idVerification.getProcessId());
    }
}
