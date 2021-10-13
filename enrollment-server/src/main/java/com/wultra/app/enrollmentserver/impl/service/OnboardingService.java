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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.model.request.*;
import com.wultra.app.enrollmentserver.model.response.OnboardingStartResponse;
import com.wultra.app.enrollmentserver.model.response.VerifyOtpResponse;
import io.getlime.core.rest.model.base.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Service implementing the onboarding process
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);

    /**
     * Start an onboarding process.
     * @param request Onboarding start request.
     * @return Onboarding start response.
     */
    @Transactional
    public OnboardingStartResponse startOnboarding(OnboardingStartRequest request) {
        return new OnboardingStartResponse();
    }

    /**
     * Resend an OTP code.
     * @param request Resend OTP code request.
     * @return Resend OTP code response.
     */
    @Transactional
    public Response resendOtp(ResendOtpRequest request) {
        return new Response();
    }

    /**
     * Verify an OTP code.
     * @param request Verify OTP code request.
     * @return Verify OTP code response.
     */
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        return new VerifyOtpResponse();
    }

    /**
     * Get onboarding process status.
     * @param request Onboarding status request.
     * @return Onboarding status response.
     */
    @Transactional
    public OnboardingStartResponse getStatus(OnboardingStatusRequest request) {
        return new OnboardingStartResponse();
    }

    /**
     * Perform cleanup of an onboarding process.
     * @param request Onboarding process cleanup request.
     * @return Onboarding process cleanup response.
     */
    @Transactional
    public Response performCleanup(OnboardingCleanupRequest request) {
        return new Response();
    }

}