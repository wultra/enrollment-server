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

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Specialization of {@link PostApprovalScreen} for redirecting the user to the merchant website or application.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public class MerchantRedirectPostApprovalScreen extends PostApprovalScreen {

    @NotNull
    private MerchantRedirectPayload payload;

    @Override
    public MerchantRedirectPayload getPayload() {
        return payload;
    }

    public void setPayload(MerchantRedirectPayload payload) {
        this.payload = payload;
    }

    /**
     * Specialization of {@link Payload} for redirecting the user to the merchant website or application.
     */
    @Data
    public static class MerchantRedirectPayload implements Payload {

        @NotNull
        private String redirectText;

        /**
         * Url to website or application.
         */
        @NotNull
        private String redirectUrl;

        /**
         * Time in seconds when the user will be redirected automatically.
         */
        private int countdown;
    }
}
