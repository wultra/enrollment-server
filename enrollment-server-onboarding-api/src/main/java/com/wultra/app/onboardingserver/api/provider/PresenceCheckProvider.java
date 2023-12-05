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
package com.wultra.app.onboardingserver.api.provider;

import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.api.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.core.annotations.PublicSpi;

/**
 * Provider which allows customization of the presence check.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@PublicSpi
public interface PresenceCheckProvider {

    /**
     * Initializes presence check process.
     *
     * @param id Owner identification.
     * @param photo Trusted photo of the user.
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    void initPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException, RemoteCommunicationException;

    /**
     * A feature flag whether the trusted photo of the user should be passed to {@link #initPresenceCheck(OwnerId, Image)}.
     * <p>
     * Some implementation may require specific source to be called by Onboarding server, some providers may handle it internally.
     *
     * @return {@code true} if the trusted photo should be provided, {@code false} otherwise.
     */
    boolean shouldProvideTrustedPhoto();

    /**
     * Starts the presence check process. The process has to be initialized before this call.
     *
     * @param id Owner identification.
     * @return Session info with data related to the presence check.
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    SessionInfo startPresenceCheck(OwnerId id) throws PresenceCheckException, RemoteCommunicationException;

    /**
     * Gets the result of presence check process.
     *
     * @param id Owner identification.
     * @param sessionInfo Session info with presence check relevant data.
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    PresenceCheckResult getResult(OwnerId id, SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException;

    /**
     * Cleans up all presence check data related to the identity.
     *
     * @param id Owner identification.
     * @param sessionInfo Session info with presence check relevant data.
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    void cleanupIdentityData(OwnerId id, SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException;

}
