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
package com.wultra.app.enrollmentserver.provider;

import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
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

    SessionInfo initPresenceCheck(OwnerId id) throws PresenceCheckException;

    void startPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException;

    PresenceCheckResult getResult(OwnerId id) throws PresenceCheckException;

    void cleanupIdentityData(OwnerId id) throws PresenceCheckException;

}