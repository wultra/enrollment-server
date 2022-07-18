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
@JsonTypeName("_user_lookup_post_400_response")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class UserLookupPost400ResponseDto {

    private String message;
    private String errorUUID;
    private String correlationId;

    public enum ErrorCodeEnum {

        INVALID_REQUEST(String.valueOf("INVALID_REQUEST")), USER_NOT_FOUND(String.valueOf("USER_NOT_FOUND")), IDENTIFICATION_FAILED(String.valueOf("IDENTIFICATION_FAILED")), ONBOARDING_NOT_ALLOWED(String.valueOf("ONBOARDING_NOT_ALLOWED"));


        private String value;

        ErrorCodeEnum(String v) {
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
        public static ErrorCodeEnum fromString(String s) {
            for (ErrorCodeEnum b : ErrorCodeEnum.values()) {
                // using Objects.toString() to be safe if value type non-object type
                // because types like 'int' etc. will be auto-boxed
                if (Objects.toString(b.value).equals(s)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected string value '" + s + "'");
        }

        @JsonCreator
        public static ErrorCodeEnum fromValue(String value) {
            for (ErrorCodeEnum b : ErrorCodeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private ErrorCodeEnum errorCode;

    /**
     * Message describing detail of error
     **/
    public UserLookupPost400ResponseDto message(String message) {
        this.message = message;
        return this;
    }


    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Unique identifier of error
     **/
    public UserLookupPost400ResponseDto errorUUID(String errorUUID) {
        this.errorUUID = errorUUID;
        return this;
    }


    @JsonProperty("errorUUID")
    public String getErrorUUID() {
        return errorUUID;
    }

    @JsonProperty("errorUUID")
    public void setErrorUUID(String errorUUID) {
        this.errorUUID = errorUUID;
    }

    /**
     * ID of error/call in related system, whose call failed
     **/
    public UserLookupPost400ResponseDto correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }


    @JsonProperty("correlationId")
    public String getCorrelationId() {
        return correlationId;
    }

    @JsonProperty("correlationId")
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Type of error
     **/
    public UserLookupPost400ResponseDto errorCode(ErrorCodeEnum errorCode) {
        this.errorCode = errorCode;
        return this;
    }


    @JsonProperty("errorCode")
    public ErrorCodeEnum getErrorCode() {
        return errorCode;
    }

    @JsonProperty("errorCode")
    public void setErrorCode(ErrorCodeEnum errorCode) {
        this.errorCode = errorCode;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserLookupPost400ResponseDto userLookupPost400Response = (UserLookupPost400ResponseDto) o;
        return Objects.equals(this.message, userLookupPost400Response.message) &&
                Objects.equals(this.errorUUID, userLookupPost400Response.errorUUID) &&
                Objects.equals(this.correlationId, userLookupPost400Response.correlationId) &&
                Objects.equals(this.errorCode, userLookupPost400Response.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, errorUUID, correlationId, errorCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UserLookupPost400ResponseDto {\n");

        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    errorUUID: ").append(toIndentedString(errorUUID)).append("\n");
        sb.append("    correlationId: ").append(toIndentedString(correlationId)).append("\n");
        sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
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

