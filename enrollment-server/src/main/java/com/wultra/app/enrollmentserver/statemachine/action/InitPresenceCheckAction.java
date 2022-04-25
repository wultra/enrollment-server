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
package com.wultra.app.enrollmentserver.statemachine.action;

import com.wultra.app.enrollmentserver.api.model.response.PresenceCheckInitResponse;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.service.PresenceCheckService;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.enrollmentserver.statemachine.EventHeaderName;
import com.wultra.app.enrollmentserver.statemachine.ExtendedStateVariable;
import com.wultra.app.enrollmentserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.enrollmentserver.statemachine.enums.EnrollmentState;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action to initialize the presence check process
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class InitPresenceCheckAction implements Action<EnrollmentState, EnrollmentEvent> {

    private final PresenceCheckService presenceCheckService;

    @Autowired
    public InitPresenceCheckAction(PresenceCheckService presenceCheckService) {
        this.presenceCheckService = presenceCheckService;
    }

    @Override
    public void execute(StateContext<EnrollmentState, EnrollmentEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        SessionInfo sessionInfo = null;
        try {
            sessionInfo = presenceCheckService.init(ownerId, processId);
        } catch (DocumentVerificationException e) {
            context.getStateMachine().setStateMachineError(e);
        } catch (PresenceCheckException e) {
            context.getStateMachine().setStateMachineError(e);
        }
        if (sessionInfo != null && !context.getStateMachine().hasStateMachineError()) {
            final PresenceCheckInitResponse response = new PresenceCheckInitResponse();
            response.setSessionAttributes(sessionInfo.getSessionAttributes());

            context.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_OBJECT, new ObjectResponse<>(response));
            context.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_STATUS, HttpStatus.OK);
        }
    }

}
