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
import io.getlime.push.model.base.PagedResponse;
import io.getlime.push.model.entity.InboxMessage;
import io.getlime.push.model.entity.ListOfInboxMessages;
import io.getlime.push.model.response.GetInboxMessageCountResponse;
import io.getlime.push.model.response.GetInboxMessageDetailResponse;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

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
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public ObjectResponse<GetInboxCountResponse> countUnreadMessages(@Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException, PowerAuthAuthenticationException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final GetInboxCountResponse response = new GetInboxCountResponse();
        try {
            final ObjectResponse<GetInboxMessageCountResponse> pushResponse = pushClient.fetchMessageCountForUser(userId, appId);
            response.setCountUnread(pushResponse.getResponseObject().getCountUnread());
            return new ObjectResponse<>(response);
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("message/list")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public ObjectResponse<GetInboxListResponse> fetchMessageList(@RequestBody ObjectRequest<GetInboxListRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException, PowerAuthAuthenticationException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final GetInboxListRequest request = objectRequest.getRequestObject();
        final boolean onlyUnread = request.isOnlyUnread();
        final int page = request.getPage();
        final int size = request.getSize();
        try {
            final ObjectResponse<ListOfInboxMessages> pushResponse = pushClient.fetchMessageListForUser(
                    userId, Collections.singletonList(appId), onlyUnread, page, size);
            final GetInboxListResponse response = new GetInboxListResponse();
            pushResponse.getResponseObject().forEach(message -> response.add(convertMessageInList(message)));
            return new PagedResponse<>(response, page, size);
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("message/detail")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public ObjectResponse<GetInboxDetailResponse> fetchMessageDetail(@RequestBody ObjectRequest<GetInboxDetailRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException, PowerAuthAuthenticationException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final GetInboxDetailRequest request = objectRequest.getRequestObject();
        try {
            final GetInboxMessageDetailResponse messageDetail = fetchInboxMessageDetail(userId, appId, request.getId());
            final GetInboxDetailResponse response = convertMessageDetail(messageDetail);
            return new ObjectResponse<>(response);
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("message/read")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public Response readMessage(@RequestBody ObjectRequest<InboxReadRequest> objectRequest, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException, PowerAuthAuthenticationException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        final InboxReadRequest request = objectRequest.getRequestObject();
        try {
            final GetInboxMessageDetailResponse messageDetail = fetchInboxMessageDetail(userId, appId, request.getId());
            if (!messageDetail.isRead()) {
                pushClient.readMessage(request.getId());
            }
            return new Response();
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("message/read-all")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE_BIOMETRY
    })
    public Response readAllMessages(@Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws InboxException, PowerAuthAuthenticationException {
        checkApiAuthentication(apiAuthentication);
        final String userId = apiAuthentication.getUserId();
        final String appId = apiAuthentication.getApplicationId();
        try {
            pushClient.readAllMessages(userId, appId);
            return new Response();
        } catch (PushServerClientException ex) {
            logger.debug(ex.getMessage(), ex);
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
    }

    // Private methods

    private void checkApiAuthentication(final PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException {
        if (apiAuthentication == null || apiAuthentication.getUserId() == null || apiAuthentication.getApplicationId() == null) {
            logger.debug("API authentication: {}", apiAuthentication);
            throw new PowerAuthAuthenticationException("Inbox request authentication failed");
        }
    }

    private GetInboxListResponse.InboxMessage convertMessageInList(final InboxMessage message) {
        final GetInboxListResponse.InboxMessage result = new GetInboxListResponse.InboxMessage();
        result.setId(message.getId());
        result.setType(message.getType().toLowerCaseString());
        result.setSubject(message.getSubject());
        result.setSummary(message.getSummary());
        result.setRead(message.isRead());
        result.setTimestampCreated(message.getTimestampCreated());
        return result;
    }

    private GetInboxDetailResponse convertMessageDetail(final GetInboxMessageDetailResponse message) {
        final GetInboxDetailResponse result = new GetInboxDetailResponse();
        result.setId(message.getId());
        result.setType(message.getType().toLowerCaseString());
        result.setSubject(message.getSubject());
        result.setSummary(message.getSummary());
        result.setBody(message.getBody());
        result.setRead(message.isRead());
        result.setTimestampCreated(message.getTimestampCreated());
        return result;
    }

    private GetInboxMessageDetailResponse fetchInboxMessageDetail(String userId, String appId, String inboxId) throws PushServerClientException, InboxException {
        final ObjectResponse<GetInboxMessageDetailResponse> objectResponse = pushClient.fetchMessageDetail(inboxId);
        final GetInboxMessageDetailResponse messageDetail = objectResponse.getResponseObject();
        if (!userId.equals(messageDetail.getUserId()) || messageDetail.getApplications() == null || !messageDetail.getApplications().contains(appId)) {
            throw new InboxException("Unable to access inbox message detail.");
        }
        return messageDetail;
    }

}
