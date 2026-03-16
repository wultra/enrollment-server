/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wultra.app.onboardingserver.statemachine.guard;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationStatusService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Guard that checks whether the consent is pending.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class ConsentPendingGuard extends GuardAdapter {

    private final OnboardingProcessRepository onboardingProcessRepository;
    private final IdentityVerificationStatusService identityVerificationStatusService;

    @Override
    protected boolean evaluate(final String processId, final OwnerId ownerId) {
        final var process = onboardingProcessRepository.findById(processId)
                .orElse(null);

        if (process == null) {
            logger.warn("Process not found, processId: {}", processId);
            return false;
        }

        try {
            final var consentPending = identityVerificationStatusService.isConsentPending(process);
            logger.info("Consent pending: {}, processId: {}", consentPending, processId);
            return !consentPending;
        } catch (final IllegalStateException e) {
            logger.warn("Exception when checking consent pending, processId: {}", processId, e);
            return false;
        }
    }
}
