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
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationFinishService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action to process verification result
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class VerificationProcessResultAction implements Action<EnrollmentState, EnrollmentEvent> {

    private static final Logger logger = LoggerFactory.getLogger(VerificationProcessResultAction.class);

    private final IdentityVerificationFinishService identityVerificationFinishService;

    private final IdentityVerificationService identityVerificationService;

    @Autowired
    public VerificationProcessResultAction(IdentityVerificationFinishService identityVerificationFinishService, IdentityVerificationService identityVerificationService) {
        this.identityVerificationFinishService = identityVerificationFinishService;
        this.identityVerificationService = identityVerificationService;
    }

    @Override
    public void execute(StateContext<EnrollmentState, EnrollmentEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        IdentityVerificationEntity identityVerification = (IdentityVerificationEntity) context.getMessageHeader(EventHeaderName.IDENTITY_VERIFICATION);
        identityVerificationService.processDocumentVerificationResult(ownerId, identityVerification, IdentityVerificationPhase.COMPLETED);
        if (identityVerification.getStatus() == IdentityVerificationStatus.ACCEPTED) {
            try {
                identityVerificationFinishService.finishIdentityVerification(ownerId);
            } catch (OnboardingProcessException | RemoteCommunicationException e) {
                context.getStateMachine().setStateMachineError(e);
            }
        }
    }

}
