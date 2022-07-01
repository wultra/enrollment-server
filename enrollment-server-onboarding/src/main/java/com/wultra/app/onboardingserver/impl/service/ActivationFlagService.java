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
 *
 */

package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.*;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for working with activation flags.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class ActivationFlagService {

    public static final String ACTIVATION_FLAG_VERIFICATION_PENDING = "VERIFICATION_PENDING";
    public static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    private final PowerAuthClient powerAuthClient;
    private final HttpCustomizationService httpCustomizationService;

    /**
     * Service constructor.
     * @param powerAuthClient PowerAuth service client.
     * @param httpCustomizationService HTTP customization service.
     */
    public ActivationFlagService(PowerAuthClient powerAuthClient, HttpCustomizationService httpCustomizationService) {
        this.powerAuthClient = powerAuthClient;
        this.httpCustomizationService = httpCustomizationService;
    }

    /**
     * Obtain list of activation flags.
     * @param ownerId Owner identification.
     * @throws PowerAuthClientException Thrown when list of activation flags could not be obtained.
     */
    public List<String> listActivationFlags(OwnerId ownerId) throws PowerAuthClientException {
        final ListActivationFlagsRequest listRequest = new ListActivationFlagsRequest();
        listRequest.setActivationId(ownerId.getActivationId());
        ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(
                listRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );

        return new ArrayList<>(response.getActivationFlags());
    }

    /**
     * Update activation flags.
     * @param ownerId Owner identification.
     * @param activationFlags Activation flags to set.
     * @throws PowerAuthClientException Thrown when activation flags could not be updated.
     */
    public void updateActivationFlags(OwnerId ownerId, List<String> activationFlags) throws PowerAuthClientException {
        final UpdateActivationFlagsRequest updateRequest = new UpdateActivationFlagsRequest();
        updateRequest.setActivationId(ownerId.getActivationId());
        updateRequest.getActivationFlags().addAll(activationFlags);
        powerAuthClient.updateActivationFlags(
                updateRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
    }

    /**
     * Remove activation flags.
     * @param ownerId Owner identification.
     * @param activationFlagsToRemove Activation flags to remove.
     * @throws PowerAuthClientException Thrown when activation flags could not be removed.
     */
    public void removeActivationFlags(OwnerId ownerId, List<String> activationFlagsToRemove) throws PowerAuthClientException {
        final RemoveActivationFlagsRequest removeRequest = new RemoveActivationFlagsRequest();
        removeRequest.setActivationId(ownerId.getActivationId());
        removeRequest.getActivationFlags().addAll(activationFlagsToRemove);
        powerAuthClient.removeActivationFlags(
                removeRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
    }
}