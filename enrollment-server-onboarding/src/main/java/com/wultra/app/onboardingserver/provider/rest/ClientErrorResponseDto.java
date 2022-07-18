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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@JsonTypeName("ClientErrorResponse")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ClientErrorResponseDto {

    private String message;
    private String errorUUID;
    private String correlationId;

    /**
     * Message describing detail of error
     **/
    public ClientErrorResponseDto message(String message) {
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
    public ClientErrorResponseDto errorUUID(String errorUUID) {
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
    public ClientErrorResponseDto correlationId(String correlationId) {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientErrorResponseDto clientErrorResponse = (ClientErrorResponseDto) o;
        return Objects.equals(this.message, clientErrorResponse.message) &&
                Objects.equals(this.errorUUID, clientErrorResponse.errorUUID) &&
                Objects.equals(this.correlationId, clientErrorResponse.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, errorUUID, correlationId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClientErrorResponseDto {\n");

        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    errorUUID: ").append(toIndentedString(errorUUID)).append("\n");
        sb.append("    correlationId: ").append(toIndentedString(correlationId)).append("\n");
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

