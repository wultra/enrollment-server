/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.model.validator;

import com.wultra.app.enrollmentserver.api.model.enrollment.request.PushRegisterRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * Validator class for push registration request object.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public class PushRegisterRequestValidator {

    /**
     * Validate push registration request object.
     *
     * @param request Push registration request.
     * @return Technical error message in case object is invalid, null in case the request object is valid.
     */
    public static String validate(PushRegisterRequest request) {

        // Validate request is present
        if (request == null) {
            return "No request object provided.";
        }

        // Validate mobile platform
        final String platform = request.getPlatform();
        if (StringUtils.isBlank(platform)) {
            return "No mobile platform was provided when registering for push messages.";
        } else if (!"ios".equalsIgnoreCase(platform) && !"android".equalsIgnoreCase(platform)) { // must be iOS or Android
            return "Unknown mobile platform was provided when registering for push messages.";
        }

        // Validate push token
        final String token = request.getToken();
        if (StringUtils.isBlank(token)) {
            return "No push registration token was provided when registering for push messages.";
        }

        return null;

    }

}
