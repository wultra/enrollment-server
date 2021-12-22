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
package com.wultra.app.enrollmentserver.controller.api;

import com.wultra.app.enrollmentserver.errorhandling.ActivationCodeException;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.impl.service.ActivationCodeService;
import com.wultra.app.enrollmentserver.model.request.ActivationCodeRequest;
import com.wultra.app.enrollmentserver.model.response.ActivationCodeResponse;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.security.powerauth.crypto.lib.encryptor.ecies.model.EciesScope;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuth;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.spring.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller publishing REST services for obtaining a new activation code.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server.activation-spawn.enabled",
        havingValue = "true"
)
@RestController
@RequestMapping(value = "api/activation")
public class ActivationCodeController {

    private static final Logger logger = LoggerFactory.getLogger(ActivationCodeController.class);

    private final ActivationCodeService activationCodeService;

    /**
     * Default autowiring constructor.
     *
     * @param activationCodeService Activation code service.
     */
    @Autowired
    public ActivationCodeController(ActivationCodeService activationCodeService) {
        this.activationCodeService = activationCodeService;
    }

    /**
     * Controller request handler for requesting the activation code.
     *
     * @param request Request with activation OTP.
     * @param eciesContext ECIES encryption context.
     * @param apiAuthentication Authentication object with user and app details.
     * @return New activation code, activation code signature and activation ID.
     * @throws PowerAuthAuthenticationException In case user authentication fails.
     * @throws InvalidRequestObjectException In case the object validation fails.
     * @throws ActivationCodeException In case fetching the activation code fails.
     */
    @RequestMapping(value = "code", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/activation/code", signatureType = {
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<ActivationCodeResponse> requestActivationCode(@EncryptedRequestBody ObjectRequest<ActivationCodeRequest> request,
                                                                        @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                        @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, ActivationCodeException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when fetching activation code");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when fetching activation code");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when fetching activation code");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when fetching activation code");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when fetching activation code");
            throw new PowerAuthAuthenticationException("Invalid request received when fetching activation code");
        }

        // Request the activation code details.
        final ActivationCodeResponse response = activationCodeService.requestActivationCode(request.getRequestObject(), apiAuthentication);
        return new ObjectResponse<>(response);
    }

}
