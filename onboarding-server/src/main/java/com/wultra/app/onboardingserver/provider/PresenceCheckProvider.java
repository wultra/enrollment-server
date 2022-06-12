/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider;

import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;

/**
 * Provider which allows customization of the presence check.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public interface PresenceCheckProvider {

    /**
     * Initializes presence check process.
     *
     * @param id Owner identification.
     * @param photo Trusted photo of the user.
     * @throws PresenceCheckException When an error during initialization occurred.
     */
    void initPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException;

    /**
     * Starts the presence check process. The process has to be initialized before this call.
     *
     * @param id Owner identification.
     * @return Session info with data related to the presence check.
     * @throws PresenceCheckException When an error occurred during presence check start.
     */
    SessionInfo startPresenceCheck(OwnerId id) throws PresenceCheckException;

    /**
     * Gets the result of presence check process.
     *
     * @param id Owner identification.
     * @param sessionInfo Session info with presence check relevant data.
     * @return Result of the presence check
     * @throws PresenceCheckException When an error during getting of the result occurred.
     */
    PresenceCheckResult getResult(OwnerId id, SessionInfo sessionInfo) throws PresenceCheckException;

    /**
     * Cleans up all presence check data related to the identity.
     *
     * @param id Owner identification.
     * @throws PresenceCheckException When an error during cleanup occurred.
     */
    void cleanupIdentityData(OwnerId id) throws PresenceCheckException;

}
