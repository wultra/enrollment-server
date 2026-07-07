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
package com.wultra.app.onboardingserver.statemachine.event;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event for {@link OnboardingState#COMPLETED_ACCEPTED}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Getter
public class OnboardingCompletedAcceptedEvent extends ApplicationEvent {

    private final transient OwnerId ownerId;
    private final String processId;

    /**
     * Creates a new event.
     *
     * @param source    the object on which the event initially occurred
     * @param ownerId   identification of the activation owner
     * @param processId onboarding process ID
     */
    public OnboardingCompletedAcceptedEvent(final Object source, final OwnerId ownerId, final String processId) {
        super(source);
        this.ownerId = ownerId;
        this.processId = processId;
    }
}
