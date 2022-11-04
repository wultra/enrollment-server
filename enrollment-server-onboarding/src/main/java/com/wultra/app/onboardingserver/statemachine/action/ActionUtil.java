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
package com.wultra.app.onboardingserver.statemachine.action;

import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import reactor.core.publisher.Mono;

/**
 * Provide common functionality to actions.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
public final class ActionUtil {

    private ActionUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void sendNextStateEvent(final StateContext<OnboardingState, OnboardingEvent> context) {
        final Object ownerId = context.getMessageHeader(EventHeaderName.OWNER_ID);
        final Message<OnboardingEvent> message = MessageBuilder.withPayload(OnboardingEvent.EVENT_NEXT_STATE)
                .setHeader(EventHeaderName.OWNER_ID, ownerId)
                .setHeader(EventHeaderName.PROCESS_ID, context.getMessageHeader(EventHeaderName.PROCESS_ID))
                .build();

        logger.debug("Sending EVENT_NEXT_STATE {}, {}", message, ownerId);
        context.getStateMachine().sendEvent(Mono.just(message)).subscribe();
    }
}
