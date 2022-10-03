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
package com.wultra.app.enrollmentserver.controller.api;

import com.wultra.app.enrollmentserver.api.model.enrollment.response.ConfigurationResponse;
import com.wultra.app.enrollmentserver.configuration.MobileApplicationConfigurationProperties;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.security.powerauth.crypto.lib.encryptor.ecies.model.EciesScope;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller publishing configuration to inform the client app.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@RestController
@RequestMapping(value = "api/configuration")
@Slf4j
public class ConfigurationController {

    private final MobileApplicationConfigurationProperties mobileApplicationConfigurationProperties;

    @Autowired
    public ConfigurationController(final MobileApplicationConfigurationProperties mobileApplicationConfigurationProperties) {
        this.mobileApplicationConfigurationProperties = mobileApplicationConfigurationProperties;
    }

    /**
     * Fetch enrollment server configuration.
     *
     * @param request request
     * @param eciesContext ECIES context.
     * @return configuration
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     */
    @PostMapping
    @Operation(summary = "Provide enrollment configuration")
    @PowerAuthEncryption(scope = EciesScope.APPLICATION_SCOPE)
    public ObjectResponse<ConfigurationResponse> fetchConfiguration(@EncryptedRequestBody Object request,
                                                     @Parameter(hidden = true) EciesEncryptionContext eciesContext) throws PowerAuthEncryptionException{

        if (eciesContext == null) {
            throw new PowerAuthEncryptionException("Encryption failed");
        }

        logger.debug("Server successfully decrypted signed data: {}", request);

        return new ObjectResponse<>(createConfigurationResponse());
    }

    private ConfigurationResponse createConfigurationResponse() {
        final var mobileApplication = new ConfigurationResponse.MobileApplication();
        mobileApplication.setAndroid(convert(mobileApplicationConfigurationProperties.getAndroid()));
        mobileApplication.setIOs(convert(mobileApplicationConfigurationProperties.getIOs()));

        final ConfigurationResponse response = new ConfigurationResponse();
        response.setMobileApplication(mobileApplication);
        return response;
    }

    private static ConfigurationResponse.VersionSpecification convert(final MobileApplicationConfigurationProperties.VersionSpecification source) {
        final var target = new ConfigurationResponse.VersionSpecification();
        target.setMinimalVersion(source.getMinimalVersion());
        target.setCurrentVersion(source.getCurrentVersion());
        return target;
    }
}
