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
package com.wultra.app.onboardingserver.impl.service.internal;

import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Service class used for generating OTP codes.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OtpGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(OtpGeneratorService.class);

    private static final int OTP_MIN_LENGTH = 4;
    private static final int OTP_MAX_LENGTH = 12;

    /**
     * Generate an OTP code.
     * @param length Length of generated OTP code.
     * @return OTP code.
     * @throws OnboardingProcessException Thrown in case OTP code generation fails.
     */
    public String generateOtpCode(int length) throws OnboardingProcessException {
        if (length < OTP_MIN_LENGTH || length > OTP_MAX_LENGTH) {
            throw new OnboardingProcessException("Invalid OTP length: " + length);
        }
        SecureRandom random = new SecureRandom();
        BigInteger bound = BigInteger.TEN.pow(length).subtract(BigInteger.ONE);
        long number = Math.abs(random.nextLong() % bound.longValue());
        return String.format("%0" + length + "d", number);
    }

}
