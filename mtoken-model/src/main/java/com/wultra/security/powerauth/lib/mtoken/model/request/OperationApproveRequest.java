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
package com.wultra.security.powerauth.lib.mtoken.model.request;

import com.wultra.security.powerauth.lib.mtoken.model.entity.PreApprovalScreen;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Request for online token signature verification.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Data
public class OperationApproveRequest {

    @NotEmpty
    private String id;

    /**
     * Operation data to approve.
     */
    @Schema(description = "Operation data to approve.", example = "A1*A100CZK*Q238400856\\/0300**D20190629*NUtility Bill Payment - 05\\/2019")
    @NotNull
    private String data;

    /**
     * Optional proximity check data. User is instructed by {@link PreApprovalScreen.ScreenType#QR_SCAN}.
     */
    @Schema(description = "Optional proximity check data." )
    private ProximityCheck proximityCheck;

    /**
     * Optional additional data, structure is customer-specific. Could be used, for example, for passing FDS data.
     */
    @Schema(description = "Optional additional data, structure is customer-specific. Could be used, for example, for storing FDS data.")
    private Map<String, Object> additionalData;

    public Optional<ProximityCheck> getProximityCheck() {
        return Optional.ofNullable(proximityCheck);
    }

    @Data
    public static class ProximityCheck {

        @NotNull
        @Schema(description = "OTP used for proximity check.")
        private String otp;

        @Schema(description = "Source from where the OTP has been gained.")
        private Type type;

        /**
         * When OTP received by the client. An optional hint for possible better estimation of the time shift correction.
         */
        @Schema(description = "When OTP received by the client. An optional hint for possible better estimation of the time shift correction.")
        private Instant timestampReceived;

        /**
         * When OTP is used by the client as part of a signed message. An optional hint for possible better estimation of the time shift correction.
         */
        @Schema(description = "When OTP is used by the client as part of a signed message. An optional hint for possible better estimation of the time shift correction.")
        private Instant timestampSent;

        public enum Type {
            QR_CODE,
            DEEPLINK
        }
    }
}
