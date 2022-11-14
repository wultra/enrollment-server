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
import com.wultra.app.enrollmentserver.api.model.enrollment.response.GetInboxListResponse;
import com.wultra.app.enrollmentserver.errorhandling.InboxException;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.push.client.PushServerClient;
import io.getlime.push.client.PushServerClientException;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller with facade for Inbox services in Push server.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ConditionalOnExpression("!'${powerauth.push.service.url}'.empty and ${enrollment-server.inbox.enabled}")
@RestController
@RequestMapping(value = "api/inbox")
public class InboxController {

    private static final Logger logger = LoggerFactory.getLogger(InboxController.class);

    private final PushServerClient pushClient;

    @Autowired
    public InboxController(PushServerClient pushClient) {
        this.pushClient = pushClient;
    }

    @PostMapping("count")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<GetInboxCountResponse> countUnreadMessages(ObjectRequest<GetInboxCountRequest> objectRequest) throws InboxException {
        final GetInboxCountRequest request = objectRequest.getRequestObject();
        final GetInboxCountResponse response = new GetInboxCountResponse();
        try {
            final ObjectResponse<io.getlime.push.model.response.GetInboxMessageCountResponse> pushResponse = pushClient.fetchMessageCountForUser(request.getUserId(), request.getAppId());
            response.setCountUnread(pushResponse.getResponseObject().getCountUnread());
        } catch (PushServerClientException ex) {
            throw new InboxException("Push server REST API call failed, error: " + ex.getMessage(), ex);
        }
        return new ObjectResponse<>(response);
    }

    @PostMapping("message/list")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<GetInboxListResponse> fetchMessageList(ObjectRequest<GetInboxListRequest> objectRequest) throws InboxException {
        return new ObjectResponse<>(new GetInboxListResponse());
    }

    @PostMapping("message/detail")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<GetInboxListResponse> fetchMessageDetail(ObjectRequest<GetInboxDetailRequest> objectRequest) throws InboxException {
        return new ObjectResponse<>(new GetInboxListResponse());
    }

    @PostMapping("message/read")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response readMessage(ObjectRequest<InboxReadRequest> objectRequest) throws InboxException {
        return new Response();
    }

    @PostMapping("message/read-all")
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response readAllMessages(ObjectRequest<InboxReadAllRequest> objectRequest) throws InboxException {
        return new Response();
    }

}
