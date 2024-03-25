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
package com.wultra.app.onboardingserver.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Identity verification configuration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Configuration
@Data
public class IdentityVerificationConfig {

    @Value("${enrollment-server-onboarding.document-verification.provider:mock}")
    private String documentVerificationProvider;

    @Value("${enrollment-server-onboarding.document-verification.cleanupEnabled:false}")
    private boolean documentVerificationCleanupEnabled;

    @Value("${enrollment-server-onboarding.presence-check.enabled:true}")
    private boolean presenceCheckEnabled;

    @Value("${enrollment-server-onboarding.presence-check.verifySelfieWithDocumentsEnabled:false}")
    private boolean verifySelfieWithDocumentsEnabled;

    @Value("${enrollment-server-onboarding.presence-check.provider:mock}")
    private String presenceCheckProvider;

    @Value("${enrollment-server-onboarding.presence-check.cleanupEnabled:false}")
    private boolean presenceCheckCleanupEnabled;

    @Value("${enrollment-server-onboarding.identity-verification.data-retention:1h}")
    private Duration dataRetentionTime;

    @Value("${enrollment-server-onboarding.onboarding-process.verification.expiration:1h}")
    private Duration verificationExpirationTime;

    @Value("${enrollment-server-onboarding.identity-verification.otp.enabled:true}")
    private boolean verificationOtpEnabled;

    @Value("${enrollment-server-onboarding.identity-verification.max-failed-attempts:5}")
    private int verificationMaxFailedAttempts;

    @Value("${enrollment-server-onboarding.identity-verification.max-failed-attempts-document-upload:5}")
    private int documentUploadMaxFailedAttempts;

    @Value("${enrollment-server-onboarding.presence-check.max-failed-attempts:5}")
    private int presenceCheckMaxFailedAttempts;

    /**
     * Minimal width of selfie image (pixels) used to crosscheck presence.
     */
    @Value("${enrollment-server-onboarding.presence-check.selfie.minimal-width:400}")
    private int minimalSelfieWidth;

    @Value("${enrollment-server-onboarding.client-evaluation.max-failed-attempts:5}")
    private int clientEvaluationMaxFailedAttempts;

    @Value("${enrollment-server-onboarding.client-evaluation.include-extracted-data:false}")
    private boolean sendingExtractedDataEnabled;

    @PostConstruct
    void validate() {
        // Once in the future, we may replace OTP in SCA by NFC document reading
        Assert.state(presenceCheckEnabled == verificationOtpEnabled, "Presence check and OTP verification have to be both disabled or both enabled");
    }
}