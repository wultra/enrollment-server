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

import com.wultra.app.enrollmentserver.api.model.enrollment.request.PushRegisterRequest;
import com.wultra.app.enrollmentserver.errorhandling.InvalidRequestObjectException;
import com.wultra.app.enrollmentserver.errorhandling.PushRegistrationFailedException;
import com.wultra.app.enrollmentserver.impl.service.PushRegistrationService;
import com.wultra.app.enrollmentserver.impl.util.ConditionalOnPropertyNotEmpty;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.core.rest.model.base.response.Response;
import com.wultra.security.powerauth.crypto.lib.enums.PowerAuthCodeType;
import com.wultra.security.powerauth.rest.api.spring.annotation.PowerAuthToken;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.wultra.app.enrollmentserver.logging.StructuredLogging.*;

/**
 * Controller with services related to Push Server registration.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@ConditionalOnPropertyNotEmpty("powerauth.push.service.url")
@RestController
@RequestMapping(value = "api/push")
@AllArgsConstructor
@Slf4j
public class PushRegistrationController {

    private final PushRegistrationService pushRegistrationService;

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
    @PostMapping("device/register")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION,
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public Response registerDeviceDefault(@RequestBody ObjectRequest<PushRegisterRequest> request, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {
        validateApiAuthentication(apiAuthentication);

        logger.info("", action("registerDeviceDefault"), stateInitiated(), kv("userId", apiAuthentication.getUserId()));
        final Response response = pushRegistrationService.registerDevice(request, apiAuthentication);
        logger.info("", action("registerDeviceDefault"), stateSucceeded());
        return response;
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
    @PostMapping("device/register/token")
    @PowerAuthToken(authenticationCodeType = {
            PowerAuthCodeType.POSSESSION,
            PowerAuthCodeType.POSSESSION_BIOMETRY,
            PowerAuthCodeType.POSSESSION_KNOWLEDGE
    })
    public Response registerDeviceToken(@RequestBody ObjectRequest<PushRegisterRequest> request, @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidRequestObjectException, PushRegistrationFailedException {
        validateApiAuthentication(apiAuthentication);

        logger.info("", action("registerDeviceToken"), stateInitiated(), kv("userId", apiAuthentication.getUserId()));
        final Response response = pushRegistrationService.registerDevice(request, apiAuthentication);
        logger.info("", action("registerDeviceToken"), stateSucceeded());
        return response;
    }

    private static void validateApiAuthentication(final PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException {
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration");
            throw new PowerAuthAuthenticationException("Unable to verify device registration");
        }
    }

}
