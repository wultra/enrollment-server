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

import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;

/**
 * Service implementing finishing of identity verification.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationFinishService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationFinishService.class);

    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    private final PowerAuthClient powerAuthClient;

    /**
     * Service constructor.
     * @param powerAuthClient PowerAuth client.
     */
    @Autowired
    public IdentityVerificationFinishService(PowerAuthClient powerAuthClient) {
        this.powerAuthClient = powerAuthClient;
    }

    @Transactional
    public void finishIdentityVerification(OwnerId ownerId) throws DocumentVerificationException {
        try {
            ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(ownerId.getActivationId());
            List<String> activationFlags = response.getActivationFlags();
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
                // Identity verification has already been finished in PowerAuth server
                return;
            }
            powerAuthClient.removeActivationFlags(ownerId.getActivationId(), Collections.singletonList(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS));
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new DocumentVerificationException("Communication with PowerAuth server failed");
        }
    }

}
