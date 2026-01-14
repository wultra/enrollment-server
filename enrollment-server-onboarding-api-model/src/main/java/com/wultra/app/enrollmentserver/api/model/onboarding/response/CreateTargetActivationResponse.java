/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Response class used when creating a target activation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Builder
@Jacksonized
public record CreateTargetActivationResponse(

        @Schema(description = """
            Activation code for the target activation.
            Uses 4x5 characters in Base32 encoding separated by a `-` character.""", example = "KA4PD-RTIE2-KOP3U-H53EA", minLength = 23, maxLength = 23)
        @NotBlank
        String activationCode) {

    @Override
    public String toString() {
        return "CreateTargetActivationResponse{" +
                "activationCode='***'" +
                '}';
    }
}
