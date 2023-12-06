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

import com.wultra.app.enrollmentserver.database.entity.OperationTemplateEntity;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenAuthException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenException;
import com.wultra.app.enrollmentserver.impl.service.converter.MobileTokenConverter;
import com.wultra.core.http.common.request.RequestContext;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.enumeration.SignatureType;
import com.wultra.security.powerauth.client.model.enumeration.UserActionResult;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.OperationDetailRequest;
import com.wultra.security.powerauth.client.model.request.OperationFailApprovalRequest;
import com.wultra.security.powerauth.client.model.request.OperationListForUserRequest;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.client.model.response.OperationUserActionResponse;
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.enumeration.ErrorCode;
import com.wultra.security.powerauth.lib.mtoken.model.response.OperationListResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for mobile token features.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
@Slf4j
public class MobileTokenService {

    private static final int OPERATION_LIST_LIMIT = 100;
    private static final String ATTR_ACTIVATION_ID = "activationId";
    private static final String ATTR_APPLICATION_ID = "applicationId";
    private static final String ATTR_IP_ADDRESS = "ipAddress";
    private static final String ATTR_USER_AGENT = "userAgent";
    private static final String ATTR_AUTH_FACTOR = "authFactor";
    private static final String ATTR_REJECT_REASON = "rejectReason";
    private static final String PROXIMITY_OTP = "proximity_otp";

    private final PowerAuthClient powerAuthClient;
    private final MobileTokenConverter mobileTokenConverter;
    private final OperationTemplateService operationTemplateService;
    private final HttpCustomizationService httpCustomizationService;

    /**
     * Default constructor with autowired dependencies.
     *
     * @param powerAuthClient PowerAuth Client.
     * @param mobileTokenConverter Converter for mobile token objects.
     * @param operationTemplateService Operation template service.
     * @param httpCustomizationService HTTP customization service.
     */
    @Autowired
    public MobileTokenService(PowerAuthClient powerAuthClient, MobileTokenConverter mobileTokenConverter, OperationTemplateService operationTemplateService, HttpCustomizationService httpCustomizationService) {
        this.powerAuthClient = powerAuthClient;
        this.mobileTokenConverter = mobileTokenConverter;
        this.operationTemplateService = operationTemplateService;
        this.httpCustomizationService = httpCustomizationService;
    }

    /**
     * Get the operation list with operations of a given users. The service either returns only pending
     * operations or all operations, depending on the provided flag.
     *
     * @param userId User ID.
     * @param applicationId Application ID.
     * @param language Language.
     * @param activationFlags Activation flags to condition the operation against.
     * @param pendingOnly Flag indicating if only pending or all operation should be returned.
     * @return Response with pending or all operations, depending on the "pendingOnly" flag.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     * @throws MobileTokenConfigurationException In the case of system misconfiguration.
     */
    public OperationListResponse operationListForUser(
            @NotNull String userId,
            @NotNull String applicationId,
            @NotNull String language,
            List<String> activationFlags,
            boolean pendingOnly) throws PowerAuthClientException, MobileTokenConfigurationException {

        final OperationListForUserRequest request = new OperationListForUserRequest();
        request.setUserId(userId);
        request.setApplications(List.of(applicationId));
        request.setPageNumber(0);
        request.setPageSize(OPERATION_LIST_LIMIT);
        final MultiValueMap<String, String> queryParams = httpCustomizationService.getQueryParams();
        final MultiValueMap<String, String> httpHeaders = httpCustomizationService.getHttpHeaders();
        final com.wultra.security.powerauth.client.model.response.OperationListResponse operations =
                pendingOnly ?
                powerAuthClient.operationPendingList(request, queryParams, httpHeaders) :
                powerAuthClient.operationList(request, queryParams, httpHeaders);

        final OperationListResponse responseObject = new OperationListResponse();
        for (OperationDetailResponse operationDetail: operations) {
            final String activationFlag = operationDetail.getActivationFlag();
            if (activationFlag == null || activationFlags.contains(activationFlag)) { // only return data if there is no flag, or if flag matches flags of activation
                final Optional<OperationTemplateEntity> operationTemplate = operationTemplateService.findTemplate(operationDetail.getOperationType(), language);
                if (operationTemplate.isEmpty()) {
                    logger.warn("No template found for operationType={}, skipping the entry.", operationDetail.getOperationType());
                    continue;
                }
                final Operation operation = mobileTokenConverter.convert(operationDetail, operationTemplate.get());
                responseObject.add(operation);
            }
        }
        return responseObject;
    }

    /**
     * Approve an operation.
     *
     * @param request request
     * @return Simple response.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public Response operationApprove(@NotNull final OperationApproveParameterObject request) throws MobileTokenException, PowerAuthClientException {

        final OperationDetailResponse operationDetail = claimOperationInternal(request.getOperationId(), null);

        final String activationFlag = operationDetail.getActivationFlag();
        if (activationFlag != null && !request.getActivationFlags().contains(activationFlag)) { // allow approval if there is no flag, or if flag matches flags of activation
            throw new MobileTokenException("OPERATION_REQUIRES_ACTIVATION_FLAG", "Operation requires activation flag: " + activationFlag + ", which is not present on activation.");
        }

        final com.wultra.security.powerauth.client.model.request.OperationApproveRequest approveRequest = new com.wultra.security.powerauth.client.model.request.OperationApproveRequest();
        approveRequest.setOperationId(request.getOperationId());
        approveRequest.setData(request.getData());
        approveRequest.setUserId(request.getUserId());
        approveRequest.setSignatureType(SignatureType.enumFromString(request.getSignatureFactors().name())); // 'toString' would perform additional toLowerCase() call
        approveRequest.setApplicationId(request.getApplicationId());
        // Prepare additional data
        approveRequest.getAdditionalData().put(ATTR_ACTIVATION_ID, request.getActivationId());
        approveRequest.getAdditionalData().put(ATTR_APPLICATION_ID, request.getApplicationId());
        approveRequest.getAdditionalData().put(ATTR_IP_ADDRESS, request.getRequestContext().getIpAddress());
        approveRequest.getAdditionalData().put(ATTR_USER_AGENT, request.getRequestContext().getUserAgent());
        approveRequest.getAdditionalData().put(ATTR_AUTH_FACTOR, request.getSignatureFactors().toString());

        if (request.getProximityCheckOtp() != null) {
            approveRequest.getAdditionalData().put(PROXIMITY_OTP, request.getProximityCheckOtp());
        }

        final OperationUserActionResponse approveResponse = powerAuthClient.operationApprove(
                approveRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );

        final UserActionResult result = approveResponse.getResult();
        if (result == UserActionResult.APPROVED) {
            return new Response();
        } else {
            final OperationDetailResponse operation = approveResponse.getOperation();
            handleStatus(operation);
            throw new MobileTokenAuthException(ErrorCode.OPERATION_FAILED, "PowerAuth server operation approval fails");
        }
    }

    /**
     * Fail operation approval (increase operation counter).
     *
     * @param operationId Operation ID.
     * @param requestContext Request context.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public void operationFailApprove(@NotNull String operationId, @NotNull RequestContext requestContext) throws PowerAuthClientException, MobileTokenException {
        final OperationFailApprovalRequest request = new OperationFailApprovalRequest();
        request.setOperationId(operationId);
        // Prepare additional data
        request.getAdditionalData().put(ATTR_IP_ADDRESS, requestContext.getIpAddress());
        request.getAdditionalData().put(ATTR_USER_AGENT, requestContext.getUserAgent());

        final OperationUserActionResponse failApprovalResponse = powerAuthClient.failApprovalOperation(
                request,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );

        final OperationDetailResponse operation = failApprovalResponse.getOperation();
        handleStatus(operation);
    }

    /**
     * Reject an operation.
     *
     * @param activationId Activation ID.
     * @param userId User ID.
     * @param applicationId Application ID.
     * @param operationId Operation ID.
     * @param requestContext Request context.
     * @param activationFlags Activation flags.
     * @param rejectReason Reason for operation rejection.
     * @return Simple response.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public Response operationReject(
            @NotNull String activationId,
            @NotNull String userId,
            @NotNull String applicationId,
            @NotNull String operationId,
            @NotNull RequestContext requestContext,
            List<String> activationFlags,
            String rejectReason) throws MobileTokenException, PowerAuthClientException {
        final OperationDetailResponse operationDetail = getOperationDetailInternal(operationId);

        final String activationFlag = operationDetail.getActivationFlag();
        if (activationFlag != null && !activationFlags.contains(activationFlag)) { // allow approval if there is no flag, or if flag matches flags of activation
            throw new MobileTokenException("OPERATION_REQUIRES_ACTIVATION_FLAG", "Operation requires activation flag: " + activationFlag + ", which is not present on activation.");
        }

        final com.wultra.security.powerauth.client.model.request.OperationRejectRequest rejectRequest = new com.wultra.security.powerauth.client.model.request.OperationRejectRequest();
        rejectRequest.setOperationId(operationId);
        rejectRequest.setUserId(userId);
        rejectRequest.setApplicationId(applicationId);
        // Prepare additional data
        rejectRequest.getAdditionalData().put(ATTR_ACTIVATION_ID, activationId);
        rejectRequest.getAdditionalData().put(ATTR_APPLICATION_ID, applicationId);
        rejectRequest.getAdditionalData().put(ATTR_IP_ADDRESS, requestContext.getIpAddress());
        rejectRequest.getAdditionalData().put(ATTR_USER_AGENT, requestContext.getUserAgent());
        rejectRequest.getAdditionalData().put(ATTR_REJECT_REASON, rejectReason);

        final OperationUserActionResponse rejectResponse = powerAuthClient.operationReject(
                rejectRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );

        final UserActionResult result = rejectResponse.getResult();
        if (result == UserActionResult.REJECTED) {
            return new Response();
        } else {
            final OperationDetailResponse operation = rejectResponse.getOperation();
            handleStatus(operation);
            throw new MobileTokenAuthException(ErrorCode.OPERATION_FAILED, "PowerAuth server operation rejection fails");
        }
    }

    /**
     * Get operation detail.
     *
     * @param operationId Operation ID.
     * @param language Language.
     * @param userId User identifier.
     * @return Operation detail.
     * @throws PowerAuthClientException In case communication with PowerAuth Server fails.
     * @throws MobileTokenException In case the operation is in incorrect state.
     * @throws MobileTokenConfigurationException In case operation template is not configured correctly.
     */
    public Operation getOperationDetail(String operationId, String language, String userId) throws MobileTokenException, PowerAuthClientException, MobileTokenConfigurationException {
        final OperationDetailResponse operationDetail = getOperationDetailInternal(operationId);
        // Check user ID against authenticated user, however skip the check in case operation is not claimed yet
        if (operationDetail.getUserId() != null && !userId.equals(operationDetail.getUserId())) {
            logger.warn("User ID from operation does not match authenticated user ID.");
            throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Invalid request");
        }
        return convertOperation(language, operationDetail);
    }

    /**
     * Claim operation.
     *
     * @param operationId Operation ID.
     * @param language Language.
     * @param userId User identifier.
     * @return Operation detail.
     * @throws PowerAuthClientException In case communication with PowerAuth Server fails.
     * @throws MobileTokenException In case the operation is in incorrect state.
     * @throws MobileTokenConfigurationException In case operation template is not configured correctly.
     */
    public Operation claimOperation(String operationId, String language, String userId) throws MobileTokenException, PowerAuthClientException, MobileTokenConfigurationException {
        final OperationDetailResponse operationDetail = claimOperationInternal(operationId, userId);
        return convertOperation(language, operationDetail);
    }

    // Private methods

    /**
     * Get operation detail by calling PowerAuth Server.
     *
     * @param operationId Operation ID.
     * @return Operation detail.
     * @throws PowerAuthClientException In case communication with PowerAuth Server fails.
     * @throws MobileTokenException When the operation is in incorrect state.
     */
    private OperationDetailResponse getOperationDetailInternal(String operationId) throws PowerAuthClientException, MobileTokenException {
        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        final OperationDetailResponse operationDetail = powerAuthClient.operationDetail(
                operationDetailRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
        handleStatus(operationDetail);
        return operationDetail;
    }

    /**
     * Get operation detail by calling PowerAuth Server.
     *
     * @param operationId Operation ID.
     * @param userId Optional user ID for operation claim.
     * @return Operation detail.
     * @throws PowerAuthClientException In case communication with PowerAuth Server fails.
     * @throws MobileTokenException When the operation is in incorrect state.
     */
    private OperationDetailResponse claimOperationInternal(String operationId, String userId) throws PowerAuthClientException, MobileTokenException {
        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        operationDetailRequest.setUserId(userId);
        final OperationDetailResponse operationDetail = powerAuthClient.operationDetail(
                operationDetailRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
        handleStatus(operationDetail);
        return operationDetail;
    }

    /**
     * Find operation template and convert the operation.
     *
     * @param language Language.
     * @param operationDetail Operation detail.
     * @return Converted operation.
     * @throws MobileTokenException In case the operation is in incorrect state.
     * @throws MobileTokenConfigurationException In case operation template is not configured correctly.
     */
    private Operation convertOperation(String language, OperationDetailResponse operationDetail) throws MobileTokenException, MobileTokenConfigurationException {
        final Optional<OperationTemplateEntity> operationTemplate = operationTemplateService.findTemplate(operationDetail.getOperationType(), language);
        if (operationTemplate.isEmpty()) {
            logger.warn("Template not found for operationType={}.", operationDetail.getOperationType());
            throw new MobileTokenException(ErrorCode.INVALID_REQUEST, "Template not found");
        }
        return mobileTokenConverter.convert(operationDetail, operationTemplate.get());
    }

    /**
     * Handle operation status.
     *
     * <ul>
     *     <li>PENDING - noop</li>
     *     <li>CANCELLED, APPROVED, REJECTED, or EXPIRED - throws exception with appropriate code and message.</li>
     * </ul>
     *
     * @param operation Operation detail.
     * @throws MobileTokenException In case operation is in status that does not allow processing, the method throws appropriate exception.
     */
    private static void handleStatus(final OperationDetailResponse operation) throws MobileTokenException {
        switch (operation.getStatus()) {
            case PENDING ->
                    logger.debug("OK, operation ID: {} is still pending", operation.getId());
            case CANCELED ->
                    throw new MobileTokenException(ErrorCode.OPERATION_ALREADY_CANCELED, "Operation was already canceled");
            case APPROVED, REJECTED ->
                    throw new MobileTokenException(ErrorCode.OPERATION_ALREADY_FINISHED, "Operation was already completed");
            case FAILED ->
                    throw new MobileTokenException(ErrorCode.OPERATION_ALREADY_FAILED, "Operation already failed");
            default ->
                    throw new MobileTokenException(ErrorCode.OPERATION_EXPIRED, "Operation already expired");
        }
    }

}
