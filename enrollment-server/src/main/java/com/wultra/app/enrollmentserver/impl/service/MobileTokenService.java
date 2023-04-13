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
import com.wultra.security.powerauth.client.model.enumeration.OperationStatus;
import com.wultra.security.powerauth.client.model.enumeration.SignatureType;
import com.wultra.security.powerauth.client.model.enumeration.UserActionResult;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.OperationDetailRequest;
import com.wultra.security.powerauth.client.model.request.OperationFailApprovalRequest;
import com.wultra.security.powerauth.client.model.request.OperationListForUserRequest;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.client.model.response.OperationUserActionResponse;
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.response.OperationListResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Service responsible for mobile token features.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
public class MobileTokenService {

    private static final int OPERATION_LIST_LIMIT = 100;
    private static final String ATTR_ACTIVATION_ID = "activationId";
    private static final String ATTR_APPLICATION_ID = "applicationId";
    private static final String ATTR_IP_ADDRESS = "ipAddress";
    private static final String ATTR_USER_AGENT = "userAgent";
    private static final String ATTR_AUTH_FACTOR = "authFactor";
    private static final String ATTR_REJECT_REASON = "rejectReason";

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
        final MultiValueMap<String, String> queryParams = httpCustomizationService.getQueryParams();
        final MultiValueMap<String, String> httpHeaders = httpCustomizationService.getHttpHeaders();
        final com.wultra.security.powerauth.client.model.response.OperationListResponse pendingList =
                pendingOnly ?
                powerAuthClient.operationPendingList(request, queryParams, httpHeaders) :
                powerAuthClient.operationList(request, queryParams, httpHeaders);

        final OperationListResponse responseObject = new OperationListResponse();
        for (OperationDetailResponse operationDetail: pendingList) {
            final String activationFlag = operationDetail.getActivationFlag();
            if (activationFlag == null || activationFlags.contains(activationFlag)) { // only return data if there is no flag, or if flag matches flags of activation
                final OperationTemplateEntity operationTemplate = operationTemplateService.prepareTemplate(operationDetail.getOperationType(), language);
                final Operation operation = mobileTokenConverter.convert(operationDetail, operationTemplate);
                responseObject.add(operation);
                if (responseObject.size() >= OPERATION_LIST_LIMIT) { // limit the list size in response
                    break;
                }
            }
        }
        return responseObject;
    }

    /**
     * Approve an operation.
     *
     * @param activationId Activation ID.
     * @param userId User ID.
     * @param applicationId Application ID.
     * @param operationId Operation ID.
     * @param data Operation Data.
     * @param signatureFactors Used signature factors.
     * @param requestContext Request context.
     * @param activationFlags Activation flags.
     * @return Simple response.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public Response operationApprove(
            @NotNull String activationId,
            @NotNull String userId,
            @NotNull String applicationId,
            @NotNull String operationId,
            @NotNull String data,
            @NotNull PowerAuthSignatureTypes signatureFactors,
            @NotNull RequestContext requestContext,
            List<String> activationFlags) throws MobileTokenException, PowerAuthClientException {

        final OperationDetailResponse operationDetail = getOperationDetail(operationId);

        final String activationFlag = operationDetail.getActivationFlag();
        if (activationFlag != null && !activationFlags.contains(activationFlag)) { // allow approval if there is no flag, or if flag matches flags of activation
            throw new MobileTokenException("OPERATION_REQUIRES_ACTIVATION_FLAG", "Operation requires activation flag: " + activationFlag + ", which is not present on activation.");
        }

        final com.wultra.security.powerauth.client.model.request.OperationApproveRequest approveRequest = new com.wultra.security.powerauth.client.model.request.OperationApproveRequest();
        approveRequest.setOperationId(operationId);
        approveRequest.setData(data);
        approveRequest.setUserId(userId);
        approveRequest.setSignatureType(SignatureType.enumFromString(signatureFactors.name())); // 'toString' would perform additional toLowerCase() call
        approveRequest.setApplicationId(applicationId);
        // Prepare additional data
        approveRequest.getAdditionalData().put(ATTR_ACTIVATION_ID, activationId);
        approveRequest.getAdditionalData().put(ATTR_APPLICATION_ID, applicationId);
        approveRequest.getAdditionalData().put(ATTR_IP_ADDRESS, requestContext.getIpAddress());
        approveRequest.getAdditionalData().put(ATTR_USER_AGENT, requestContext.getUserAgent());
        approveRequest.getAdditionalData().put(ATTR_AUTH_FACTOR, signatureFactors.toString());
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
            handleStatus(operation.getStatus());
            throw new MobileTokenAuthException();
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
        handleStatus(operation.getStatus());
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
        final OperationDetailResponse operationDetail = getOperationDetail(operationId);

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
            handleStatus(operation.getStatus());
            throw new MobileTokenAuthException();
        }
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
    private OperationDetailResponse getOperationDetail(String operationId) throws PowerAuthClientException, MobileTokenException {
        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        final OperationDetailResponse operationDetail = powerAuthClient.operationDetail(
                operationDetailRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
        handleStatus(operationDetail.getStatus());
        return operationDetail;
    }

    /**
     * Handle operation status.
     *
     * <ul>
     *     <li>PENDING - noop</li>
     *     <li>CANCELLED, APPROVED, REJECTED, or EXPIRED - throws exception with appropriate code and message.</li>
     * </ul>
     *
     * @param status Operation status.
     * @throws MobileTokenException In case operation is in status that does not allow processing, the method throws appropriate exception.
     */
    private void handleStatus(OperationStatus status) throws MobileTokenException {
        switch (status) {
            case PENDING: {
                // OK, this operation is still pending
                break;
            }
            case CANCELED: {
                throw new MobileTokenException("OPERATION_ALREADY_CANCELED", "Operation was already canceled");
            }
            case APPROVED:
            case REJECTED: {
                throw new MobileTokenException("OPERATION_ALREADY_FINISHED", "Operation was already completed");
            }
            case FAILED: {
                throw new MobileTokenException("OPERATION_ALREADY_FAILED", "Operation already failed");
            }
            case EXPIRED:
            default: {
                throw new MobileTokenException("OPERATION_EXPIRED", "Operation already expired");
            }
        }
    }

}
