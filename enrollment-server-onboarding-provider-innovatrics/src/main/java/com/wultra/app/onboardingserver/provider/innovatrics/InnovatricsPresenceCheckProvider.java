/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.innovatrics;

import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.api.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.api.provider.PresenceCheckProvider;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link PresenceCheckProvider} with <a href="https://www.innovatrics.com/">Innovatrics</a>.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "innovatrics")
@Component
@Slf4j
@AllArgsConstructor
class InnovatricsPresenceCheckProvider implements PresenceCheckProvider {

    private final InnovatricsApiService innovatricsApiService;

    @Override
    public void initPresenceCheck(final OwnerId id, final Image photo) {
        logger.debug("#initPresenceCheck does nothing for Innovatrics, {}", id);
    }

    @Override
    public SessionInfo startPresenceCheck(final OwnerId id) {
        logger.debug("#startPresenceCheck does nothing for Innovatrics, {}", id);
        return new SessionInfo();
    }

    @Override
    public PresenceCheckResult getResult(final OwnerId id, final SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException {
        // TODO (racansky, 2023-11-28)
        return null;
    }

    @Override
    public void cleanupIdentityData(OwnerId id) throws PresenceCheckException, RemoteCommunicationException {
        // TODO (racansky, 2023-11-28)
    }
}
