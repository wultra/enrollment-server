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
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Information screen displayed after the operation approval.
 *
 * @author Lubos Racansky, lubos.raansky@wultra.com
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InfoPostApprovalScreen.class, name = "INFO_MESSAGE"),
        @JsonSubTypes.Type(value = MerchantRedirectPostApprovalScreen.class, name = "MERCHANT_REDIRECT")
})
@Data
public abstract class PostApprovalScreen<T extends PostApprovalScreen.Payload> {

    /**
     * Type of the post-approval screen.
     */
    public enum ScreenType {
        /**
         * The purpose of the screen is to inform the user without any possible action.
         */
        INFO_MESSAGE,

        /**
         * The purpose of the screen is to be able to redirect the user to the merchant website or application.
         */
        MERCHANT_REDIRECT
    }

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
     * Specific payload which depends on type or post approval screen.
     */
    private T payload;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MerchantRedirectPostApprovalScreen.MerchantRedirectPayload.class, name = "MERCHANT_REDIRECT")
    })
    @Data
    public static class Payload {
    }
}
