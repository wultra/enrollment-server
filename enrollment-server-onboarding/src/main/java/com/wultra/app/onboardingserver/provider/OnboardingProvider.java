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
package com.wultra.app.onboardingserver.provider;

import com.wultra.app.onboardingserver.common.annotation.PublicSpi;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import reactor.core.publisher.Mono;

/**
 * Provider which allows customization of the onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@PublicSpi
public interface OnboardingProvider {

    /**
     * Lookup user.
     *
     * @param request lookup user request
     * @return lookup user response
     * @throws OnboardingProviderException if there is a problem to lookup user
     */
    LookupUserResponse lookupUser(LookupUserRequest request) throws OnboardingProviderException;

    /**
     * Send otp code.
     *
     * @param request send otp code request
     * @throws OnboardingProviderException if there is a problem to send otp code
     */
    void sendOtpCode(SendOtpCodeRequest request) throws OnboardingProviderException;

    /**
     * Provide consent text.
     *
     * @param request consent text request
     * @return consent text
     * @throws OnboardingProviderException if there is a problem to fetch consent text
     */
    String fetchConsent(ConsentTextRequest request) throws OnboardingProviderException;

    /**
     * Record dis/approval of consent.
     *
     * @param request approval request
     * @return approval response
     * @throws OnboardingProviderException if there is a problem to approve consent
     */
    ApproveConsentResponse approveConsent(ApproveConsentRequest request) throws OnboardingProviderException;

    /**
     * Detects whether customer verification matches known records and customer identity is verified.
     *
     * @param request evaluation request
     * @return mono with evaluation response
     */
    Mono<EvaluateClientResponse> evaluateClient(EvaluateClientRequest request);
}
