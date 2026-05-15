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

import com.wultra.app.enrollmentserver.api.model.enrollment.request.ActivationCodeRequest;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.ActivationCodeResponse;
import com.wultra.app.enrollmentserver.errorhandling.ActivationCodeException;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.impl.service.ActivationCodeService;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuth;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionScope;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static net.logstash.logback.argument.StructuredArguments.kv;

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
@AllArgsConstructor
@Slf4j
public class ActivationCodeController {

    private final ActivationCodeService activationCodeService;

    /**
     * Controller request handler for requesting the activation code.
     *
     * @param request Request with activation OTP.
     * @param encryptionContext ECIES encryption context.
     * @param apiAuthentication Authentication object with user and app details.
     * @return New activation code, activation code signature and activation ID.
     * @throws PowerAuthAuthenticationException In case user authentication fails.
     * @throws PowerAuthEncryptionException In case request decryption fails.
     * @throws InvalidRequestObjectException In case the object validation fails.
     * @throws ActivationCodeException In case fetching the activation code fails.
     */
    @Operation(
            summary = "Request activation code",
            description = """
                    Handle a request for activation code in activation spawn.
                    The target application is specified in the request body (applicationId),
                    while the source application is identified from the PowerAuth signature.""",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Activation code successfully requested."),
                    @ApiResponse(responseCode = "401", description = "PowerAuth authentication failed.", content = @Content(schema = @Schema(hidden = true)))
            }
    )
    @PostMapping("code")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/activation/code", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<ActivationCodeResponse> requestActivationCode(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<ActivationCodeRequest> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext,
            @Parameter(hidden = true) final PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, ActivationCodeException, PowerAuthEncryptionException {

        final ActivationCodeRequest requestObject = request.getRequestObject();
        logger.info("", kv("action", "requestActivationCode"), kv("state", "initiated"), kv("applicationId", requestObject.getApplicationId()));
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when fetching activation code");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when fetching activation code");
        }

        // Check if the request was correctly decrypted
        if (encryptionContext == null) {
            logger.error("ECIES encryption failed when fetching activation code");
            throw new PowerAuthEncryptionException("ECIES decryption failed when fetching activation code");
        }

        final ActivationCodeResponse response = activationCodeService.requestActivationCode(requestObject, apiAuthentication);
        logger.info("", kv("action", "requestActivationCode"), kv("state", "succeeded"));
        return new ObjectResponse<>(response);
    }
}
