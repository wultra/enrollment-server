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
package com.wultra.app.enrollmentserver.api.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

/**
 * Request class used when asking for consent for onboarding.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
public class OnboardingConsentApprovalRequest {

    @Schema(required = true)
    private UUID processId;

    @Schema(required = true, example = "abc123456")
    private String userId;

    @Schema(required = true, example = "GDPR")
    private String consentType;

    @Schema(required = true)
    private Boolean approved;
}
