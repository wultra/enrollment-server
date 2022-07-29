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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import io.getlime.core.rest.model.base.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action to initialize the verification
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class VerificationInitAction implements Action<EnrollmentState, EnrollmentEvent> {

    private final IdentityVerificationService identityVerificationService;

    @Autowired
    public VerificationInitAction(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    @Override
    public void execute(StateContext<EnrollmentState, EnrollmentEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        IdentityVerificationEntity identityVerification = null;
        try {
            identityVerification = identityVerificationService.initializeIdentityVerification(ownerId, processId);
        } catch (IdentityVerificationException | OnboardingProcessLimitException | RemoteCommunicationException e) {
            context.getStateMachine().setStateMachineError(e);
        }
        if (identityVerification != null) {
            context.getExtendedState().getVariables().put(ExtendedStateVariable.IDENTITY_VERIFICATION, identityVerification);
        }
        if (!context.getStateMachine().hasStateMachineError()) {
            context.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_OBJECT, new Response());
            context.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_STATUS, HttpStatus.OK);
        }
    }

}
