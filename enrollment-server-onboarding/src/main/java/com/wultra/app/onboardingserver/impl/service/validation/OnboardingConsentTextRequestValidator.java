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
package com.wultra.app.onboardingserver.impl.service.validation;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.OnboardingConsentTextRequest;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ValidationException;

/**
 * OnboardingConsentApprovalRequestValidator for {@link OnboardingConsentTextRequest}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public class OnboardingConsentTextRequestValidator {

    /**
     * Validate the given request.
     *
     * @param request request to validate
     * @throws ValidationException when invalid
     */
    public static void validate(final OnboardingConsentTextRequest request) {
        if (StringUtils.isBlank(request.getConsentType())
                || request.getProcessId() == null) {
            throw new ValidationException("Missing mandatory attributes");
        }
    }
}
