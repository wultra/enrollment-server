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

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Listener for {@link OnboardingCompletedAcceptedEvent}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
@Slf4j
public class OnboardingCompletedAcceptedListener {

    /**
     * Handles the {@link OnboardingCompletedAcceptedEvent} after transaction commit.
     *
     * @param event the completed-accepted event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOnboardingCompletedAccepted(final OnboardingCompletedAcceptedEvent event) {
        logger.info("",
                kv("action", "onOnboardingCompletedAccepted"),
                kv("state", "initiated"),
                kv("processId", event.getProcessId()),
                kv("userId", event.getOwnerId().getUserId()),
                kv("activationId", event.getOwnerId().getActivationId()));

        // TODO (michal-rozehnal-w, 2026-04-24, #1723) Sending is implemented in separate issue

        logger.info("",
                kv("action", "onOnboardingCompletedAccepted"),
                kv("state", "succeeded"));
    }
}

