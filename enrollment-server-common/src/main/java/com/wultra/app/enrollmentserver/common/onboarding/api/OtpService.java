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
package com.wultra.app.enrollmentserver.common.onboarding.api;

import com.wultra.app.enrollmentserver.api.model.response.OtpVerifyResponse;
import com.wultra.app.enrollmentserver.common.annotation.PublicApi;
import com.wultra.app.enrollmentserver.common.onboarding.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;

/**
 * Service implementing OTP verification during onboarding process.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@PublicApi
public interface OtpService {

    /**
     * Verify an OTP code.
     *
     * @param processId Process identifier.
     * @param otpCode OTP code sent by the user.
     * @param otpType OTP type.
     * @return Verify OTP code response.
     * @throws OnboardingProcessException Thrown when process or OTP code is not found.
     */
    OtpVerifyResponse verifyOtpCode(String processId, String otpCode, OtpType otpType) throws OnboardingProcessException;
}
