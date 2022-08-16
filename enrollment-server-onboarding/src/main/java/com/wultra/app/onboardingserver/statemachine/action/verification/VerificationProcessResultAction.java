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
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationFinishService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Action to process verification result
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class VerificationProcessResultAction implements Action<OnboardingState, OnboardingEvent> {

    private final TransactionTemplate transactionTemplate;

    private final IdentityVerificationFinishService identityVerificationFinishService;

    private final IdentityVerificationService identityVerificationService;

    @Autowired
    public VerificationProcessResultAction(
            TransactionTemplate transactionTemplate,
            IdentityVerificationFinishService identityVerificationFinishService,
            IdentityVerificationService identityVerificationService) {
        this.transactionTemplate = transactionTemplate;
        this.identityVerificationFinishService = identityVerificationFinishService;
        this.identityVerificationService = identityVerificationService;
    }

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                // TODO Consider move to new DocumentVerificationService
                identityVerificationService.processDocumentVerificationResult(ownerId, identityVerification, IdentityVerificationPhase.COMPLETED);
                if (identityVerification.getStatus() == IdentityVerificationStatus.ACCEPTED) {
                    try {
                        identityVerificationFinishService.finishIdentityVerification(ownerId);
                    } catch (IdentityVerificationException | OnboardingProcessException | RemoteCommunicationException e) {
                        context.getStateMachine().setStateMachineError(e);
                    }
                }
            }

        });
    }

}
