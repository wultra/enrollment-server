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
            final Map<String, Object> json = mapper.readValue(entity.getCustomData(), new TypeReference<>(){});
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
        try {
            logger.debug("Setting locale to custom_data: {} of process ID: {}", entity.getCustomData(), entity.getId());
            final Map<String, Object> json = mapper.readValue(entity.getCustomData(), new TypeReference<>(){});
            json.put(CUSTOM_DATA_LOCALE_KEY, locale.getLanguage());
            entity.setCustomData(mapper.writeValueAsString(json));
        } catch (JsonProcessingException e) {
            logger.warn("Problem to parse custom_data of process ID: {}", entity.getId(), e);
        }
    }
}
