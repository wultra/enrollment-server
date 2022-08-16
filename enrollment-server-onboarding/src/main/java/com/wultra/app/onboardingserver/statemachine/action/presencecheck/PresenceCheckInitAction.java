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
package com.wultra.app.onboardingserver.statemachine.action.presencecheck;

import com.wultra.app.enrollmentserver.api.model.onboarding.response.PresenceCheckInitResponse;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.impl.service.PresenceCheckService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.app.onboardingserver.statemachine.util.StateContextUtil;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action to initialize the presence check process
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class PresenceCheckInitAction implements Action<OnboardingState, OnboardingEvent> {

    private final PresenceCheckService presenceCheckService;

    @Autowired
    public PresenceCheckInitAction(PresenceCheckService presenceCheckService) {
        this.presenceCheckService = presenceCheckService;
    }

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        SessionInfo sessionInfo = null;
        try {
            sessionInfo = presenceCheckService.init(ownerId, processId);
        } catch (DocumentVerificationException | IdentityVerificationException | OnboardingProcessLimitException |
                 PresenceCheckException | PresenceCheckLimitException | RemoteCommunicationException e) {
            context.getStateMachine().setStateMachineError(e);
        }
        if (sessionInfo != null && !context.getStateMachine().hasStateMachineError()) {
            final PresenceCheckInitResponse response = new PresenceCheckInitResponse();
            response.setSessionAttributes(sessionInfo.getSessionAttributes());
            StateContextUtil.setResponseOk(context, new ObjectResponse<>(response));
        }
    }

}
