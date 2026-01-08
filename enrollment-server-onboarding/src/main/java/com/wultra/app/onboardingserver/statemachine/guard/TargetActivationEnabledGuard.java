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
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationTargetActivationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The guards whether the target activation is enabled.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class TargetActivationEnabledGuard extends GuardAdapter {

    private final IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    @Override
    protected boolean evaluate(final String processId, final OwnerId ownerId) {
        try {
            final boolean result = identityVerificationTargetActivationService.isTargetActivationEnabled(processId);
            logger.debug("Target activation is enabled: {} for processId: {}, {}", result, processId, ownerId);
            return result;
        } catch (OnboardingProcessException e) {
            logger.error("Error fetching configuration for processId: {}, {}", processId, ownerId, e);
            return false;
        }
    }
}
