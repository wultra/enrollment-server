/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.impl.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.errorhandling.ActivationCodeException;
import com.wultra.security.powerauth.client.model.entity.ApplicationConfigurationItem;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.response.GetApplicationConfigResponse;
import com.wultra.security.powerauth.client.v4.PowerAuthClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * Specialization of {@link DelegatingActivationCodeHandler} using application configuration from PowerAuth Server.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@AllArgsConstructor
@Slf4j
class PowerAuthActivationCodeHandler implements DelegatingActivationCodeHandler {

    private static final String ACTIVATION_CODE = "activation_code";

    private final PowerAuthClient powerAuthClient;

    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public String fetchDestinationApplicationId(final String targetApplicationId, final String sourceApplicationId, final List<String> activationFlags, final List<String> applicationRoles) throws ActivationCodeException {
        try {
            final GetApplicationConfigResponse applicationConfig = powerAuthClient.getApplicationConfig(sourceApplicationId);
            return applicationConfig.getApplicationConfigs().stream()
                    .filter(it -> it.getKey().equals(ACTIVATION_CODE))
                    .findFirst()
                    .map(ApplicationConfigurationItem::getValues)
                    .map(this::convert)
                    .map(ActivationCodeConfiguration::allowedTargetApplicationIds)
                    .filter(allowedIds -> allowedIds.contains(targetApplicationId))
                    .map(allowedIds -> targetApplicationId)
                    .orElse(null);
        } catch (PowerAuthClientException e) {
            throw new ActivationCodeException("Fetching application configuration for ID: %s failed.".formatted(sourceApplicationId), e);
        }
    }

    private ActivationCodeConfiguration convert(final List<Object> values) {
        return values.stream()
                .map(this::convert)
                .filter(Objects::nonNull)
                .filter(ActivationCodeConfiguration::isTypeOfCopy)
                .findFirst()
                .orElse(null);
    }

    private ActivationCodeConfiguration convert(final Object value) {
        try {
            return objectMapper.convertValue(value, new TypeReference<>() {});
        } catch (IllegalArgumentException e) {
            logger.warn("Unable to convert {}", value, e);
            return null;
        }
    }

    private record ActivationCodeConfiguration(List<String> allowedTargetApplicationIds, ActivationType type) {
        boolean isTypeOfCopy() {
            return type == ActivationType.COPY;
        }
    }

    private enum ActivationType {

        /**
         * Keeps the original activation.
         */
        COPY,

        /**
         * The original activation is to be removed.
         */
        MOVE
    }
}
