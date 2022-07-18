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
@JsonTypeName("ClientEvaluateResponse")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ClientEvaluateResponseDto {


    public enum ResultEnum {

        OK(String.valueOf("OK")), NOK(String.valueOf("NOK"));


        private String value;

        ResultEnum(String v) {
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
        public static ResultEnum fromString(String s) {
            for (ResultEnum b : ResultEnum.values()) {
                // using Objects.toString() to be safe if value type non-object type
                // because types like 'int' etc. will be auto-boxed
                if (Objects.toString(b.value).equals(s)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected string value '" + s + "'");
        }

        @JsonCreator
        public static ResultEnum fromValue(String value) {
            for (ResultEnum b : ResultEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private ResultEnum result;
    private ClientEvaluateResponseInfoDto info;

    /**
     * Overall result of super aggregation function
     **/
    public ClientEvaluateResponseDto result(ResultEnum result) {
        this.result = result;
        return this;
    }


    @JsonProperty("result")
    public ResultEnum getResult() {
        return result;
    }

    @JsonProperty("result")
    public void setResult(ResultEnum result) {
        this.result = result;
    }

    /**
     *
     **/
    public ClientEvaluateResponseDto info(ClientEvaluateResponseInfoDto info) {
        this.info = info;
        return this;
    }


    @JsonProperty("info")
    public ClientEvaluateResponseInfoDto getInfo() {
        return info;
    }

    @JsonProperty("info")
    public void setInfo(ClientEvaluateResponseInfoDto info) {
        this.info = info;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientEvaluateResponseDto clientEvaluateResponse = (ClientEvaluateResponseDto) o;
        return Objects.equals(this.result, clientEvaluateResponse.result) &&
                Objects.equals(this.info, clientEvaluateResponse.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, info);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClientEvaluateResponseDto {\n");

        sb.append("    result: ").append(toIndentedString(result)).append("\n");
        sb.append("    info: ").append(toIndentedString(info)).append("\n");
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

