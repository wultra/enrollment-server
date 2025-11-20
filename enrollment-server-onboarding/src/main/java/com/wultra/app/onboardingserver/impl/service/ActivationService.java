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

import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import com.wultra.security.powerauth.client.model.enumeration.CommitPhase;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.InitActivationRequest;
import com.wultra.security.powerauth.client.model.request.LookupApplicationByAppKeyRequest;
import com.wultra.security.powerauth.client.model.request.RemoveActivationRequest;
import com.wultra.security.powerauth.client.model.request.v3.GetActivationStatusRequest;
import com.wultra.security.powerauth.client.model.response.InitActivationResponse;
import com.wultra.security.powerauth.client.model.response.LookupApplicationByAppKeyResponse;
import com.wultra.security.powerauth.client.v3.PowerAuthClient;
import com.wultra.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for working with activations.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class ActivationService {

    private final PowerAuthClient powerAuthClient;

    private final HttpCustomizationService httpCustomizationService;


    /**
     * Init activation.
     *
     * @param userId user ID
     * @param applicationKey Application key
     * @return activation code
     * @throws RemoteCommunicationException if communication with PowerAuth server fails
     */
    public String initActivation(final String userId, final String applicationKey) throws RemoteCommunicationException {
        try {
            final LookupApplicationByAppKeyRequest lookupRequest = new LookupApplicationByAppKeyRequest();
            lookupRequest.setApplicationKey(applicationKey);

            final LookupApplicationByAppKeyResponse lookupResponse = powerAuthClient.lookupApplicationByAppKey(
                    lookupRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders());

            final InitActivationRequest request = new InitActivationRequest();
            request.setApplicationId(lookupResponse.getApplicationId());
            request.setUserId(userId);
            request.setCommitPhase(CommitPhase.ON_KEY_EXCHANGE);

            final InitActivationResponse initActivationResponse = powerAuthClient.initActivation(
                    request,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders());

            return initActivationResponse.getActivationCode();
        } catch (PowerAuthClientException e) {
            throw new RemoteCommunicationException("Communication with PowerAuth server failed: " + e.getMessage(), e);
        }
    }

    /**
     * Remove activation.
     *
     * @param activationId Activation ID.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void removeActivation(final String activationId) throws RemoteCommunicationException {
        final RemoveActivationRequest request = new RemoveActivationRequest();
        request.setActivationId(activationId);

        try {
            powerAuthClient.removeActivation(request, httpCustomizationService.getQueryParams(), httpCustomizationService.getHttpHeaders());
        } catch (PowerAuthClientException e) {
            throw new RemoteCommunicationException("Communication with PowerAuth server failed: " + e.getMessage(), e);
        }
    }

    /**
     * Return activation status.
     *
     * @param activationId Activation ID.
     * @return activation status
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public ActivationStatus fetchActivationStatus(final String activationId) throws RemoteCommunicationException {
        final GetActivationStatusRequest request = new GetActivationStatusRequest();
        request.setActivationId(activationId);

        try {
            return powerAuthClient.getActivationStatus(request, httpCustomizationService.getQueryParams(), httpCustomizationService.getHttpHeaders())
                    .getActivationStatus();
        } catch (PowerAuthClientException e) {
            throw new RemoteCommunicationException("Communication with PowerAuth server failed: " + e.getMessage(), e);
        }
    }
}
