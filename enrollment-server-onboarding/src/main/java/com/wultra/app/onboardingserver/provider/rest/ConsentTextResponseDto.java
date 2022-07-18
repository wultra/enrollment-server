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
@JsonTypeName("ConsentTextResponse")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen")
class ConsentTextResponseDto {

    private String consentText;

    /**
     * Resulting consent text in specified language
     **/
    public ConsentTextResponseDto consentText(String consentText) {
        this.consentText = consentText;
        return this;
    }


    @JsonProperty("consentText")
    public String getConsentText() {
        return consentText;
    }

    @JsonProperty("consentText")
    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsentTextResponseDto consentTextResponse = (ConsentTextResponseDto) o;
        return Objects.equals(this.consentText, consentTextResponse.consentText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consentText);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ConsentTextResponseDto {\n");

        sb.append("    consentText: ").append(toIndentedString(consentText)).append("\n");
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

