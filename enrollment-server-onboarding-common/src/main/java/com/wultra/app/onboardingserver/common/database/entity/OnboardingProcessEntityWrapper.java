/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.common.database.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * Wrapper for {@link OnboardingProcessEntity}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
public final class OnboardingProcessEntityWrapper {

    /**
     * Key for {@link OnboardingProcessEntity#getCustomData()} storing locale.
     */
    private static final String CUSTOM_DATA_LOCALE_KEY = "locale";
    private static final String CUSTOM_DATA_IP_ADDRESS_KEY = "ipAddress";
    private static final String CUSTOM_DATA_USER_AGENT_KEY = "userAgent";

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final OnboardingProcessEntity entity;

    private final ObjectMapper mapper = new ObjectMapper();

    public OnboardingProcessEntityWrapper(final OnboardingProcessEntity entity) {
        this.entity = entity;
    }

    /**
     * Get locale from {@code customData}.
     *
     * @return locale
     */
    public Locale getLocale() {
        try {
            logger.debug("Getting locale from custom_data: {} of process ID: {}", entity.getCustomData(), entity.getId());
            final Map<String, Object> json = readCustomData();
            final String language = json.getOrDefault(CUSTOM_DATA_LOCALE_KEY, DEFAULT_LOCALE.getLanguage()).toString();
            return new Locale(language);
        } catch (JsonProcessingException e) {
            logger.warn("Problem to parse custom_data of process ID: {}", entity.getId(), e);
            return DEFAULT_LOCALE;
        }
    }

    /**
     * Set the given locale to {@code customData}.
     *
     * @param locale locale
     */
    public void setLocale(final Locale locale) {
        setValue(CUSTOM_DATA_LOCALE_KEY, locale.getLanguage());
    }

    /**
     * Set the given IP address to {@code customData}.
     *
     * @param ipAddress IP address
     */
    public void setIpAddress(final String ipAddress) {
        setValue(CUSTOM_DATA_IP_ADDRESS_KEY, ipAddress);
    }

    /**
     * Get IP address from {@code customData}.
     *
     * @return IP address
     */
    public String getIpAddress() {
        return getValueString(CUSTOM_DATA_IP_ADDRESS_KEY);
    }

    /**
     * Set the given user agent to {@code customData}.
     *
     * @param userAgent User agent
     */
    public void setUserAgent(final String userAgent) {
        setValue(CUSTOM_DATA_USER_AGENT_KEY, userAgent);
    }

    /**
     * Get user agent from {@code customData}.
     *
     * @return user agent
     */
    public String getUserAgent() {
        return getValueString(CUSTOM_DATA_USER_AGENT_KEY);
    }

    /**
     * Return string {@code OnboardingProcessEntity#fdsData} as map.
     *
     * @return FDS data or null
     */
    public Map<String, Object> getFdsData() {
        try {
            final String fdsData = entity.getFdsData();
            logger.debug("Getting fds_data: {} of process ID: {}", fdsData, entity.getId());
            if (fdsData == null) {
                return null;
            }
            return mapper.readValue(fdsData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.warn("Problem to parse fds_data of process ID: {}", entity.getId(), e);
            return null;
        }
    }

    /**
     * Set the given map of FDS data to string {@code OnboardingProcessEntity#fdsData}.
     *
     * @param fdsData FDS data, may be null
     */
    public void setFdsData(final Map<String, Object> fdsData) {
        try {
            if (fdsData == null) {
                entity.setFdsData(null);
            } else {
                logger.debug("Setting fds_data: {} of process ID: {}", fdsData, entity.getId());
                entity.setFdsData(mapper.writeValueAsString(fdsData));
            }
        } catch (JsonProcessingException e) {
            logger.warn("Problem to parse fds_data of process ID: {}", entity.getId(), e);
        }
    }

    private void setValue(final String key, final Object value) {
        try {
            logger.debug("Setting {} to custom_data: {} of process ID: {}", key, entity.getCustomData(), entity.getId());
            final Map<String, Object> json = readCustomData();
            json.put(key, value);
            entity.setCustomData(mapper.writeValueAsString(json));
        } catch (JsonProcessingException e) {
            logger.warn("Problem to parse custom_data of process ID: {}", entity.getId(), e);
        }
    }

    private String getValueString(final String key) {
        try {
            logger.debug("Getting {} from custom_data: {} of process ID: {}", key, entity.getCustomData(), entity.getId());
            final Map<String, Object> json = readCustomData();
            return json.getOrDefault(key, "unknown").toString();
        } catch (JsonProcessingException e) {
            logger.warn("Problem to parse custom_data of process ID: {}", entity.getId(), e);
            return "unknown";
        }
    }

    private Map<String, Object> readCustomData() throws JsonProcessingException {
        final Map<String, Object> value = mapper.readValue(entity.getCustomData(), new TypeReference<>() {});
        if (value == null) {
            logger.warn("Read null value from custom_data: {} of process ID: {} ", entity.getCustomData(), entity.getId());
            return Collections.emptyMap();
        }
        return value;
    }
}
