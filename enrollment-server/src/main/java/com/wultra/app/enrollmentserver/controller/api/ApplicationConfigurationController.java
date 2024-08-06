/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2024 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.api.model.enrollment.request.OidcApplicationConfigurationRequest;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.OidcApplicationConfigurationResponse;
import com.wultra.app.enrollmentserver.errorhandling.PowerAuthApplicationConfigurationException;
import com.wultra.app.enrollmentserver.impl.service.ApplicationConfigurationService;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that provides application configuration.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@RestController
@RequestMapping("/api/config")
@Slf4j
@AllArgsConstructor
public class ApplicationConfigurationController {

    private ApplicationConfigurationService applicationConfigurationService;

    /**
     * Fetch OIDC application configuration.
     *
     * @param request Request OIDC application configuration.
     * @param encryptionContext PowerAuth ECIES encryption context.
     * @return OIDC application configuration.
     * @throws PowerAuthApplicationConfigurationException In case there is an error while fetching claims.
     * @throws PowerAuthEncryptionException In case of failed encryption.
     */
    @PowerAuthEncryption(scope = EncryptionScope.APPLICATION_SCOPE)
    @PostMapping("oidc")
    @Operation(
            summary = "Fetch OIDC application configuration.",
            description = "Fetch OIDC application configuration."
    )
    public ObjectResponse<OidcApplicationConfigurationResponse> fetchOidcConfiguration(
            @EncryptedRequestBody OidcApplicationConfigurationRequest request,
            @Parameter(hidden = true) EncryptionContext encryptionContext) throws PowerAuthApplicationConfigurationException, PowerAuthEncryptionException {
        if (encryptionContext == null) {
            logger.error("Encryption failed");
            throw new PowerAuthEncryptionException("Encryption failed");
        }

        final OidcApplicationConfigurationResponse result = applicationConfigurationService.fetchOidcApplicationConfiguration(ApplicationConfigurationService.OidcQuery.builder()
                .providerId(request.getProviderId())
                .applicationKey(encryptionContext.getApplicationKey())
                .build());
        return new ObjectResponse<>(result);
    }
}
