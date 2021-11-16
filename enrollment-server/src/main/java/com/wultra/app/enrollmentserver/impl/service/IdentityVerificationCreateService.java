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

import com.wultra.app.enrollmentserver.database.IdentityVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementing creating of identity verification.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationCreateService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationCreateService.class);

    private static final String ACTIVATION_FLAG_VERIFICATION_PENDING = "VERIFICATION_PENDING";
    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    private final IdentityVerificationRepository identityVerificationRepository;
    private final PowerAuthClient powerAuthClient;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param powerAuthClient PowerAuth client.
     */
    @Autowired
    public IdentityVerificationCreateService(IdentityVerificationRepository identityVerificationRepository, PowerAuthClient powerAuthClient) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.powerAuthClient = powerAuthClient;
    }

    @Transactional
    public IdentityVerificationEntity createIdentityVerification(OwnerId ownerId) throws DocumentVerificationException {
        try {
            ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(ownerId.getActivationId());
            List<String> activationFlags = new ArrayList<>(response.getActivationFlags());
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                throw new DocumentVerificationException("Activation flag VERIFICATION_PENDING not found when initializing identity verification");
            }
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_PENDING);
            activationFlags.add(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);
            powerAuthClient.updateActivationFlags(ownerId.getActivationId(), activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.error(ex.getMessage(), ex);
            throw new DocumentVerificationException("Communication with PowerAuth server failed");
        }
        IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
        entity.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUserId(ownerId.getUserId());
        return identityVerificationRepository.save(entity);
    }

}
