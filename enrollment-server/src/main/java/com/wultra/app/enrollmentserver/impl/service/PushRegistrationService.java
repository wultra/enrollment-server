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

import com.wultra.app.enrollmentserver.api.model.enrollment.request.PushRegisterRequest;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.errorhandling.PushRegistrationFailedException;
import com.wultra.app.enrollmentserver.model.validator.PushRegisterRequestValidator;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.push.client.PushServerClient;
import io.getlime.push.client.PushServerClientException;
import io.getlime.push.model.enumeration.MobilePlatform;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for push token registration.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Service
public class PushRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(PushRegistrationService.class);

    private final PushServerClient client;

    @Autowired
    public PushRegistrationService(PushServerClient client) {
        this.client = client;
    }

    public Response registerDevice(
            @NotNull ObjectRequest<PushRegisterRequest> request,
            String userId,
            String activationId,
            String applicationId) throws InvalidRequestObjectException, PushRegistrationFailedException {

        logger.info("Push registration started, user ID: {}", userId);

        // Verify that userId, applicationId and activationId are set
        if (userId == null || applicationId == null || activationId == null) {
            logger.error("Missing an attribute required for push registration, user ID: {}", userId);
            throw new PushRegistrationFailedException();
        }

        // Check the request body presence
        final PushRegisterRequest requestObject = request.getRequestObject();
        final String error = PushRegisterRequestValidator.validate(requestObject);
        if (error != null) {
            logger.error("Invalid request object in push registration - {}, user ID: {}", error, userId);
            throw new InvalidRequestObjectException();
        }

        final MobilePlatform platform = convert(requestObject.getPlatform());
        final String token = requestObject.getToken();

        try {
            final boolean result = client.createDevice(applicationId, token, platform, activationId);
            if (result) {
                logger.info("Push registration succeeded, user ID: {}", userId);
                return new Response();
            } else {
                logger.warn("Push registration failed, user ID: {}", userId);
                throw new PushRegistrationFailedException();
            }
        } catch (PushServerClientException ex) {
            logger.error("Push registration failed", ex);
            throw new PushRegistrationFailedException();
        }
    }

    private static MobilePlatform convert(final PushRegisterRequest.Platform source) {
        return switch (source) {
            case IOS -> MobilePlatform.IOS;
            case ANDROID -> MobilePlatform.ANDROID;
        };
    }

}
