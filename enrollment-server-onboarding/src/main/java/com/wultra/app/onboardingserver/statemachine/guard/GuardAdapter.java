/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.statemachine.guard;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

/**
 * The guard adapter extracting context and passing it to {@link #evaluate(String, OwnerId)}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public abstract class GuardAdapter implements Guard<OnboardingState, OnboardingEvent> {

    @Override
    public boolean evaluate(StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        return evaluate(processId, ownerId);
    }

    /**
     * Evaluate a guard condition.
     *
     * @param processId Process identifier.
     * @param ownerId Owner identification.
     * @return true, if guard evaluation is successful, false otherwise.
     */
    protected abstract boolean evaluate(final String processId, final OwnerId ownerId);
}
