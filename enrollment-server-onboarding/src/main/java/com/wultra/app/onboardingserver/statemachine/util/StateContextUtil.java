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
package com.wultra.app.onboardingserver.statemachine.util;

import com.google.common.base.Preconditions;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import io.getlime.core.rest.model.base.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateContext;

/**
 * State context util
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
public class StateContextUtil {

    public static void setResponseOk(StateContext<OnboardingState, OnboardingEvent> context, Response response) {
        Preconditions.checkArgument(
                !context.getStateMachine().hasStateMachineError(),
                String.format("Found state machine error in %s, when expected ok", context)
        );
        context.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_OBJECT, response);
        context.getExtendedState().getVariables().put(ExtendedStateVariable.RESPONSE_STATUS, HttpStatus.OK);
    }

}
