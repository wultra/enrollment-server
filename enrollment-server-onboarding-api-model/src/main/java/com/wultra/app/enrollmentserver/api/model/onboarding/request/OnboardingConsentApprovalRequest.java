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
package com.wultra.app.enrollmentserver.api.model.onboarding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request class used when asking for consent for onboarding.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
public class OnboardingConsentApprovalRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String processId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "GDPR")
    private String consentType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean approved;
}
