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
import com.wultra.app.enrollmentserver.model.enumeration.ActivationType;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Response class used when starting the onboarding process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Builder
@Jacksonized
public record OnboardingStartResponse(

        @Schema(description = "Process ID of the onboarding process.", format = "uuid", example = "edebea8d-5eb4-4b92-b366-2a12a0dafe53")
        @NotBlank
        String processId,

        @Schema(description = "Current status of the onboarding process.")
        @NotNull
        OnboardingStatus onboardingStatus,

        @Schema(description = "Configuration data for the onboarding process.")
        @NotNull
        ConfigurationDataDto config,

        @Schema(description = """
            Activation code used during the activation process.
            For `activationType=IDENTITY`, `activationCode` is not present; the activation is created later on in the onboarding process.
            Uses 4x5 characters in Base32 encoding separated by a `-` character.""", example = "KA4PD-RTIE2-KOP3U-H53EA", minLength = 23, maxLength = 23)
        String activationCode,

        @Schema(description = """
                Activation type. When `CODE`, `activationCode` has to be present.
                `ACTIVATION_ALREADY_EXISTS` indicates that the process uses the active activation that signed the start request.""")
        @NotNull
        ActivationType activationType) {

    @AssertTrue(message = "activationCode must be present when activationType is CODE")
    private boolean isActivationCodeValid() {
        return activationType != ActivationType.CODE || activationCode != null && !activationCode.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "OnboardingStartResponse{" +
                "processId='" + processId + '\'' +
                ", onboardingStatus=" + onboardingStatus +
                ", config=" + config +
                ", activationType=" + activationType +
                '}';
    }
}
