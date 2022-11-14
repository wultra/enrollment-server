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

package com.wultra.app.enrollmentserver.api.model.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Model class for inbox message count request.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetInboxCountRequest {

    @NotNull
    @Size(min = 1, max = 255)
    @Schema(type = "string", example = "User identifier")
    private String userId;

    @NotNull
    @Size(min = 1, max = 255)
    @Schema(type = "string", example = "Application identifier")
    @ToString.Exclude
    private String appId;

}
