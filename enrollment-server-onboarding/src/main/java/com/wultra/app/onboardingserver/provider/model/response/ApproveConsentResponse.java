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
 *
 */
package com.wultra.app.onboardingserver.provider.model.response;

import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.ApproveConsentRequest;
import com.wultra.core.annotations.PublicApi;
import lombok.Getter;
import lombok.ToString;

/**
 * Response object for {@link OnboardingProvider#approveConsent(ApproveConsentRequest)}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@PublicApi
@ToString
@Getter
public final class ApproveConsentResponse {

    /**
     * Whether business logic error occurred during consent approval.
     */
    private boolean errorOccurred;

    /**
     * Error detail to store within onboarding process.
     */
    private String errorDetail;
}
