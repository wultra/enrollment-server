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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.api.model.enrollment.request.ActivationCodeRequest;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.ActivationCodeResponse;
import com.wultra.app.enrollmentserver.errorhandling.ActivationCodeException;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.impl.service.converter.ActivationCodeConverter;
import com.wultra.app.enrollmentserver.model.validator.ActivationCodeRequestValidator;
import com.wultra.core.audit.base.Audit;
import com.wultra.core.audit.base.model.AuditDetail;
import com.wultra.security.powerauth.client.model.enumeration.ActivationTransferType;
import com.wultra.security.powerauth.client.model.enumeration.CommitPhase;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.InitActivationRequest;
import com.wultra.security.powerauth.client.model.response.InitActivationResponse;
import com.wultra.security.powerauth.client.v4.PowerAuthClient;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for fetching the new activation codes.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
public class ActivationCodeService {

    private final PowerAuthClient powerAuthClient;
    private final ActivationCodeConverter activationCodeConverter;
    private final HttpCustomizationService httpCustomizationService;

    private DelegatingActivationCodeHandler delegatingActivationCodeHandler;

    private final Audit audit;

    /**
     * Request activation code for provided OTP value, user ID and app ID.
     *
     * @param request Request with OTP value.
     * @param apiAuthentication Authentication object.
     * @return Response with activation code, activation code signature, and activation ID.
     * @throws ActivationCodeException In case of invalid user / app attributes or communication with PowerAuth Service fails.
     * @throws InvalidRequestObjectException In case request object validation fails.
     */
    public ActivationCodeResponse requestActivationCode(ActivationCodeRequest request, PowerAuthApiAuthentication apiAuthentication) throws InvalidRequestObjectException, ActivationCodeException {

        final String sourceUserId = apiAuthentication.getUserId();
        final String sourceApplicationId = apiAuthentication.getApplicationId();

        logger.info("Activation code registration started, user ID: {}", sourceUserId);

        // Validate the request object
        final String error = ActivationCodeRequestValidator.validate(request);
        if (error != null) {
            logger.error("Invalid object in activation code request - {}, user ID: {}", error, sourceUserId);
            throw new InvalidRequestObjectException();
        }

        final DelegatingActivationCodeHandler.TransferConfigurationResponse response = delegatingActivationCodeHandler.fetchTransferConfiguration(DelegatingActivationCodeHandler.TransferConfigurationRequest.builder()
                .targetApplicationId(request.getApplicationId())
                .sourceApplicationId(sourceApplicationId)
                .build());

        if (response == null) {
            throw new ActivationCodeException("Invalid application ID. The provided source application ID: %s cannot activate the target application ID: %s.".formatted(sourceApplicationId, request.getApplicationId()));
        }

        final String targetApplicationId = response.applicationId();

        try {
            // Create a new activation
            logger.info("Calling PowerAuth Server with new activation request, user ID: {}, application ID: {}", sourceUserId, targetApplicationId);
            final InitActivationRequest initRequest = new InitActivationRequest();
            initRequest.setUserId(sourceUserId);
            initRequest.setApplicationId(response.applicationId());
            initRequest.setParentActivationId(apiAuthentication.getActivationContext().getActivationId());
            initRequest.setTransferType(convert(response.type()));
            initRequest.setCommitPhase(CommitPhase.ON_KEY_EXCHANGE);
            initRequest.setActivationOtp(request.getOtp());
            initRequest.setFlags(response.initialFlags());
            initRequest.setAdditionalData(Map.of(
                    "sourceApplicationId", sourceApplicationId,
                    "targetApplicationId", targetApplicationId,
                    "transferType", response.type().name()));

            final InitActivationResponse iar = powerAuthClient.initActivation(
                    initRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );
            logger.info("Successfully obtained a new activation with ID: {}", iar.getActivationId());
            auditInitActivation(iar);

            return activationCodeConverter.convert(iar);
        } catch (PowerAuthClientException e) {
            throw new ActivationCodeException("Unable to call PowerAuth.", e);
        }
    }

    private static ActivationTransferType convert(final DelegatingActivationCodeHandler.ActivationTransferType source) {
        return switch (source) {
            case SPAWN -> ActivationTransferType.SPAWN;
            case MOVE -> ActivationTransferType.MOVE;
        };
    }

    private void auditInitActivation(final InitActivationResponse response) {
        final String userId = response.getUserId();
        final AuditDetail auditDetail = AuditDetail.builder()
                .type("activation")
                .subjectId(userId)
                .param("activationId", response.getActivationId())
                .param("userId", userId)
                .build();
        audit.info("Init activation for user: {}", auditDetail, userId);
    }
}
