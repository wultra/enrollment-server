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
package com.wultra.app.enrollmentserver.model.validator;

import com.wultra.app.enrollmentserver.api.model.enrollment.request.ActivationCodeRequest;
import org.apache.commons.lang3.StringUtils;

/**
 * Validator for activation code request.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public class ActivationCodeRequestValidator {

    private static final int MIN_OTP_LEN = 16;

    /**
     * Validate activation code request object.
     *
     * @param request Activation code request.
     * @return Technical error message in case object is invalid, null in case the request object is valid.
     */
    public static String validate(ActivationCodeRequest request) {

        // Validate request is present
        if (request == null) {
            return "No request object provided.";
        }

        // Validate application value
        final String applicationId = request.getApplicationId();
        if (StringUtils.isBlank(applicationId)) { // application ID must be present
            return "No application ID was provided.";
        }

        // Validate OTP value
        final String otp = request.getOtp();
        if (StringUtils.isBlank(otp)) { // OTP must be present
            return "No OTP was provided.";
        } else if (otp.length() < MIN_OTP_LEN) { // OTP must be sufficiently long
            return "OTP is too short, minimum of " + MIN_OTP_LEN + " characters is required for the activation code fetch use-case.";
        }

        return null;
    }
}
