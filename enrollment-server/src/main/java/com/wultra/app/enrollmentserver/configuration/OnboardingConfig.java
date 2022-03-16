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

import java.time.Duration;

/**
 * Identity verification configuration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Configuration
@Data
public class OnboardingConfig {

    @Value("${enrollment-server.identity-verification.otp.length:8}")
    private int otpLength;

    @Value("${enrollment-server.onboarding-process.activation.expiration.seconds:300}")
    private int activationExpirationTime;

    @Value("${enrollment-server.onboarding-process.otp.expiration:PT30S}")
    private Duration otpExpirationTime;

    @Value("${enrollment-server.onboarding-process.otp.max-failed-attempts:5}")
    private int otpMaxFailedAttempts;

    @Value("${enrollment-server.onboarding-process.otp.resend-period:PT30S}")
    private Duration otpResendPeriod;

    @Value("${enrollment-server.onboarding-process.max-processes-per-day:5}")
    private int maxProcessCountPerDay;

}
