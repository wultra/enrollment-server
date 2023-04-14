/*
 * PowerAuth Mobile Token Model
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
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * Information screen displayed after the operation approval.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GenericPostApprovalScreen.class, name = "GENERIC"),
        @JsonSubTypes.Type(value = MerchantRedirectPostApprovalScreen.class, name = "MERCHANT_REDIRECT"),
        @JsonSubTypes.Type(value = ReviewPostApprovalScreen.class, name = "REVIEW")
})
public abstract class PostApprovalScreen {

    /**
     * Screen heading.
     */
    @NotNull
    private String heading;

    /**
     * Screen message displayed under heading.
     */
    @NotNull
    private String message;

    /**
     * Return screen specific payload.
     *
     * @return payload
     */
    public abstract Payload getPayload();

    public interface Payload {
    }

}
