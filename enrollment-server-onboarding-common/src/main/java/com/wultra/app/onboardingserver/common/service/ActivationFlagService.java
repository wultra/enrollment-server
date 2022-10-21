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

package com.wultra.app.onboardingserver.common.service;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.*;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for working with activation flags.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class ActivationFlagService {

    private static final Logger logger = LoggerFactory.getLogger(ActivationFlagService.class);

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
     * Initialize activation flags for the first identity verification in an onboarding process.
     * @param ownerId Owner identification.
     * @throws IdentityVerificationException Thrown when VERIFICATION_PENDING activation flag is missing.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void initActivationFlagsForIdentityVerification(OwnerId ownerId) throws IdentityVerificationException, RemoteCommunicationException {
        try {
            final List<String> activationFlags = listActivationFlagsInternal(ownerId);
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                throw new IdentityVerificationException("Activation flag VERIFICATION_PENDING not found when initializing identity verification");
            }
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_PENDING);
            activationFlags.add(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            updateActivationFlags(ownerId, activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }
    }

    /**
     * Update activation flags for failed identity verification.
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void updateActivationFlagsForFailedIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException {
        try {
            final List<String> activationFlags = listActivationFlagsInternal(ownerId);

            // Remove flag VERIFICATION_IN_PROGRESS
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            // Add flag VERIFICATION_PENDING to restart the identity verification process
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                activationFlags.add(ACTIVATION_FLAG_VERIFICATION_PENDING);
            }

            updateActivationFlags(ownerId, activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }
    }

    /**
     * Update activation flags for successful completion of identity verification.
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when
     */
    public void updateActivationFlagsForSucceededIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException, IdentityVerificationException {
        try {
            final List<String> activationFlags = listActivationFlagsInternal(ownerId);
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
                throw new IdentityVerificationException("Activation flag VERIFICATION_IN_PROGRESS not found when completing identity verification");
            }

            // Remove flag VERIFICATION_IN_PROGRESS
            removeActivationFlags(ownerId, Collections.singletonList(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS));
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }
    }

    /**
     * Obtain list of activation flags.
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when list of activation flags could not be obtained.
     */
    public List<String> listActivationFlags(OwnerId ownerId) throws RemoteCommunicationException {
        try {
            return listActivationFlagsInternal(ownerId);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }
    }

    /**
     * Obtain list of activation flags.
     * @param ownerId Owner identification.
     * @throws PowerAuthClientException Thrown when list of activation flags could not be obtained.
     */
    private List<String> listActivationFlagsInternal(OwnerId ownerId) throws PowerAuthClientException {
        final ListActivationFlagsRequest listRequest = new ListActivationFlagsRequest();
        listRequest.setActivationId(ownerId.getActivationId());
        final ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(
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
    private void updateActivationFlags(OwnerId ownerId, List<String> activationFlags) throws PowerAuthClientException {
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
    private void removeActivationFlags(OwnerId ownerId, List<String> activationFlagsToRemove) throws PowerAuthClientException {
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