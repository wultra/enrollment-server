/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.errorhandling.MobileTokenAuthException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenException;
import com.wultra.app.enrollmentserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.enrollmentserver.impl.service.MobileTokenService;
import com.wultra.app.enrollmentserver.impl.service.OperationApproveParameterObject;
import com.wultra.app.enrollmentserver.impl.service.OperationRejectParameterObject;
import com.wultra.core.http.common.request.RequestContext;
import com.wultra.core.http.common.request.RequestContextConverter;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.ObjectResponse;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.error.PowerAuthError;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.enumeration.ErrorCode;
import com.wultra.security.powerauth.lib.mtoken.model.request.OperationApproveRequest;
import com.wultra.security.powerauth.lib.mtoken.model.request.OperationDetailRequest;
import com.wultra.security.powerauth.lib.mtoken.model.request.OperationRejectRequest;
import com.wultra.security.powerauth.lib.mtoken.model.response.MobileTokenResponse;
import com.wultra.security.powerauth.lib.mtoken.model.response.OperationListResponse;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuth;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.wultra.app.enrollmentserver.controller.api.LoggingUtils.extractActivationId;
import static com.wultra.app.enrollmentserver.logging.StructuredLogging.*;

/**
 * Controller that publishes the default mobile token services.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server.mtoken.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RestController
@RequestMapping("api/auth/token/app")
@AllArgsConstructor
@Slf4j
public class MobileTokenController {

    private static final String APPLICATION_NOT_FOUND = "ERR0015";
    private static final String INVALID_REQUEST = "ERR0024";
    private static final String OPERATION_NOT_FOUND = "ERR0034";
    private static final String OPERATION_INVALID_STATE = "ERR0036";
    private static final String OPERATION_APPROVE_FAILURE = "ERR0037";
    private static final String OPERATION_REJECT_FAILURE = "ERR0038";

    /**
     * Disallowed flags contain onboarding flags used before the onboarding process is finished.
     * @implNote These flags are assigned in the onboarding server module in the {@code ActivationFlagService}.
     */
    private static final List<String> DISALLOWED_FLAGS = List.of("VERIFICATION_PENDING", "VERIFICATION_IN_PROGRESS");

    private final MobileTokenService mobileTokenService;

    /**
     * Get the list of pending operations.
     *
     * @param auth Authentication object.
     * @param locale Locale.
     * @return List of pending operations.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws MobileTokenConfigurationException In the case of system misconfiguration.
     */
    @PostMapping("/operation/list")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION,
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<OperationListResponse> operationList(@Parameter(hidden = true) PowerAuthApiAuthentication auth, @Parameter(hidden = true) Locale locale) throws MobileTokenException, MobileTokenConfigurationException, RemoteCommunicationException {
        logger.info("", action("operationList"), stateInitiated(), kv("activationId", extractActivationId(auth)));
        validateApiAuthentication(auth);

        try {
            final String userId = auth.getUserId();
            final String applicationId = auth.getApplicationId();
            final String activationId = auth.getActivationContext().getActivationId();
            final String language = locale.getLanguage();
            final OperationListResponse listResponse = mobileTokenService.operationListForUser(userId, applicationId, language, activationId, true);
            logger.info("", action("operationList"), stateSucceeded());
            final Date currentTimestamp = new Date();
            return new MobileTokenResponse<>(listResponse, currentTimestamp);
        } catch (PowerAuthClientException e) {
            final String errorCode = e.getPowerAuthError().map(PowerAuthError::getCode).orElse("ERROR_CODE_MISSING");
            switch (errorCode) {
                case APPLICATION_NOT_FOUND -> {
                    logger.warn("", action("operationList"), stateFailed(), kv("reason", "applicationNotFound"));
                    throw new MobileTokenException(ErrorCode.INVALID_APPLICATION, "No application was found with the provided identifier.", e);
                }
                case INVALID_REQUEST -> {
                    logger.warn("", action("operationList"), stateFailed(), kv("reason", "validation"));
                    throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Request validation error: %s".formatted(e.getMessage()), e);
                }
                default -> {
                    logger.warn("", action("operationList"), stateFailed(), kv("reason", "powerAuthConnection"));
                    throw new RemoteCommunicationException("Unable to call upstream service.", e);
                }
            }
        }
    }

    /**
     * Get the detail of an operation.
     *
     * @param auth Authentication object.
     * @param locale Locale.
     * @return List of pending operations.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws MobileTokenConfigurationException In the case of system misconfiguration.
     */
    @PostMapping("/operation/detail")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION,
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<Operation> fetchOperationDetail(
            @Valid @RequestBody final ObjectRequest<OperationDetailRequest> request,
            @Parameter(hidden = true) final PowerAuthApiAuthentication auth,
            @Parameter(hidden = true) final Locale locale) throws MobileTokenException, MobileTokenConfigurationException, RemoteCommunicationException {

        final String operationId = request.getRequestObject().getId();
        logger.info("", action("fetchOperationDetail"), stateInitiated(), kv("activationId", extractActivationId(auth)), kv("operationId", operationId));
        validateApiAuthentication(auth);

        try {
            final String language = locale.getLanguage();
                final String userId = auth.getUserId();
                final Operation response = mobileTokenService.fetchOperationDetail(operationId, language, userId);
                logger.info("", action("fetchOperationDetail"), stateSucceeded());
                final Date currentTimestamp = new Date();
                return new MobileTokenResponse<>(response, currentTimestamp);

        } catch (PowerAuthClientException e) {
            final String errorCode = e.getPowerAuthError().map(PowerAuthError::getCode).orElse("ERROR_CODE_MISSING");
            switch (errorCode) {
                case OPERATION_NOT_FOUND -> {
                    logger.warn("", action("fetchOperationDetail"), stateFailed(), kv("reason", "operationNotFound"));
                    throw new MobileTokenException(ErrorCode.INVALID_OPERATION, "No operation was found with the provided identifier.", e);
                }
                case INVALID_REQUEST -> {
                    logger.warn("", action("fetchOperationDetail"), stateFailed(), kv("reason", "validation"));
                    throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Request validation error: %s".formatted(e.getMessage()), e);
                }
                default -> {
                    logger.warn("", action("fetchOperationDetail"), stateFailed(), kv("reason", "powerAuthConnection"));
                    throw new RemoteCommunicationException("Unable to call upstream service.", e);
                }
            }
        }
    }

    /**
     * Claim operation for a user.
     *
     * @param auth Authentication object.
     * @param locale Locale.
     * @return List of pending operations.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws MobileTokenConfigurationException In the case of system misconfiguration.
     */
    @PostMapping("/operation/detail/claim")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION,
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<Operation> claimOperation(
            @Valid @RequestBody final ObjectRequest<OperationDetailRequest> request,
            @Parameter(hidden = true) final PowerAuthApiAuthentication auth,
            @Parameter(hidden = true) final Locale locale) throws MobileTokenException, MobileTokenConfigurationException, RemoteCommunicationException {

        final String operationId = request.getRequestObject().getId();
        logger.info("", action("claimOperation"), stateInitiated(), kv("activationId", extractActivationId(auth)), kv("operationId", operationId));
        validateApiAuthentication(auth);

        try {
            final String language = locale.getLanguage();
                final String userId = auth.getUserId();
                final Operation response = mobileTokenService.claimOperation(operationId, language, userId);
                logger.info("", action("claimOperation"), stateSucceeded());
                final Date currentTimestamp = new Date();
                return new MobileTokenResponse<>(response, currentTimestamp);

        } catch (PowerAuthClientException e) {
            final String errorCode = e.getPowerAuthError().map(PowerAuthError::getCode).orElse("ERROR_CODE_MISSING");
            switch (errorCode) {
                case OPERATION_NOT_FOUND -> {
                    logger.warn("", action("claimOperation"), stateFailed(), kv("reason", "operationNotFound"));
                    throw new MobileTokenException(ErrorCode.INVALID_OPERATION, "No operation was found with the provided identifier.", e);
                }
                case INVALID_REQUEST -> {
                    logger.warn("", action("claimOperation"), stateFailed(), kv("reason", "validation"));
                    throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Request validation error: %s".formatted(e.getMessage()), e);
                }
                default -> {
                    logger.warn("", action("claimOperation"), stateFailed(), kv("reason", "powerAuthConnection"));
                    throw new RemoteCommunicationException("Unable to call upstream service.", e);
                }
            }
        }
    }

    /**
     * Get the list of all operations.
     *
     * @param auth Authentication object.
     * @param locale Locale.
     * @return List of all operations.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws MobileTokenConfigurationException In the case of system misconfiguration.
     */
    @PostMapping("/operation/history")
    @PowerAuth(resourceId = "/operation/history", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<OperationListResponse> operationListAll(@Parameter(hidden = true) PowerAuthApiAuthentication auth, @Parameter(hidden = true) Locale locale) throws MobileTokenException, MobileTokenConfigurationException, RemoteCommunicationException {
        logger.info("", action("operationListAll"), stateInitiated(), kv("activationId", extractActivationId(auth)));
        validateApiAuthentication(auth);

        try {
            final String userId = auth.getUserId();
            final String applicationId = auth.getApplicationId();
            final String activationId = auth.getActivationContext().getActivationId();
            final String language = locale.getLanguage();
            final OperationListResponse listResponse = mobileTokenService.operationListForUser(userId, applicationId, language, activationId, false);
            logger.info("", action("operationListAll"), stateSucceeded());
            return new ObjectResponse<>(listResponse);
        } catch (PowerAuthClientException e) {
            final String errorCode = e.getPowerAuthError().map(PowerAuthError::getCode).orElse("ERROR_CODE_MISSING");
            switch (errorCode) {
                case APPLICATION_NOT_FOUND -> {
                    logger.warn("", action("operationListAll"), stateFailed(), kv("reason", "applicationNotFound"));
                    throw new MobileTokenException(ErrorCode.INVALID_APPLICATION, "No application was found with the provided identifier.", e);
                }
                case INVALID_REQUEST -> {
                    logger.warn("", action("operationListAll"), stateFailed(), kv("reason", "validation"));
                    throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Request validation error: %s".formatted(e.getMessage()), e);
                }
                default -> {
                    logger.warn("", action("operationListAll"), stateFailed(), kv("reason", "powerAuthConnection"));
                    throw new RemoteCommunicationException("Unable to call upstream service.", e);
                }
            }
        }
    }

    /**
     * Authorize operation.
     *
     * @param request Request for operation approval.
     * @param auth Authentication object.
     * @param servletRequest HttpServletRequest instance.
     * @return Simple response object.
     * @throws MobileTokenException In the case error mobile token service occurs.
     */
    @PostMapping("/operation/authorize")
    @PowerAuth(resourceId = "/operation/authorize", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE,
            PowerAuthCodeType.POSSESSION_BIOMETRY
    })
    public Response operationApprove(
            @Valid @RequestBody final ObjectRequest<OperationApproveRequest> request,
            @Parameter(hidden = true) final PowerAuthApiAuthentication auth,
            final HttpServletRequest servletRequest) throws MobileTokenException, RemoteCommunicationException {

        final OperationApproveRequest requestObject = request.getRequestObject();
        final String operationId = requestObject.getId();
        logger.info("", action("operationApprove"), stateInitiated(), kv("activationId", extractActivationId(auth)), kv("operationId", operationId));

        try {
            final String data = requestObject.getData();

            final RequestContext requestContext = RequestContextConverter.convert(servletRequest);

            if (auth != null && auth.getUserId() != null) {
                final String activationId = auth.getActivationContext().getActivationId();
                final String userId = auth.getUserId();
                final String applicationId = auth.getApplicationId();
                final PowerAuthCodeType signatureFactors = auth.getAuthenticationContext().getAuthenticationCodeType();
                final List<String> activationFlags = auth.getActivationContext().getActivationFlags();
                if (activationFlags.stream().anyMatch(DISALLOWED_FLAGS::contains)) {
                    logger.warn("Operation approval failed due to presence of a disallowed activation flag, operation ID: {}.", operationId, kv("operationId", operationId));
                    throw new MobileTokenAuthException();
                }
                final var serviceRequest = OperationApproveParameterObject.builder()
                        .activationId(activationId)
                        .userId(userId)
                        .applicationId(applicationId)
                        .operationId(operationId)
                        .data(data)
                        .signatureFactors(signatureFactors)
                        .requestContext(requestContext)
                        .activationFlags(activationFlags)
                        .proximityCheckOtp(fetchProximityCheckOtp(requestObject))
                        .mobileTokenData(requestObject.getMobileTokenData())
                        .build();

                final Response response = mobileTokenService.operationApprove(serviceRequest);
                logger.info("", action("operationApprove"), stateSucceeded());
                return response;
            } else {
                // make sure to fail operation as well, to increase the failed number
                mobileTokenService.operationFailApprove(operationId, requestContext);
                logger.debug("Operation approval failed due to failed user authentication, operation ID: {}.", operationId, kv("operationId", operationId));
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            final String errorCode = e.getPowerAuthError().map(PowerAuthError::getCode).orElse("ERROR_CODE_MISSING");
            switch (errorCode) {
                case APPLICATION_NOT_FOUND -> {
                    logger.warn("", action("operationApprove"), stateFailed(), kv("reason", "applicationNotFound"));
                    throw new MobileTokenException(ErrorCode.INVALID_APPLICATION, "No application was found with the provided identifier.", e);
                }
                case OPERATION_NOT_FOUND, OPERATION_APPROVE_FAILURE, OPERATION_INVALID_STATE -> {
                    logger.warn("", action("operationApprove"), stateFailed(), kv("reason", "operationNotFoundOrInvalidState"));
                    throw new MobileTokenException(ErrorCode.INVALID_OPERATION, "Operation not found or is in an unexpected state.", e);
                }
                case INVALID_REQUEST -> {
                    logger.warn("", action("operationApprove"), stateFailed(), kv("reason", "validation"));
                    throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Request validation error: %s".formatted(e.getMessage()), e);
                }
                default -> {
                    logger.warn("", action("operationApprove"), stateFailed(), kv("reason", "powerAuthConnection"));
                    throw new RemoteCommunicationException("Unable to call upstream service.", e);
                }
            }
        }
    }

    private static String fetchProximityCheckOtp(OperationApproveRequest requestObject) {
        if (requestObject.getProximityCheck().isEmpty()) {
            return null;
        }
        final var proximityCheck = requestObject.getProximityCheck().get();
        logger.info("Operation ID: {} using proximity check OTP, timestampReceived: {}, timestampSent: {}", requestObject.getId(), proximityCheck.getTimestampReceived(), proximityCheck.getTimestampSent(), kv("operationId", requestObject.getId()));
        return proximityCheck.getOtp();
    }

    /**
     * Operation reject.
     *
     * @param request Operation reject request.
     * @param auth Authentication object.
     * @param servletRequest HttpServletRequest instance.
     * @return Simple response object.
     * @throws MobileTokenException In the case error mobile token service occurs.
     */
    @PostMapping("/operation/cancel")
    @PowerAuth(resourceId = "/operation/cancel", authenticationCodeType = {
            PowerAuthCodeType.POSSESSION
    })
    public Response operationReject(
            @Valid @RequestBody final ObjectRequest<OperationRejectRequest> request,
            @Parameter(hidden = true) final PowerAuthApiAuthentication auth,
            final HttpServletRequest servletRequest) throws MobileTokenException, RemoteCommunicationException {

        final OperationRejectRequest requestObject = request.getRequestObject();
        final String operationId = requestObject.getId();
        logger.info("", action("operationReject"), stateInitiated(), kv("activationId", extractActivationId(auth)), kv("operationId", operationId));

        try {
            if (auth != null && auth.getUserId() != null) {
                final var result = mobileTokenService.operationReject(OperationRejectParameterObject.builder()
                        .activationId(auth.getActivationContext().getActivationId())
                        .applicationId(auth.getApplicationId())
                        .userId(auth.getUserId())
                        .activationFlags(auth.getActivationContext().getActivationFlags())
                        .operationId(operationId)
                        .rejectReason(requestObject.getReason())
                        .requestContext(RequestContextConverter.convert(servletRequest))
                        .mobileTokenData(requestObject.getMobileTokenData())
                        .build());
                logger.info("", action("operationReject"), stateSucceeded());
                return result;
            } else {
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            final String errorCode = e.getPowerAuthError().map(PowerAuthError::getCode).orElse("ERROR_CODE_MISSING");
            switch (errorCode) {
                case APPLICATION_NOT_FOUND -> {
                    logger.warn("", action("operationReject"), stateFailed(), kv("reason", "applicationNotFound"));
                    throw new MobileTokenException(ErrorCode.INVALID_APPLICATION, "No application was found with the provided identifier: %s".formatted(auth.getApplicationId()), e);
                }
                case OPERATION_NOT_FOUND, OPERATION_REJECT_FAILURE -> {
                    logger.warn("", action("operationReject"), stateFailed(), kv("reason", "operationNotFoundOrInvalidState"));
                    throw new MobileTokenException(ErrorCode.INVALID_OPERATION, "Operation not found or is in an unexpected state", e);
                }
                case INVALID_REQUEST -> {
                    logger.warn("", action("operationReject"), stateFailed(), kv("reason", "validation"));
                    throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Request validation error: %s".formatted(e.getMessage()), e);
                }
                default -> {
                    logger.warn("", action("operationReject"), stateFailed(), kv("reason", "powerAuthConnection"));
                    throw new RemoteCommunicationException("Unable to call upstream service.", e);
                }
            }
        }
    }

    private static void validateApiAuthentication(final PowerAuthApiAuthentication auth) throws MobileTokenAuthException {
        if (auth == null) {
            logger.warn("API authentication is missing");
            throw new MobileTokenAuthException();
        }
    }
}
