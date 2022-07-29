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
@JsonTypeName("ConsentTextRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ConsentTextRequestDto {

    private String processId;
    private String userId;

    /**
     * Language in ISO 3166-1 alpha-2 format lower cased.
     */
    private String language;
    private String consentType;

    /**
     * Represents unique identifier of one specific mobile token reactivation flow
     **/
    public ConsentTextRequestDto processId(String processId) {
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
    public ConsentTextRequestDto userId(String userId) {
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
    public ConsentTextRequestDto language(String language) {
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
     *
     **/
    public ConsentTextRequestDto consentType(String consentType) {
        this.consentType = consentType;
        return this;
    }


    @JsonProperty("consentType")
    public String getConsentType() {
        return consentType;
    }

    @JsonProperty("consentType")
    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsentTextRequestDto consentTextRequest = (ConsentTextRequestDto) o;
        return Objects.equals(this.processId, consentTextRequest.processId) &&
                Objects.equals(this.userId, consentTextRequest.userId) &&
                Objects.equals(this.language, consentTextRequest.language) &&
                Objects.equals(this.consentType, consentTextRequest.consentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, userId, language, consentType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ConsentTextRequestDto {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
        sb.append("    language: ").append(toIndentedString(language)).append("\n");
        sb.append("    consentType: ").append(toIndentedString(consentType)).append("\n");
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

