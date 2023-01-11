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
package com.wultra.app.onboardingserver.statemachine.guard;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckNotEnabledException;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard to ensure enabled presence check
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class PresenceCheckEnabledGuard implements Guard<OnboardingState, OnboardingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PresenceCheckEnabledGuard.class);

    private final IdentityVerificationConfig identityVerificationConfig;

    @Autowired
    public PresenceCheckEnabledGuard(IdentityVerificationConfig identityVerificationConfig) {
        this.identityVerificationConfig = identityVerificationConfig;
    }

    @Override
    public boolean evaluate(StateContext<OnboardingState, OnboardingEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        String processId = (String) context.getMessageHeader(EventHeaderName.PROCESS_ID);

        if (!identityVerificationConfig.isPresenceCheckEnabled()) {
            logger.warn("Not enabled presence check: {}, {}", processId, ownerId);
            context.getStateMachine().setStateMachineError(new PresenceCheckNotEnabledException());
            return false;
        }
        return true;
    }

}
