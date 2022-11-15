/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.api.model.enrollment.request.*;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.GetInboxCountResponse;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.GetInboxDetailResponse;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.GetInboxListResponse;
import com.wultra.app.enrollmentserver.errorhandling.InboxException;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.push.client.PushServerClient;
import io.getlime.push.client.PushServerClientException;
import io.getlime.push.model.entity.InboxMessage;
import io.getlime.push.model.entity.ListOfInboxMessages;
import io.getlime.push.model.response.GetInboxMessageCountResponse;
import io.getlime.push.model.response.GetInboxMessageDetailResponse;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller with facade for Inbox services in Push server.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ConditionalOnExpression("!'${powerauth.push.service.url}'.empty and ${enrollment-server.inbox.enabled}")
@RestController
@RequestMapping(value = "api/inbox")
@Slf4j
public class InboxController {

    private final PushServerClient pushClient;

    @Autowired
    public InboxController(PushServerClient pushClient) {
        this.pushClient = pushClient;
    }

    @PostMapping("count")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<GetInboxCountResponse> countUnreadMessages(ObjectRequest<GetInboxCountRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final GetInboxCountResponse response = new GetInboxCountResponse();
        try {
            final ObjectResponse<GetInboxMessageCountResponse> pushResponse = pushClient.fetchMessageCountForUser(userId, appId);
            response.setCountUnread(pushResponse.getResponseObject().getCountUnread());
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
        return new ObjectResponse<>(response);
    }

    @PostMapping("message/list")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<GetInboxListResponse> fetchMessageList(ObjectRequest<GetInboxListRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final GetInboxListRequest request = objectRequest.getRequestObject();
        final GetInboxListResponse response = new GetInboxListResponse();
        try {
            final ObjectResponse<ListOfInboxMessages> pushResponse = pushClient.fetchMessageListForUser(
                    userId, appId, request.getPage(), request.getSize());
            pushResponse.getResponseObject().forEach(message -> response.add(convertMessageInList(message)));
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
        return new ObjectResponse<>(response);
    }

    @PostMapping("message/detail")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<GetInboxDetailResponse> fetchMessageDetail(ObjectRequest<GetInboxDetailRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final GetInboxDetailRequest request = objectRequest.getRequestObject();
        GetInboxDetailResponse response;
        try {
            final ObjectResponse<GetInboxMessageDetailResponse> pushResponse = pushClient.fetchMessageDetail(
                    userId, appId, request.getId());
            final GetInboxMessageDetailResponse inbox = pushResponse.getResponseObject();
            response = convertMessageDetail(inbox);
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
        return new ObjectResponse<>(response);
    }

    @PostMapping("message/read")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response readMessage(ObjectRequest<InboxReadRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final InboxReadRequest request = objectRequest.getRequestObject();
        try {
            pushClient.readMessage(userId, appId, request.getId());
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
        return new Response();
    }

    @PostMapping("message/read-all")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response readAllMessages(ObjectRequest<InboxReadAllRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        try {
            pushClient.readAllMessages(userId, appId);
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
        return new Response();
    }

    private void checkApiAuthentication(final PowerAuthApiAuthentication apiAuthentication) throws InboxException {
        if (apiAuthentication == null || apiAuthentication.getUserId() == null || apiAuthentication.getApplicationId() == null) {
            logger.debug("API authentication: {}", apiAuthentication);
            throw new InboxException("Inbox request authentication failed");
        }
    }

    private GetInboxListResponse.InboxMessage convertMessageInList(final InboxMessage message) {
        final GetInboxListResponse.InboxMessage result = new GetInboxListResponse.InboxMessage();
        result.setId(message.getId());
        result.setSubject(message.getSubject());
        result.setRead(message.isRead());
        result.setTimestampCreated(message.getTimestampCreated());
        return result;
    }

    private GetInboxDetailResponse convertMessageDetail(final GetInboxMessageDetailResponse message) {
        final GetInboxDetailResponse result = new GetInboxDetailResponse();
        result.setId(message.getId());
        result.setSubject(message.getSubject());
        result.setBody(message.getBody());
        result.setRead(message.isRead());
        result.setTimestampCreated(message.getTimestampCreated());
        return result;
    }
}
