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
@JsonTypeName("ClientEvaluateRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ClientEvaluateRequestDto {

    private String processId;
    private String identityVerificationId;
    private String userId;
    private String verificationId;
    private String provider;

    /**
     * Represents unique identifier of one specific mobile token reactivation flow
     **/
    public ClientEvaluateRequestDto processId(String processId) {
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
     * Represents unique identifier of one specific sub process inside main process of reactivation
     **/
    public ClientEvaluateRequestDto identityVerificationId(String identityVerificationId) {
        this.identityVerificationId = identityVerificationId;
        return this;
    }


    @JsonProperty("identityVerificationId")
    public String getIdentityVerificationId() {
        return identityVerificationId;
    }

    @JsonProperty("identityVerificationId")
    public void setIdentityVerificationId(String identityVerificationId) {
        this.identityVerificationId = identityVerificationId;
    }

    /**
     * Represents user identifier
     **/
    public ClientEvaluateRequestDto userId(String userId) {
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
     * Identifier of verification in indentity verification system under which the uploaded documents were verified.
     **/
    public ClientEvaluateRequestDto investigationId(String verificationId) {
        this.verificationId = verificationId;
        return this;
    }


    @JsonProperty("verificationId")
    public String getVerificationId() {
        return verificationId;
    }

    @JsonProperty("verificationId")
    public void setVerificationId(String verificationId) {
        this.verificationId = verificationId;
    }

    /**
     * Provider used by Wultra components, not used by adapter
     **/
    public ClientEvaluateRequestDto provider(String provider) {
        this.provider = provider;
        return this;
    }


    @JsonProperty("provider")
    public String getProvider() {
        return provider;
    }

    @JsonProperty("provider")
    public void setProvider(String provider) {
        this.provider = provider;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientEvaluateRequestDto clientEvaluateRequest = (ClientEvaluateRequestDto) o;
        return Objects.equals(this.processId, clientEvaluateRequest.processId) &&
                Objects.equals(this.identityVerificationId, clientEvaluateRequest.identityVerificationId) &&
                Objects.equals(this.userId, clientEvaluateRequest.userId) &&
                Objects.equals(this.verificationId, clientEvaluateRequest.verificationId) &&
                Objects.equals(this.provider, clientEvaluateRequest.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, identityVerificationId, userId, verificationId, provider);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClientEvaluateRequestDto {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    identityVerificationId: ").append(toIndentedString(identityVerificationId)).append("\n");
        sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
        sb.append("    verificationId: ").append(toIndentedString(verificationId)).append("\n");
        sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
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

