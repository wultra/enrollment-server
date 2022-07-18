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
@JsonTypeName("ClientEvaluateResponse_info")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ClientEvaluateResponseInfoDto {

    private Boolean aggregationResult;
    private Boolean mdcCheck;
    private Boolean documentsAccepted;

    /**
     * Flag whether aggregation function over indentity verification system validators was successfull or not
     **/
    public ClientEvaluateResponseInfoDto aggregationResult(Boolean aggregationResult) {
        this.aggregationResult = aggregationResult;
        return this;
    }


    @JsonProperty("aggregationResult")
    public Boolean getAggregationResult() {
        return aggregationResult;
    }

    @JsonProperty("aggregationResult")
    public void setAggregationResult(Boolean aggregationResult) {
        this.aggregationResult = aggregationResult;
    }

    /**
     * Whether data check in MDC against Siebel data was successfull or not
     **/
    public ClientEvaluateResponseInfoDto mdcCheck(Boolean mdcCheck) {
        this.mdcCheck = mdcCheck;
        return this;
    }


    @JsonProperty("mdcCheck")
    public Boolean getMdcCheck() {
        return mdcCheck;
    }

    @JsonProperty("mdcCheck")
    public void setMdcCheck(Boolean mdcCheck) {
        this.mdcCheck = mdcCheck;
    }

    /**
     * Whether documents were accepted by IBL or not
     **/
    public ClientEvaluateResponseInfoDto documentsAccepted(Boolean documentsAccepted) {
        this.documentsAccepted = documentsAccepted;
        return this;
    }


    @JsonProperty("documentsAccepted")
    public Boolean getDocumentsAccepted() {
        return documentsAccepted;
    }

    @JsonProperty("documentsAccepted")
    public void setDocumentsAccepted(Boolean documentsAccepted) {
        this.documentsAccepted = documentsAccepted;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientEvaluateResponseInfoDto clientEvaluateResponseInfo = (ClientEvaluateResponseInfoDto) o;
        return Objects.equals(this.aggregationResult, clientEvaluateResponseInfo.aggregationResult) &&
                Objects.equals(this.mdcCheck, clientEvaluateResponseInfo.mdcCheck) &&
                Objects.equals(this.documentsAccepted, clientEvaluateResponseInfo.documentsAccepted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aggregationResult, mdcCheck, documentsAccepted);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClientEvaluateResponseInfoDto {\n");

        sb.append("    aggregationResult: ").append(toIndentedString(aggregationResult)).append("\n");
        sb.append("    mdcCheck: ").append(toIndentedString(mdcCheck)).append("\n");
        sb.append("    documentsAccepted: ").append(toIndentedString(documentsAccepted)).append("\n");
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

