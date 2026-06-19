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

package com.wultra.app.onboardingserver.controller.api.v2;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitV2Request;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationRestService;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ErrorResponse;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.*;

/**
 * Identity Verification V2 REST API Controller.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@RestController
@AllArgsConstructor
@RequestMapping(value = "api/v2/identity")
@Slf4j
class IdentityVerificationV2Controller {

    private final IdentityVerificationRestService identityVerificationRestService;

    @Operation(
            summary = "Submit identity verification documents (V2)",
            description = "Submit documents for identity verification process or re-submit previously submitted documents.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Documents successfully submitted"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Reached limit for onboarding processes",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unknown server error while processing request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
            }
    )
    @PostMapping("document/submit")
    @PowerAuthEncryption(scope = EncryptionScope.ACTIVATION_SCOPE)
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public Response submitDocuments(
            @NotNull @EncryptedRequestBody @Valid final ObjectRequest<DocumentSubmitV2Request> request,
            @Parameter(hidden = true) final EncryptionContext encryptionContext,
            @Parameter(hidden = true) final PowerAuthApiAuthentication apiAuthentication
    ) throws OnboardingProcessException, RemoteCommunicationException, IdentityVerificationLimitException, DocumentSubmitException, PowerAuthEncryptionException, IdentityVerificationException, OnboardingProcessLimitException, PowerAuthAuthenticationException {

        final DocumentSubmitV2Request requestObject = request.getRequestObject();
        logger.info("Submit documents v2 initiated", action("submitDocumentsV2"), stateInitiated(), kv("processId", requestObject.processId()));

        try {
            final var response = identityVerificationRestService.submitDocumentsV2(requestObject, encryptionContext, apiAuthentication);

            logger.info("Submit documents v2 succeeded", action("submitDocumentsV2"), stateSucceeded());
            return response;
        } catch (final Exception e) {
            logger.error("Submit documents v2 failed", action("submitDocumentsV2"), stateFailed());
            throw e;
        }
    }
}
