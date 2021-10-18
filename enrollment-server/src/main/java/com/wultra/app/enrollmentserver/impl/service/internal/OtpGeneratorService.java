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
package com.wultra.app.enrollmentserver.impl.service.internal;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Service class used for generating OTP codes.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OtpGeneratorService {

    /**
     * Generate an OTP code.
     * @return OTP code.
     */
    public String generateOtpCode() {
        // TODO - configuration of number of digits
        SecureRandom random = new SecureRandom();
        int number = random.nextInt(99999999);
        return String.format("%08d", number);
    }

}
