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
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.model.request.ListActivationFlagsRequest;
import com.wultra.security.powerauth.client.model.request.RemoveActivationFlagsRequest;
import com.wultra.security.powerauth.client.model.request.UpdateActivationFlagsRequest;
import com.wultra.security.powerauth.client.model.response.ListActivationFlagsResponse;
import com.wultra.security.powerauth.client.v4.PowerAuthClient;
import com.wultra.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for working with activation flags.
 *
 * @implNote Mind that the flags have to be disallowed in the enrollment server module in {@code MobileTokenController}.
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class ActivationFlagService {

    private static final String ACTIVATION_FLAG_VERIFICATION_PENDING = "VERIFICATION_PENDING";
    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    private final PowerAuthClient powerAuthClient;
    private final HttpCustomizationService httpCustomizationService;

    /**
     * Fetch initial activation flags.
     *
     * @return List of initial activation flags.
     */
    public List<String> fetchInitialActivationFlags() {
        return List.of(ACTIVATION_FLAG_VERIFICATION_PENDING);
    }

    /**
     * Add an activation flag when it is not present yet.
     *
     * @param ownerId Owner identification.
     * @param activationFlag Activation flag to add.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void addActivationFlag(final OwnerId ownerId, final String activationFlag) throws RemoteCommunicationException {
        try {
            final List<String> activationFlags = new ArrayList<>(listActivationFlagsInternal(ownerId.getActivationId()));
            if (!activationFlags.contains(activationFlag)) {
                activationFlags.add(activationFlag);
                updateActivationFlags(ownerId.getActivationId(), activationFlags);
            }
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed");
            throw new RemoteCommunicationException("Communication with PowerAuth server failed", ex);
        }
    }

    /**
     * Remove an activation flag when it is present.
     *
     * @param ownerId Owner identification.
     * @param activationFlag Activation flag to remove.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void removeActivationFlag(final OwnerId ownerId, final String activationFlag) throws RemoteCommunicationException {
        try {
            removeActivationFlags(ownerId.getActivationId(), Collections.singletonList(activationFlag));
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed");
            throw new RemoteCommunicationException("Communication with PowerAuth server failed", ex);
        }
    }

    /**
     * Initialize activation flags for the first identity verification in an onboarding process.
     * @param ownerId Owner identification.
     * @throws IdentityVerificationException Thrown when VERIFICATION_PENDING activation flag is missing.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void initActivationFlagsForIdentityVerification(OwnerId ownerId) throws IdentityVerificationException, RemoteCommunicationException {
        try {
            final List<String> activationFlags = new ArrayList<>(listActivationFlagsInternal(ownerId.getActivationId()));
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                throw new IdentityVerificationException("Activation flag VERIFICATION_PENDING not found when initializing identity verification, " + ownerId);
            }
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_PENDING);
            activationFlags.add(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            updateActivationFlags(ownerId.getActivationId(), activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed");
            throw new RemoteCommunicationException("Communication with PowerAuth server failed", ex);
        }
    }

    /**
     * Update activation flags for failed identity verification according to the process configuration.
     *
     * @param ownerId Owner identification.
     * @param configuration Onboarding process configuration.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    public void updateActivationFlagsForFailedIdentityVerification(
            final OwnerId ownerId,
            final OnboardingProcessConfigurationValue configuration) throws RemoteCommunicationException {

        if (configuration.existingActivation()) {
            removeActivationFlag(ownerId, configuration.existingActivationFlag());
        } else {
            updateActivationFlagsFromVerificationInProgressToVerificationPending(ownerId);
        }
    }

    /**
     * Update activation flags to restart the identity verification process.
     * Remove {@code VERIFICATION_IN_PROGRESS} but add {@code VERIFICATION_PENDING}.
     *
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    private void updateActivationFlagsFromVerificationInProgressToVerificationPending(final OwnerId ownerId) throws RemoteCommunicationException {
        try {
            final List<String> activationFlags = new ArrayList<>(listActivationFlagsInternal(ownerId.getActivationId()));

            // Remove flag VERIFICATION_IN_PROGRESS
            activationFlags.remove(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);

            // Add flag VERIFICATION_PENDING to restart the identity verification process
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING)) {
                activationFlags.add(ACTIVATION_FLAG_VERIFICATION_PENDING);
            }

            updateActivationFlags(ownerId.getActivationId(), activationFlags);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed");
            throw new RemoteCommunicationException("Communication with PowerAuth server failed", ex);
        }
    }

    /**
     * Update activation flags after a successful identity verification according to the process configuration.
     *
     * @param ownerId Owner identification.
     * @param configuration Onboarding process configuration.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when the legacy verification flag is missing.
     */
    public void updateActivationFlagsForSucceededIdentityVerification(
            final OwnerId ownerId,
            final OnboardingProcessConfigurationValue configuration) throws RemoteCommunicationException, IdentityVerificationException {

        if (configuration.existingActivation()) {
            removeActivationFlag(ownerId, configuration.existingActivationFlag());
        } else {
            removeActivationFlagVerificationInProgress(ownerId.getActivationId());
        }
    }

    /**
     * Remove activation flag {@code VERIFICATION_IN_PROGRESS} after a successful identity verification.
     *
     * @param activationId Activation ID.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when the activation flag VERIFICATION_IN_PROGRESS is not found.
     */
    private void removeActivationFlagVerificationInProgress(final String activationId)
            throws RemoteCommunicationException, IdentityVerificationException {

        try {
            final List<String> activationFlags = listActivationFlagsInternal(activationId);
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
                throw new IdentityVerificationException("Activation flag VERIFICATION_IN_PROGRESS not found when completing identity verification");
            }

            removeActivationFlags(activationId, Collections.singletonList(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS));
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed");
            throw new RemoteCommunicationException("Communication with PowerAuth server failed", ex);
        }
    }

    /**
     * Find out if activation flag {@code VERIFICATION_PENDING} is present.
     *
     * @param activationId Activation ID.
     * @return {@code True} if activation flag {@code VERIFICATION_PENDING} is present, {@code false} otherwise.
     * @throws RemoteCommunicationException Thrown when list of activation flags could not be obtained.
     */
    public boolean containsActivationFlagVerificationPending(final String activationId) throws RemoteCommunicationException {
        try {
            final List<String> flags = listActivationFlagsInternal(activationId);
            return flags.contains(ACTIVATION_FLAG_VERIFICATION_PENDING);
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed");
            throw new RemoteCommunicationException("Communication with PowerAuth server failed", ex);
        }
    }

    /**
     * Obtain list of activation flags.
     * @param activationId Activation ID.
     * @throws PowerAuthClientException Thrown when list of activation flags could not be obtained.
     */
    private List<String> listActivationFlagsInternal(final String activationId) throws PowerAuthClientException {
        final ListActivationFlagsRequest request = new ListActivationFlagsRequest();
        request.setActivationId(activationId);
        final ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(
                request,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );

        return response.getActivationFlags();
    }

    /**
     * Update activation flags.
     * @param activationId Activation ID.
     * @param activationFlags Activation flags to set.
     * @throws PowerAuthClientException Thrown when activation flags could not be updated.
     */
    private void updateActivationFlags(final String activationId, final List<String> activationFlags) throws PowerAuthClientException {
        final UpdateActivationFlagsRequest updateRequest = new UpdateActivationFlagsRequest();
        updateRequest.setActivationId(activationId);
        updateRequest.getActivationFlags().addAll(activationFlags);
        powerAuthClient.updateActivationFlags(
                updateRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
    }

    /**
     * Remove activation flags.
     * @param activationId Activation ID.
     * @param activationFlagsToRemove Activation flags to remove.
     * @throws PowerAuthClientException Thrown when activation flags could not be removed.
     */
    private void removeActivationFlags(final String activationId, List<String> activationFlagsToRemove) throws PowerAuthClientException {
        final RemoveActivationFlagsRequest removeRequest = new RemoveActivationFlagsRequest();
        removeRequest.setActivationId(activationId);
        removeRequest.getActivationFlags().addAll(activationFlagsToRemove);
        powerAuthClient.removeActivationFlags(
                removeRequest,
                httpCustomizationService.getQueryParams(),
                httpCustomizationService.getHttpHeaders()
        );
    }
}