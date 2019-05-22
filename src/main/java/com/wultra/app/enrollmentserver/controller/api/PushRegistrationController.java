/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2019 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.request.PushRegisterRequest;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.errorhandling.PushRegistrationFailedException;
import com.wultra.app.enrollmentserver.model.validator.PushRegisterRequestValidator;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.push.client.MobilePlatform;
import io.getlime.push.client.PushServerClient;
import io.getlime.push.client.PushServerClientException;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.base.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.base.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller with services related to Push Server registration.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@RestController
@RequestMapping(value = "api/push")
public class PushRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(PushRegistrationController.class);

    private PushServerClient client;

    @Autowired
    public PushRegistrationController(PushServerClient client) {
        this.client = client;
    }

    @RequestMapping(value = "device/register", method = RequestMethod.POST)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public Response registerDeviceDefault(@RequestBody ObjectRequest<PushRegisterRequest> request, PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {
        return registerDeviceImpl(request, apiAuthentication);
    }

    @RequestMapping(value = "device/register/token", method = RequestMethod.POST)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public Response registerDeviceToken(@RequestBody ObjectRequest<PushRegisterRequest> request, PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {
        return registerDeviceImpl(request, apiAuthentication);
    }

    private Response registerDeviceImpl(ObjectRequest<PushRegisterRequest> request, PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {

        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration");
            throw new PowerAuthAuthenticationException("Unable to verify device registration");
        }

        logger.info("Push registration started, user ID: {}", apiAuthentication.getUserId());

        // Check the request body presence
        final PushRegisterRequest requestObject = request.getRequestObject();
        final String error = PushRegisterRequestValidator.validate(requestObject);
        if (error != null) {
            logger.error("Invalid request object in push registration - {}, user ID: {}", error, apiAuthentication.getUserId());
            throw new InvalidRequestObjectException();
        }

        // Get the values from the request
        String platform = requestObject.getPlatform();
        String token = requestObject.getToken();

        // Check if the context is authenticated - if it is, add activation ID.
        // This assures that the activation is assigned with a correct device.
        String activationId = apiAuthentication.getActivationId();
        Long applicationId = apiAuthentication.getApplicationId();

        // Verify that applicationId and activationId are set
        if (applicationId == null || activationId == null) {
            logger.error("Invalid activation in push registration, user ID: {}", apiAuthentication.getUserId());
            throw new PushRegistrationFailedException();
        }

        // Register the device and return response
        MobilePlatform mobilePlatform = MobilePlatform.Android;
        if ("ios".equalsIgnoreCase(platform)) {
            mobilePlatform = MobilePlatform.iOS;
        }
        try {
            boolean result = client.createDevice(applicationId, token, mobilePlatform, activationId);
            if (result) {
                logger.info("Push registration succeeded, user ID: {}", apiAuthentication.getUserId());
                return new Response();
            } else {
                logger.warn("Push registration failed, user ID: {}", apiAuthentication.getUserId());
                throw new PushRegistrationFailedException();
            }
        } catch (PushServerClientException ex) {
            logger.error("Push registration failed", ex);
            throw new PushRegistrationFailedException();
        }
    }

}
