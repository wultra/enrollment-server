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
package com.wultra.app.enrollmentserver.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Identity verification configuration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Configuration
@Data
public class OnboardingConfig {

    @Value("${enrollment-server.document-verification.provider:mock}")
    private String documentVerificationProvider;

    @Value("${enrollment-server.identity-verification.otp.length:8}")
    private int otpLength;

    @Value("${enrollment-server.onboarding-process.expiration.seconds:300}")
    private int processExpirationTime;

    @Value("${enrollment-server.onboarding-process.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${enrollment-server.onboarding-process.resend-period.seconds:30}")
    private int resendPeriod;

    @Value("${enrollment-server.onboarding-process.max-processes-per-day:5}")
    private int maxProcessCountPerDay;

    @Value("${enrollment-server.presence-check.provider:mock}")
    private String presenceCheckProvider;

}
