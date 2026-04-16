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
package com.wultra.app.enrollmentserver.api.model.enrollment.response;
 
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
 
/**
 * Response with a new activation code.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
@Schema(description = "Response with a new activation code.")
public class ActivationCodeResponse {
 
    @Schema(description = "Activation ID of the new activation.", example = "9e0ba60f-bf22-4ff5-b999-2733784e5eaa")
    private String activationId;
 
    @Schema(description = "New activation code.", example = "ABCDE-FGHIJ")
    private String activationCode;
 
    @Schema(description = "Activation signature used for verification of the activation code.", example = "base64-encoded-signature")
    private String activationSignature;
 
}
