/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            logger.error(ex.getMessage(), ex);
            throw new DocumentVerificationException("Communication with PowerAuth server failed");
        }
    }

}
