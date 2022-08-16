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
package com.wultra.app.onboardingserver.provider;

import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link OnboardingProvider} throwing an exception for each method.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class EmptyOnboardingProvider implements OnboardingProvider {

    @Override
    public LookupUserResponse lookupUser(LookupUserRequest request) throws OnboardingProviderException {
        throw createException();
    }

    @Override
    public void sendOtpCode(SendOtpCodeRequest request) throws OnboardingProviderException {
        throw createException();
    }

    @Override
    public String fetchConsent(ConsentTextRequest request) throws OnboardingProviderException {
        throw createException();
    }

    @Override
    public ApproveConsentResponse approveConsent(ApproveConsentRequest request) throws OnboardingProviderException {
        throw createException();
    }

    @Override
    public Mono<EvaluateClientResponse> evaluateClient(EvaluateClientRequest request) {
        return Mono.error(createException());
    }

    private static OnboardingProviderException createException() {
        return new OnboardingProviderException("OnboardingProvider is not available. " +
                "Implement an onboarding provider and make it accessible using autowiring.");
    }
}
