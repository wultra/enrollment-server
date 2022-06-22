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

import com.wultra.app.enrollmentserver.common.onboarding.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.errorhandling.RemoteCommunicationException;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsRequest;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import com.wultra.security.powerauth.client.v3.RemoveActivationFlagsRequest;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
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
    private final OnboardingServiceImpl onboardingService;
    private final HttpCustomizationService httpCustomizationService;

    /**
     * Service constructor.
     * @param powerAuthClient PowerAuth client.
     * @param onboardingService Onboarding service.
     * @param httpCustomizationService HTTP customization service.
     */
    @Autowired
    public IdentityVerificationFinishService(PowerAuthClient powerAuthClient, OnboardingServiceImpl onboardingService, HttpCustomizationService httpCustomizationService) {
        this.powerAuthClient = powerAuthClient;
        this.onboardingService = onboardingService;
        this.httpCustomizationService = httpCustomizationService;
    }

    /**
     * Finish identity verification by removing the VERIFICATION_IN_PROGRESS flag.
     *
     * @param ownerId Owner identification.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException Thrown when onboarding process termination fails.
     */
    @Transactional
    public void finishIdentityVerification(OwnerId ownerId) throws RemoteCommunicationException, OnboardingProcessException {
        try {
            final ListActivationFlagsRequest listRequest = new ListActivationFlagsRequest();
            listRequest.setActivationId(ownerId.getActivationId());
            final ListActivationFlagsResponse response = powerAuthClient.listActivationFlags(
                    listRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );
            final List<String> activationFlags = response.getActivationFlags();
            if (!activationFlags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
                // Identity verification has already been finished in PowerAuth server
                return;
            }
            final RemoveActivationFlagsRequest removeRequest = new RemoveActivationFlagsRequest();
            removeRequest.setActivationId(ownerId.getActivationId());
            removeRequest.getActivationFlags().add(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS);
            powerAuthClient.removeActivationFlags(
                    removeRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }

        // Terminate onboarding process
        final OnboardingProcessEntity processEntity = onboardingService.findExistingProcessWithVerificationInProgress(ownerId.getActivationId());
        processEntity.setStatus(OnboardingStatus.FINISHED);
        processEntity.setTimestampFinished(new Date());
        onboardingService.updateProcess(processEntity);
    }

}
