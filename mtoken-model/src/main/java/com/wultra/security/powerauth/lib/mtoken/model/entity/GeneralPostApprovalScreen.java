/*
 * PowerAuth Mobile Token Model
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
package com.wultra.security.powerauth.lib.mtoken.model.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Specialization of {@link PostApprovalScreen} for general usage.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public class GeneralPostApprovalScreen extends PostApprovalScreen {

    @NotNull
    private GeneralPayload payload;

    @Override
    public GeneralPayload getPayload() {
        return payload;
    }

    public void setPayload(GeneralPayload payload) {
        this.payload = payload;
    }

    /**
     * Specialization of {@link Payload} for general usage.
     */
    @Data
    public static class GeneralPayload implements Payload {

        private final Map<String, Object> properties = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return properties;
        }

        @JsonAnySetter
        public void setProperty(String key, Object value) {
            this.properties.put(key, value);
        }
    }
}
