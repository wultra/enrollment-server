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
package com.wultra.app.onboardingserver.common.activation;

import com.wultra.app.enrollmentserver.api.model.onboarding.response.OtpVerifyResponse;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.api.OtpService;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;

/**
 * Service used for verifying OTP codes during activation.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public class ActivationOtpService {

    private final OtpService otpService;

    /**
     * Service constructors.
     * @param otpService OTP service.
     */
    public ActivationOtpService(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Verify an OTP code during activation.
     * @param processId Onboarding process identifier.
     * @param ownerId Owner identification.
     * @param otpCode OTP code.
     * @throws OnboardingProcessException Thrown when onboarding process or OTP code is not found.
     * @return OTP verification response.
     */
    public OtpVerifyResponse verifyOtpCode(String processId, OwnerId ownerId, String otpCode) throws OnboardingProcessException {
        return otpService.verifyOtpActivationCode(processId, ownerId, otpCode);
    }

}