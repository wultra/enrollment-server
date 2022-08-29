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
package com.wultra.app.onboardingserver.statemachine.guard.status;

import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Guard to check accepted flag.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
public class StatusAcceptedGuard implements Guard<OnboardingState, OnboardingEvent> {

    public static final String KEY_ACCEPTED = "accepted";

    @Override
    public boolean evaluate(StateContext<OnboardingState, OnboardingEvent> context) {
        final Map<Object, Object> variables = context.getExtendedState().getVariables();
        return Boolean.TRUE.equals(variables.get(KEY_ACCEPTED));
    }
}
