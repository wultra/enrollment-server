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
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthApplicationConfigurationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import com.wultra.security.powerauth.rest.api.spring.service.oidc.OidcApplicationConfiguration;
import com.wultra.security.powerauth.rest.api.spring.service.oidc.OidcApplicationConfigurationService;
import com.wultra.security.powerauth.rest.api.spring.service.oidc.OidcConfigurationQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    private OidcApplicationConfigurationService oidcApplicationConfigurationService;

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
            @NotNull @EncryptedRequestBody @Valid final OidcApplicationConfigurationRequest request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext) throws PowerAuthEncryptionException, PowerAuthApplicationConfigurationException {

        logger.info("action: fetchOidcConfiguration, state: initiated, providerId: {}", request != null ? request.getProviderId() : null);

        if (encryptionContext == null) {
            logger.error("action: fetchOidcConfiguration, state: failed, reason: encryptionFailed");
            throw new PowerAuthEncryptionException("Encryption failed");
        }

        final OidcApplicationConfiguration oidcApplicationConfiguration = oidcApplicationConfigurationService.fetchOidcApplicationConfiguration(OidcConfigurationQuery.builder()
                .providerId(request.getProviderId())
                .applicationKey(encryptionContext.getApplicationKey())
                .build());
        final OidcApplicationConfigurationResponse result = convert(oidcApplicationConfiguration);
        logger.info("action: fetchOidcConfiguration, state: succeeded");
        return new ObjectResponse<>(result);
    }

    private static OidcApplicationConfigurationResponse convert(final OidcApplicationConfiguration source) {
        final OidcApplicationConfigurationResponse target = new OidcApplicationConfigurationResponse();
        target.setClientId(source.getClientId());
        target.setAuthorizeUri(source.getAuthorizeUri());
        target.setScopes(source.getScopes());
        target.setRedirectUri(source.getRedirectUri());
        target.setProviderId(source.getProviderId());
        target.setPkceEnabled(source.isPkceEnabled());
        return target;
    }
}
