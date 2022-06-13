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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsRequest;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import com.wultra.security.powerauth.client.v3.UpdateActivationFlagsRequest;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
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

    /**
     * Identity verification repository.
     */
    private final IdentityVerificationRepository identityVerificationRepository;

    /**
     * PowerAuth client.
     */
    private final PowerAuthClient powerAuthClient;
    private final HttpCustomizationService httpCustomizationService;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param powerAuthClient PowerAuth client.
     * @param httpCustomizationService HTTP customization service.
     */
    @Autowired
    public IdentityVerificationCreateService(IdentityVerificationRepository identityVerificationRepository, PowerAuthClient powerAuthClient, HttpCustomizationService httpCustomizationService) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.powerAuthClient = powerAuthClient;
        this.httpCustomizationService = httpCustomizationService;
    }

    /**
     * Creates new identity for the verification process.
     *
     * @param ownerId Owner identification.
     * @return Identity verification entity
     * @throws IdentityVerificationException Thrown when identity verification initialization fails.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    @Transactional
    public IdentityVerificationEntity createIdentityVerification(OwnerId ownerId, String processId) throws IdentityVerificationException, RemoteCommunicationException {
        try {
            final ListActivationFlagsRequest listRequest = new ListActivationFlagsRequest();
            listRequest.setActivationId(ownerId.getActivationId());
            final ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(
                    listRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );

            List<String> activationFlags = new ArrayList<>(response.getActivationFlags());
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                throw new IdentityVerificationException("Activation flag VERIFICATION_PENDING not found when initializing identity verification");
            }
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_PENDING);
            activationFlags.add(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            final UpdateActivationFlagsRequest updateRequest = new UpdateActivationFlagsRequest();
            updateRequest.setActivationId(ownerId.getActivationId());
            updateRequest.getActivationFlags().addAll(activationFlags);
            powerAuthClient.updateActivationFlags(
                    updateRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }

        final IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
        entity.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUserId(ownerId.getUserId());
        entity.setProcessId(processId);

        return identityVerificationRepository.save(entity);
    }

}
