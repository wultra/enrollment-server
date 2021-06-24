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

import com.wultra.app.enrollmentserver.database.entity.OperationTemplate;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenAuthException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenException;
import com.wultra.app.enrollmentserver.impl.service.converter.MobileTokenConverter;
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
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import com.wultra.security.powerauth.lib.mtoken.model.entity.Operation;
import com.wultra.security.powerauth.lib.mtoken.model.response.OperationListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

/**
 * Service responsible for mobile token features.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
public class MobileTokenService {

    private static final int OPERATION_LIST_LIMIT = 100;

    private final PowerAuthClient powerAuthClient;
    private final MobileTokenConverter mobileTokenConverter;
    private final OperationTemplateService operationTemplateService;

    /**
     * Default constructor with autowired dependencies.
     *
     * @param powerAuthClient PowerAuth Client.
     * @param mobileTokenConverter Converter for mobile token objects.
     * @param operationTemplateService Operation template service.
     */
    @Autowired
    public MobileTokenService(PowerAuthClient powerAuthClient, MobileTokenConverter mobileTokenConverter, OperationTemplateService operationTemplateService) {
        this.powerAuthClient = powerAuthClient;
        this.mobileTokenConverter = mobileTokenConverter;
        this.operationTemplateService = operationTemplateService;
    }

    /**
     * Get the operation list with operations of a given users. The service either returns only pending
     * operations or all operations, depending on the provided flag.
     *
     * @param userId User ID.
     * @param applicationId Application ID.
     * @param language Language.
     * @param pendingOnly Flag indicating if only pending or all operation should be returned.
     * @return Response with pending or all operations, depending on the "pendingOnly" flag.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     * @throws MobileTokenConfigurationException In the case of system misconfiguration.
     */
    public OperationListResponse operationListForUser(
            @NotNull String userId,
            @NotNull Long applicationId,
            @NotNull String language,
            boolean pendingOnly) throws PowerAuthClientException, MobileTokenConfigurationException {

        final OperationListForUserRequest request = new OperationListForUserRequest();
        request.setUserId(userId);
        request.setApplicationId(applicationId);
        final com.wultra.security.powerauth.client.model.response.OperationListResponse pendingList = pendingOnly ?
                powerAuthClient.operationPendingList(request) : powerAuthClient.operationList(request);

        final OperationListResponse responseObject = new OperationListResponse();
        for (OperationDetailResponse operationDetail: pendingList) {
            final OperationTemplate operationTemplate = operationTemplateService.prepareTemplate(operationDetail.getOperationType(), language);
            final Operation operation = mobileTokenConverter.convert(operationDetail, operationTemplate);
            responseObject.add(operation);
            if (responseObject.size() >= OPERATION_LIST_LIMIT) { // limit the list size in response
                break;
            }
        }
        return responseObject;
    }

    /**
     * Approve an operation.
     *
     * @param userId User ID.
     * @param applicationId Application ID.
     * @param operationId Operation ID.
     * @param data Operation Data.
     * @param signatureFactors Used signature factors.
     * @return Simple response.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public Response operationApprove(
            @NotNull String userId,
            @NotNull Long applicationId,
            @NotNull String operationId,
            @NotNull String data,
            @NotNull PowerAuthSignatureTypes signatureFactors) throws MobileTokenException, PowerAuthClientException {

        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        final OperationDetailResponse operationDetailResponse = powerAuthClient.operationDetail(operationDetailRequest);
        OperationStatus status = operationDetailResponse.getStatus();
        handleStatus(status);

        final com.wultra.security.powerauth.client.model.request.OperationApproveRequest approveRequest = new com.wultra.security.powerauth.client.model.request.OperationApproveRequest();
        approveRequest.setOperationId(operationId);
        approveRequest.setData(data);
        approveRequest.setUserId(userId);
        approveRequest.setSignatureType(SignatureType.enumFromString(signatureFactors.name())); // 'toString' would perform additional toLowerCase() call
        approveRequest.setApplicationId(applicationId);
        final OperationUserActionResponse approveResponse = powerAuthClient.operationApprove(approveRequest);

        final UserActionResult result = approveResponse.getResult();
        if (result == UserActionResult.APPROVED) {
            return new Response();
        } else {
            final OperationDetailResponse operation = approveResponse.getOperation();
            status = operation.getStatus();
            handleStatus(status);
            throw new MobileTokenAuthException();
        }
    }

    /**
     * Fail operation approval (increase operation counter).
     *
     * @param operationId Operation ID.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public void operationFailApprove(String operationId) throws PowerAuthClientException, MobileTokenException {
        final OperationFailApprovalRequest request = new OperationFailApprovalRequest();
        request.setOperationId(operationId);
        final OperationUserActionResponse failApprovalResponse = powerAuthClient.failApprovalOperation(request);

        final OperationDetailResponse operation = failApprovalResponse.getOperation();
        handleStatus(operation.getStatus());
    }

    /**
     * Reject an operation.
     *
     * @param userId User ID.
     * @param applicationId Application ID.
     * @param operationId Operation ID.
     * @return Simple response.
     * @throws MobileTokenException In the case error mobile token service occurs.
     * @throws PowerAuthClientException In the case that PowerAuth service call fails.
     */
    public Response operationReject(
            @NotNull String userId,
            @NotNull Long applicationId,
            @NotNull String operationId) throws MobileTokenException, PowerAuthClientException {
        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        final OperationDetailResponse operationDetailResponse = powerAuthClient.operationDetail(operationDetailRequest);
        OperationStatus status = operationDetailResponse.getStatus();
        handleStatus(status);

        final com.wultra.security.powerauth.client.model.request.OperationRejectRequest rejectRequest = new com.wultra.security.powerauth.client.model.request.OperationRejectRequest();
        rejectRequest.setOperationId(operationId);
        rejectRequest.setUserId(userId);
        rejectRequest.setApplicationId(applicationId);

        final OperationUserActionResponse rejectResponse = powerAuthClient.operationReject(rejectRequest);

        final UserActionResult result = rejectResponse.getResult();
        if (result == UserActionResult.REJECTED) {
            return new Response();
        } else {
            final OperationDetailResponse operation = rejectResponse.getOperation();
            status = operation.getStatus();
            handleStatus(status);
            throw new MobileTokenAuthException();
        }
    }

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
