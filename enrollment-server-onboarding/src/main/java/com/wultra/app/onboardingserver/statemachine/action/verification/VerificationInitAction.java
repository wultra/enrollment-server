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
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationCreateService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import io.getlime.core.rest.model.base.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Action to initialize the verification
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class VerificationInitAction implements Action<OnboardingState, OnboardingEvent> {

    private final IdentityVerificationCreateService identityVerificationCreateService;

    @Autowired
    public VerificationInitAction(IdentityVerificationCreateService identityVerificationCreateService) {
        this.identityVerificationCreateService = identityVerificationCreateService;
    }

    @Override
    public void execute(final StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        IdentityVerificationEntity identityVerification = null;
        try {
            identityVerification = identityVerificationCreateService.createIdentityVerification(ownerId, processId);
        } catch (IdentityVerificationException | OnboardingProcessLimitException | RemoteCommunicationException e) {
            context.getStateMachine().setStateMachineError(e);
        }

        final Map<Object, Object> variables = context.getExtendedState().getVariables();
        if (identityVerification != null) {
            variables.put(ExtendedStateVariable.IDENTITY_VERIFICATION, identityVerification);
        }
        if (!context.getStateMachine().hasStateMachineError()) {
            variables.put(ExtendedStateVariable.RESPONSE_OBJECT, new Response());
            variables.put(ExtendedStateVariable.RESPONSE_STATUS, HttpStatus.OK);
        }
    }

}
