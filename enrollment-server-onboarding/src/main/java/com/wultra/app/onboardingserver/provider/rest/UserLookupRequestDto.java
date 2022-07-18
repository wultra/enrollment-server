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

import java.util.Map;
import java.util.Objects;

/**
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@JsonTypeName("UserLookupRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class UserLookupRequestDto {

    private String processId;
    private Map<String, Object> identification;

    /**
     * Represents unique identifier of one specific mobile token reactivation flow
     **/
    public UserLookupRequestDto processId(String processId) {
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
     *
     **/
    public UserLookupRequestDto identification(Map<String, Object> identification) {
        this.identification = identification;
        return this;
    }


    @JsonProperty("identification")
    public Map<String, Object> getIdentification() {
        return identification;
    }

    @JsonProperty("identification")
    public void setIdentification(Map<String, Object> identification) {
        this.identification = identification;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserLookupRequestDto userLookupRequest = (UserLookupRequestDto) o;
        return Objects.equals(this.processId, userLookupRequest.processId) &&
                Objects.equals(this.identification, userLookupRequest.identification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, identification);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UserLookupRequestDto {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    identification: ").append(toIndentedString(identification)).append("\n");
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

