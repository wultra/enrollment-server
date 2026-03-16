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
package com.wultra.app.enrollmentserver.api.model.onboarding.response;

import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.ConfigurationDataDto;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Response class used when checking identity verification status.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
public class IdentityVerificationStatusResponse {

    private ConfigurationDataDto config;

    @Schema(description = "Process ID")
    @NotBlank
    private String processId;

    @NotNull
    private IdentityVerificationStatus identityVerificationStatus;

    private IdentityVerificationPhase identityVerificationPhase;

    @Schema(description = "Process type identifier.", example = "reactivation")
    @NotBlank
    private String processType;

    @Schema(description = "Reject reason. Apply for phases such as client evaluation, presence check, or client approval")
    private String rejectReason;

    @Schema(description = "Specifies whether consent is required and pending.")
    private boolean consentRequired;
}
