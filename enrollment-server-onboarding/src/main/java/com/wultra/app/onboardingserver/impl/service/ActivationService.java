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
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.GetActivationStatusRequest;
import com.wultra.security.powerauth.client.model.request.RemoveActivationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for working with activations.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
public class ActivationService {

    private final PowerAuthClient powerAuthClient;

    /**
     * All-arg constructor.
     *
     * @param powerAuthClient PowerAuth service client.
     */
    public ActivationService(final PowerAuthClient powerAuthClient) {
        this.powerAuthClient = powerAuthClient;
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
            powerAuthClient.removeActivation(request);
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
            return powerAuthClient.getActivationStatus(request).getActivationStatus();
        } catch (PowerAuthClientException e) {
            throw new RemoteCommunicationException("Communication with PowerAuth server failed: " + e.getMessage(), e);
        }
    }
}
