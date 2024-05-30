/*
 * PowerAuth Mobile Token Model
 * Copyright (C) 2017 Wultra s.r.o.
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
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/**
 * Model class representing an operation to be authorized.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
public class Operation {

    @NotNull
    private String id;
    @NotNull
    private String name;
    @NotNull
    private String data;
    private String status;

    /**
     * Optional details why the status has changed by backend services.
     * The value is more about code than free-text detail.
     * Mind that it differs from {@code reason} at the cancel request filled by the user.
     */
    @Schema(description = """
            Optional details why the status has changed by backend services.
            The value is more about code than free-text detail.
            Mind that it differs from `reason` at the cancel request filled by the user.
            """)
    @Size(max = 32)
    private String statusReason;

    private Date operationCreated;
    private Date operationExpires;
    private UiExtensions ui;
    @NotNull
    private AllowedSignatureType allowedSignatureType;
    @NotNull
    private FormData formData = new FormData();

}
