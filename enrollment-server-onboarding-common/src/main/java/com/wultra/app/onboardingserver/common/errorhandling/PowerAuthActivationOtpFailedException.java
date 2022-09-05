/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.common.errorhandling;

import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthActivationException;

/**
 * Exception thrown in case activation using OTP code fails completely (hard fail).
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class PowerAuthActivationOtpFailedException extends PowerAuthActivationException {

    private static final long serialVersionUID = -7173917873802997723L;

}
