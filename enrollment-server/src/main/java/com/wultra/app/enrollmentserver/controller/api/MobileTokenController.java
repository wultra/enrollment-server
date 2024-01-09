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
import com.wultra.app.enrollmentserver.impl.service.MobileTokenService;
import com.wultra.app.enrollmentserver.impl.service.OperationApproveParameterObject;
import com.wultra.core.http.common.request.RequestContext;
import com.wultra.core.http.common.request.RequestContextConverter;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.request.OperationApproveRequest;
import com.wultra.security.powerauth.lib.mtoken.model.request.OperationDetailRequest;
import com.wultra.security.powerauth.lib.mtoken.model.request.OperationRejectRequest;
import com.wultra.security.powerauth.lib.mtoken.model.response.MobileTokenResponse;
import com.wultra.security.powerauth.lib.mtoken.model.response.OperationListResponse;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuth;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Locale;

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
public class MobileTokenController {

    private static final Logger logger = LoggerFactory.getLogger(MobileTokenController.class);

    // Disallowed flags contain onboarding flags used before onboarding process is finished
    private static final List<String> DISALLOWED_FLAGS = List.of("VERIFICATION_PENDING", "VERIFICATION_IN_PROGRESS");

    private final MobileTokenService mobileTokenService;

    /**
     * Default constructor with autowired dependencies.
     *
     * @param mobileTokenService Mobile token service.
     */
    @Autowired
    public MobileTokenController(MobileTokenService mobileTokenService) {
        this.mobileTokenService = mobileTokenService;
    }

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
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public ObjectResponse<OperationListResponse> operationList(@Parameter(hidden = true) PowerAuthApiAuthentication auth, @Parameter(hidden = true) Locale locale) throws MobileTokenException, MobileTokenConfigurationException {
        try {
            if (auth != null) {
                final String userId = auth.getUserId();
                final String applicationId = auth.getApplicationId();
                final String activationId = auth.getActivationContext().getActivationId();
                final String language = locale.getLanguage();
                final OperationListResponse listResponse = mobileTokenService.operationListForUser(userId, applicationId, language, activationId, true);
                final Date currentTimestamp = new Date();
                return new MobileTokenResponse<>(listResponse, currentTimestamp);
            } else {
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            logger.error("Unable to call upstream service.", e);
            throw new MobileTokenAuthException();
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
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public ObjectResponse<Operation> getOperationDetail(@RequestBody ObjectRequest<OperationDetailRequest> request,
                                                        @Parameter(hidden = true) PowerAuthApiAuthentication auth,
                                                        @Parameter(hidden = true) Locale locale) throws MobileTokenException, MobileTokenConfigurationException {
        try {
            if (auth != null) {
                final String operationId = request.getRequestObject().getId();
                final String language = locale.getLanguage();
                final String userId = auth.getUserId();
                final Operation response = mobileTokenService.getOperationDetail(operationId, language, userId);
                final Date currentTimestamp = new Date();
                return new MobileTokenResponse<>(response, currentTimestamp);
            } else {
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            logger.error("Unable to call upstream service.", e);
            throw new MobileTokenAuthException();
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
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public ObjectResponse<Operation> claimOperation(@RequestBody ObjectRequest<OperationDetailRequest> request,
                                                    @Parameter(hidden = true) PowerAuthApiAuthentication auth,
                                                    @Parameter(hidden = true) Locale locale) throws MobileTokenException, MobileTokenConfigurationException {
        try {
            if (auth != null) {
                final String operationId = request.getRequestObject().getId();
                final String language = locale.getLanguage();
                final String userId = auth.getUserId();
                final Operation response = mobileTokenService.claimOperation(operationId, language, userId);
                final Date currentTimestamp = new Date();
                return new MobileTokenResponse<>(response, currentTimestamp);
            } else {
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            logger.error("Unable to call upstream service.", e);
            throw new MobileTokenAuthException();
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
    @PowerAuth(resourceId = "/operation/history", signatureType = {
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<OperationListResponse> operationListAll(@Parameter(hidden = true) PowerAuthApiAuthentication auth, @Parameter(hidden = true) Locale locale) throws MobileTokenException, MobileTokenConfigurationException {
        try {
            if (auth != null) {
                final String userId = auth.getUserId();
                final String applicationId = auth.getApplicationId();
                final String activationId = auth.getActivationContext().getActivationId();
                final String language = locale.getLanguage();
                final OperationListResponse listResponse = mobileTokenService.operationListForUser(userId, applicationId, language, activationId, false);
                return new ObjectResponse<>(listResponse);
            } else {
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            logger.error("Unable to call upstream service.", e);
            throw new MobileTokenAuthException();
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
    @PowerAuth(resourceId = "/operation/authorize", signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY
    })
    public Response operationApprove(
            @RequestBody ObjectRequest<OperationApproveRequest> request,
            @Parameter(hidden = true) PowerAuthApiAuthentication auth,
            HttpServletRequest servletRequest) throws MobileTokenException {
        try {

            final OperationApproveRequest requestObject = request.getRequestObject();
            if (requestObject == null) {
                logger.warn("Request object is null when approving operation.");
                throw new MobileTokenAuthException();
            }

            final String operationId = requestObject.getId();
            final String data = requestObject.getData();
            if (operationId == null) {
                logger.warn("Operation ID is null when approving operation.");
                throw new MobileTokenAuthException();
            }

            final RequestContext requestContext = RequestContextConverter.convert(servletRequest);

            if (auth != null && auth.getUserId() != null) {
                final String activationId = auth.getActivationContext().getActivationId();
                final String userId = auth.getUserId();
                final String applicationId = auth.getApplicationId();
                final PowerAuthSignatureTypes signatureFactors = auth.getAuthenticationContext().getSignatureType();
                final List<String> activationFlags = auth.getActivationContext().getActivationFlags();
                if (activationFlags.stream().anyMatch(DISALLOWED_FLAGS::contains)) {
                    logger.warn("Operation approval failed due to presence of a disallowed activation flag, operation ID: {}.", operationId);
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
                        .build();

                return mobileTokenService.operationApprove(serviceRequest);
            } else {
                // make sure to fail operation as well, to increase the failed number
                mobileTokenService.operationFailApprove(operationId, requestContext);
                logger.debug("Operation approval failed due to failed user authentication, operation ID: {}.", operationId);
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            logger.error("Unable to call upstream service.", e);
            throw new MobileTokenAuthException();
        }
    }

    private static String fetchProximityCheckOtp(OperationApproveRequest requestObject) {
        if (requestObject.getProximityCheck().isEmpty()) {
            return null;
        }
        final var proximityCheck = requestObject.getProximityCheck().get();
        logger.info("Operation ID: {} using proximity check OTP, timestampReceived: {}, timestampSent: {}", requestObject.getId(), proximityCheck.getTimestampReceived(), proximityCheck.getTimestampSent());
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
    @PowerAuth(resourceId = "/operation/cancel", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response operationReject(
            @RequestBody ObjectRequest<OperationRejectRequest> request,
            @Parameter(hidden = true) PowerAuthApiAuthentication auth,
            HttpServletRequest servletRequest) throws MobileTokenException {
        try {

            final OperationRejectRequest requestObject = request.getRequestObject();
            if (requestObject == null) {
                throw new MobileTokenAuthException();
            }

            final RequestContext requestContext = RequestContextConverter.convert(servletRequest);

            if (auth != null && auth.getUserId() != null) {
                final String activationId = auth.getActivationContext().getActivationId();
                final String applicationId = auth.getApplicationId();
                final String userId = auth.getUserId();
                final List<String> activationFlags = auth.getActivationContext().getActivationFlags();
                final String operationId = requestObject.getId();
                final String rejectReason = requestObject.getReason();
                return mobileTokenService.operationReject(activationId, userId, applicationId, operationId, requestContext, activationFlags, rejectReason);
            } else {
                throw new MobileTokenAuthException();
            }
        } catch (PowerAuthClientException e) {
            logger.error("Unable to call upstream service.", e);
            throw new MobileTokenAuthException();
        }
    }

}
