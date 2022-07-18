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
@JsonTypeName("ConsentStorageRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ConsentStorageRequestDto {

    private String userId;
    private ConsentTypePropertyDto consentType;
    private String processId;
    private Boolean approved;

    /**
     * Represents user identifier
     **/
    public ConsentStorageRequestDto userId(String userId) {
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
    public ConsentStorageRequestDto consentType(ConsentTypePropertyDto consentType) {
        this.consentType = consentType;
        return this;
    }


    @JsonProperty("consentType")
    public ConsentTypePropertyDto getConsentType() {
        return consentType;
    }

    @JsonProperty("consentType")
    public void setConsentType(ConsentTypePropertyDto consentType) {
        this.consentType = consentType;
    }

    /**
     * Represents unique identifier of one specific mobile token reactivation flow
     **/
    public ConsentStorageRequestDto processId(String processId) {
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
     * If consent was approved or declined
     **/
    public ConsentStorageRequestDto approved(Boolean approved) {
        this.approved = approved;
        return this;
    }


    @JsonProperty("approved")
    public Boolean getApproved() {
        return approved;
    }

    @JsonProperty("approved")
    public void setApproved(Boolean approved) {
        this.approved = approved;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsentStorageRequestDto consentStorageRequest = (ConsentStorageRequestDto) o;
        return Objects.equals(this.userId, consentStorageRequest.userId) &&
                Objects.equals(this.consentType, consentStorageRequest.consentType) &&
                Objects.equals(this.processId, consentStorageRequest.processId) &&
                Objects.equals(this.approved, consentStorageRequest.approved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, consentType, processId, approved);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ConsentStorageRequestDto {\n");

        sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
        sb.append("    consentType: ").append(toIndentedString(consentType)).append("\n");
        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    approved: ").append(toIndentedString(approved)).append("\n");
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

