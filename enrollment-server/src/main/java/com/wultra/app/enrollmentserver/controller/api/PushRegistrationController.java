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

import com.wultra.app.enrollmentserver.impl.service.PushRegistrationService;
import com.wultra.app.enrollmentserver.impl.util.ConditionalOnPropertyNotEmpty;
import com.wultra.app.enrollmentserver.api.model.request.PushRegisterRequest;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.errorhandling.PushRegistrationFailedException;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller with services related to Push Server registration.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ConditionalOnPropertyNotEmpty(value="powerauth.push.service.url")
@RestController
@RequestMapping(value = "api/push")
public class PushRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(PushRegistrationController.class);

    private final PushRegistrationService pushRegistrationService;

    /**
     * Constructor with autowired dependencies.
     *
     * @param pushRegistrationService Push registration service.
     */
    @Autowired
    public PushRegistrationController(PushRegistrationService pushRegistrationService) {
        this.pushRegistrationService = pushRegistrationService;
    }

    /**
     * Register device for the push notifications.
     *
     * @param request Push registration request.
     * @param apiAuthentication Authentication object.
     * @return Simple response.
     * @throws PowerAuthAuthenticationException In case authentication fails.
     * @throws InvalidRequestObjectException In case object validation fails.
     * @throws PushRegistrationFailedException In case push registration fails.
     */
    @RequestMapping(value = "device/register", method = RequestMethod.POST)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public Response registerDeviceDefault(@RequestBody ObjectRequest<PushRegisterRequest> request, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {
        return registerDeviceImpl(request, apiAuthentication);
    }

    /**
     * Register device for the push notifications. This method is present for the compatibility reasons only.
     *
     * @param request Push registration request.
     * @param apiAuthentication Authentication object.
     * @return Simple response.
     * @throws PowerAuthAuthenticationException In case authentication fails.
     * @throws InvalidRequestObjectException In case object validation fails.
     * @throws PushRegistrationFailedException In case push registration fails.
     */
    @RequestMapping(value = "device/register/token", method = RequestMethod.POST)
    @PowerAuthToken(signatureType = {
            PowerAuthSignatureTypes.POSSESSION,
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public Response registerDeviceToken(@RequestBody ObjectRequest<PushRegisterRequest> request, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {
        return registerDeviceImpl(request, apiAuthentication);
    }

    private Response registerDeviceImpl(
            ObjectRequest<PushRegisterRequest> request,
            PowerAuthApiAuthentication apiAuthentication)
            throws PowerAuthAuthenticationException, PushRegistrationFailedException, InvalidRequestObjectException {

        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration");
            throw new PowerAuthAuthenticationException("Unable to verify device registration");
        }

        // Check if the context is authenticated - if it is, add activation ID.
        // This assures that the activation is assigned with a correct device.
        final String userId = apiAuthentication.getUserId();
        final String activationId = apiAuthentication.getActivationContext().getActivationId();
        final Long applicationId = apiAuthentication.getApplicationId();

        return pushRegistrationService.registerDevice(request, userId, activationId, applicationId);
    }

}
