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
package com.wultra.app.onboardingserver.provider.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@JsonTypeName("OtpSendRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class OtpSendRequestDto {

    private String processId;
    private String userId;
    private String language;
    private String otpCode;

    public enum OtpTypeEnum {

        ACTIVATION(String.valueOf("ACTIVATION")), USER_VERIFICATION(String.valueOf("USER_VERIFICATION"));


        private String value;

        OtpTypeEnum(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        /**
         * Convert a String into String, as specified in the
         * <a href="https://download.oracle.com/otndocs/jcp/jaxrs-2_0-fr-eval-spec/index.html">See JAX RS 2.0 Specification, section 3.2, p. 12</a>
         */
        public static OtpTypeEnum fromString(String s) {
            for (OtpTypeEnum b : OtpTypeEnum.values()) {
                // using Objects.toString() to be safe if value type non-object type
                // because types like 'int' etc. will be auto-boxed
                if (Objects.toString(b.value).equals(s)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected string value '" + s + "'");
        }

        @JsonCreator
        public static OtpTypeEnum fromValue(String value) {
            for (OtpTypeEnum b : OtpTypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private OtpTypeEnum otpType;
    private Boolean resend;

    /**
     * Represents unique identifier of one specific mobile token reactivation flow
     **/
    public OtpSendRequestDto processId(String processId) {
        this.processId = processId;
        return this;
    }


    @JsonProperty("processId")
    public String getProcessId() {
        return processId;
    }

    @JsonProperty("processId")
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * Represents user identifier
     **/
    public OtpSendRequestDto userId(String userId) {
        this.userId = userId;
        return this;
    }


    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     *
     **/
    public OtpSendRequestDto language(String language) {
        this.language = language;
        return this;
    }


    @JsonProperty("language")
    public String getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * One time password send to users mobile for verificartion
     **/
    public OtpSendRequestDto otpCode(String otpCode) {
        this.otpCode = otpCode;
        return this;
    }


    @JsonProperty("otpCode")
    public String getOtpCode() {
        return otpCode;
    }

    @JsonProperty("otpCode")
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    /**
     * OTP type specifying which text template will be used
     **/
    public OtpSendRequestDto otpType(OtpTypeEnum otpType) {
        this.otpType = otpType;
        return this;
    }


    @JsonProperty("otpType")
    public OtpTypeEnum getOtpType() {
        return otpType;
    }

    @JsonProperty("otpType")
    public void setOtpType(OtpTypeEnum otpType) {
        this.otpType = otpType;
    }

    /**
     * Flag if this call represent first try or some repeated try
     **/
    public OtpSendRequestDto resend(Boolean resend) {
        this.resend = resend;
        return this;
    }


    @JsonProperty("resend")
    public Boolean getResend() {
        return resend;
    }

    @JsonProperty("resend")
    public void setResend(Boolean resend) {
        this.resend = resend;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OtpSendRequestDto otpSendRequest = (OtpSendRequestDto) o;
        return Objects.equals(this.processId, otpSendRequest.processId) &&
                Objects.equals(this.userId, otpSendRequest.userId) &&
                Objects.equals(this.language, otpSendRequest.language) &&
                Objects.equals(this.otpCode, otpSendRequest.otpCode) &&
                Objects.equals(this.otpType, otpSendRequest.otpType) &&
                Objects.equals(this.resend, otpSendRequest.resend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, userId, language, otpCode, otpType, resend);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OtpSendRequestDto {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
        sb.append("    language: ").append(toIndentedString(language)).append("\n");
        sb.append("    otpCode: ").append(toIndentedString(otpCode)).append("\n");
        sb.append("    otpType: ").append(toIndentedString(otpType)).append("\n");
        sb.append("    resend: ").append(toIndentedString(resend)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }


}

