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
package com.wultra.app.onboardingserver.controller.api;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.ConfigurationRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.ConfigurationResponse;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.onboardingserver.impl.service.ConfigurationService;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Configuration controller.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping(value = "api/configuration")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @PostMapping
    @PowerAuthEncryption(scope = EncryptionScope.APPLICATION_SCOPE)
    @Operation(
            summary = "Fetch onboarding process configuration.",
            description = "Fetch onboarding process configuration for the given type."
    )
    public ObjectResponse<ConfigurationResponse> fetchConfiguration(
            @EncryptedRequestBody @Valid final ObjectRequest<ConfigurationRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext) throws PowerAuthEncryptionException, InvalidRequestObjectException {

        logger.info("action: fetchConfiguration, state: initiated");

        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES decryption failed");
        }

        if (request == null || request.getRequestObject() == null) {
            throw new PowerAuthEncryptionException("Invalid request received");
        }

        final String processType = request.getRequestObject().processType();
        final ConfigurationResponse result = configurationService.fetchConfiguration(processType)
                .map(OnboardingProcessConfigurationEntity::getConfiguration)
                .map(ConfigurationController::convert)
                .orElseThrow(() -> new InvalidRequestObjectException("Configuration not found for processType: " + processType));

        logger.info("action: fetchConfiguration, state: succeeded");
        logger.debug("action: fetchConfiguration, state: succeeded, result: {}", result);

        return new ObjectResponse<>(result);
    }

    private static ConfigurationResponse convert(final OnboardingProcessConfigurationValue source) {
        return ConfigurationResponse.builder()
                .enabled(source.enabled())
                .otpForIdentification(source.otpForIdentification())
                .otpForIdentityVerification(source.otpForIdentityVerification())
                .documents(convert(source.documents()))
                .build();
    }

    private static ConfigurationResponse.Documents convert(final OnboardingProcessConfigurationValue.Documents source) {
        if (source == null) {
            return null;
        }

        return ConfigurationResponse.Documents.builder()
                .requiredDocumentsCount(source.requiredDocumentsCount())
                .items(convert(source.items()))
                .build();
    }

    private static List<ConfigurationResponse.Document> convert(final List<OnboardingProcessConfigurationValue.Document> source) {
        if (source == null) {
            return null;
        }

        return source.stream()
                .map(ConfigurationController::convert)
                .toList();
    }

    private static ConfigurationResponse.Document convert(final OnboardingProcessConfigurationValue.Document source) {
        return ConfigurationResponse.Document.builder()
                .type(convert(source.type()))
                .mandatory(source.mandatory())
                .sideCount(source.sideCount())
                .build();
    }

    private static ConfigurationResponse.DocumentType convert(final OnboardingProcessConfigurationValue.DocumentType source) {
        return switch (source) {
            case ID_CARD -> ConfigurationResponse.DocumentType.ID_CARD;
            case PASSPORT -> ConfigurationResponse.DocumentType.PASSPORT;
            case DRIVING_LICENCE -> ConfigurationResponse.DocumentType.DRIVING_LICENCE;
        };
    }
}
