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
package com.wultra.app.onboardingserver.provider.innovatrics;

import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuth;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthActivation;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import com.wultra.security.powerauth.rest.api.spring.exception.authentication.PowerAuthTokenInvalidException;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


/**
 * Controller publishing REST services for uploading Innovatrics liveness data.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "innovatrics")
@RestController
@RequestMapping(value = "api/identity")
@AllArgsConstructor
@Slf4j
class InnovatricsLivenessController {

    private InnovatricsLivenessService innovatricsLivenessService;

    /**
     * Upload Innovatrics liveness data.
     *
     * @param requestData Binary request data
     * @param encryptionContext Encryption context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Presence check initialization response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws PowerAuthEncryptionException Thrown when request decryption fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws RemoteCommunicationException Thrown when there is a problem with the remote communication.
     */
    @PostMapping("presence-check/upload")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/presence-check/upload", authenticationCodeType = PowerAuthCodeType.POSSESSION)
    public Response upload(
            @EncryptedRequestBody byte[] requestData,
            @Parameter(hidden = true) EncryptionContext encryptionContext,
            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException {

        logger.info("action: upload, state: initiated, activationId: {}", extractActivation(apiAuthentication).orElse(null));
        if (apiAuthentication == null) {
            throw new PowerAuthTokenInvalidException("Unable to verify device registration when uploading liveness");
        }

        if (encryptionContext == null) {
            throw new PowerAuthEncryptionException("ECIES encryption failed when uploading liveness");
        }

        if (requestData == null) {
            throw new PowerAuthEncryptionException("Invalid request received when uploading liveness");
        }

        innovatricsLivenessService.upload(requestData, encryptionContext);
        logger.info("action: upload, state: succeeded");
        return new Response();
    }

    // TODO (racansky, 2026-02-25, #1589) remove when validation of encryptionContext made implicit
    private static Optional<PowerAuthActivation> extractActivation(final PowerAuthApiAuthentication apiAuthentication) {
        return Optional.ofNullable(apiAuthentication).map(PowerAuthApiAuthentication::getActivationContext);
    }
}
