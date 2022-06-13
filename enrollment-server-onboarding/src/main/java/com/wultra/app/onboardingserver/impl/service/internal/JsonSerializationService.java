/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.impl.service.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

/**
 * Service class used for JSON serialization.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class JsonSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(JsonSerializationService.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Deserialize an object from JSON.
     * @param json Serialized JSON representation of the object..
     * @return Deserialized object
     */
    @Nullable
    public <T> T deserialize(String json, Class<T> cls) {
        try {
            return objectMapper.readValue(json, cls);
        } catch (JsonProcessingException e) {
            logger.error("JSON serialization failed due to an error", e);
            return null;
        }
    }

    /**
     * Serialize an object into JSON.
     * @param object Object.
     * @return Serialized JSON representation of the object.
     */
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("JSON serialization failed due to an error", e);
            return null;
        }
    }
}