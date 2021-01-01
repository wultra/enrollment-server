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
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenConfigurationException;
import com.wultra.app.enrollmentserver.errorhandling.MobileTokenException;
import com.wultra.app.enrollmentserver.impl.service.converter.MobileTokenConverter;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.enumeration.UserActionResult;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.OperationDetailRequest;
import com.wultra.security.powerauth.client.model.request.OperationListForUserRequest;
import com.wultra.security.powerauth.client.model.response.OperationDetailResponse;
import com.wultra.security.powerauth.client.model.response.OperationUserActionResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.lib.mtoken.model.entity.Operation;
import io.getlime.security.powerauth.lib.mtoken.model.response.OperationListResponse;
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

    private final PowerAuthClient powerAuthClient;
    private final MobileTokenConverter mobileTokenConverter;
    private final OperationTemplateService operationTemplateService;

    @Autowired
    public MobileTokenService(PowerAuthClient powerAuthClient, MobileTokenConverter mobileTokenConverter, OperationTemplateService operationTemplateService) {
        this.powerAuthClient = powerAuthClient;
        this.mobileTokenConverter = mobileTokenConverter;
        this.operationTemplateService = operationTemplateService;
    }

    public OperationListResponse operationListForUser(
            @NotNull String userId,
            @NotNull Long applicationId,
            @NotNull String language) throws PowerAuthClientException, MobileTokenConfigurationException {

        final OperationListForUserRequest request = new OperationListForUserRequest();
        request.setUserId(userId);
        request.setApplicationId(applicationId);
        final com.wultra.security.powerauth.client.model.response.OperationListResponse pendingList = powerAuthClient.operationPendingList(request);

        final OperationListResponse responseObject = new OperationListResponse();
        for (OperationDetailResponse operationDetail: pendingList) {
            final OperationTemplate operationTemplate = operationTemplateService.prepareTemplate(operationDetail.getOperationType(), language);
            final Operation operation = mobileTokenConverter.convert(operationDetail, operationTemplate);
            responseObject.add(operation);
        }
        return responseObject;
    }

    public Response operationApprove(
            @NotNull String userId,
            @NotNull Long applicationId,
            @NotNull String operationId,
            @NotNull String data,
            @NotNull String signatureFactors) throws MobileTokenException, PowerAuthClientException {

        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        final OperationDetailResponse operationDetailResponse = powerAuthClient.operationDetail(operationDetailRequest);
        String status = operationDetailResponse.getStatus();
        handleStatus(status);

        final com.wultra.security.powerauth.client.model.request.OperationApproveRequest approveRequest = new com.wultra.security.powerauth.client.model.request.OperationApproveRequest();
        approveRequest.setOperationId(operationId);
        approveRequest.setData(data);
        approveRequest.setUserId(userId);
        approveRequest.setSignatureType(signatureFactors);
        approveRequest.setApplicationId(applicationId);
        final OperationUserActionResponse approveResponse = powerAuthClient.operationApprove(approveRequest);

        final UserActionResult result = approveResponse.getResult();
        if (result == UserActionResult.APPROVED) {
            return new Response();
        } else {
            final OperationDetailResponse operation = approveResponse.getOperation();
            status = operation.getStatus();
            handleStatus(status);
        }
        throw new MobileTokenException("POWERAUTH_AUTH_FAIL", "Authentication failed");
    }

    public Response operationReject(
            @NotNull String userId,
            @NotNull Long applicationId,
            @NotNull String operationId) throws MobileTokenException, PowerAuthClientException {
        final OperationDetailRequest operationDetailRequest = new OperationDetailRequest();
        operationDetailRequest.setOperationId(operationId);
        final OperationDetailResponse operationDetailResponse = powerAuthClient.operationDetail(operationDetailRequest);
        String status = operationDetailResponse.getStatus();
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
        }
        throw new MobileTokenException("POWERAUTH_AUTH_FAIL", "Authentication failed");
    }

    private void handleStatus(String status) throws MobileTokenException {
        switch (status) {
            case "PENDING": {
                // OK, this operation is still pending
                break;
            }
            case "CANCELLED": {
                throw new MobileTokenException("OPERATION_ALREADY_CANCELED", "Operation was already cancelled");
            }
            case "EXPIRED": {
                throw new MobileTokenException("OPERATION_EXPIRED", "Operation already expired");
            }
            case "APPROVED":
            case "REJECTED": {
                throw new MobileTokenException("OPERATION_ALREADY_FINISHED", "Operation was already completed");
            }
            case "FAILED": {
                throw new MobileTokenException("OPERATION_ALREADY_FAILED", "Operation already failed");
            }
        }
    }

}
