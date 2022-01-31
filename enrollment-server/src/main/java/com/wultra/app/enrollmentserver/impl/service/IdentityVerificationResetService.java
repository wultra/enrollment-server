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

import com.wultra.app.enrollmentserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service implementing reset of identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationResetService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationResetService.class);

    private static final String ACTIVATION_FLAG_VERIFICATION_PENDING = "VERIFICATION_PENDING";
    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    /**
     * PowerAuth client.
     */
    private final PowerAuthClient powerAuthClient;

    /**
     * Service constructor.
     * @param powerAuthClient PowerAuth client.
     */
    @Autowired
    public IdentityVerificationResetService(PowerAuthClient powerAuthClient) {
        this.powerAuthClient = powerAuthClient;
    }

    /**
     * Reset identity verification by setting activation flag to VERIFICATION_PENDING.
     *
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void resetIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException {
        try {
            ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(ownerId.getActivationId());

            List<String> activationFlags = new ArrayList<>(response.getActivationFlags());
            // Remove flag VERIFICATION_IN_PROGRESS
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            // Add flag VERIFICATION_PENDING to restart the identity verification process
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                activationFlags.add(ACTIVATION_FLAG_VERIFICATION_PENDING);
            }

            powerAuthClient.updateActivationFlags(ownerId.getActivationId(), activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }
    }

}
