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
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuth;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.getlime.security.powerauth.rest.api.spring.exception.authentication.PowerAuthTokenInvalidException;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller publishing REST services for uploading Innovatrics liveness data.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and ${enrollment-server-onboarding.onboarding-process.enabled} == true
        """)
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
    @PowerAuth(resourceId = "/api/identity/presence-check/upload", signatureType = PowerAuthSignatureTypes.POSSESSION)
    public Response upload(
            @EncryptedRequestBody byte[] requestData,
            @Parameter(hidden = true) EncryptionContext encryptionContext,
            @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws IdentityVerificationException, PowerAuthAuthenticationException, PowerAuthEncryptionException, RemoteCommunicationException {

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
        return new Response();
    }
}
