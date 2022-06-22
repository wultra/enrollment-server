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

import com.wultra.app.enrollmentserver.api.model.request.IdentityVerificationStatusRequest;
import com.wultra.app.enrollmentserver.api.model.response.IdentityVerificationStatusResponse;
import com.wultra.app.enrollmentserver.common.onboarding.errorhandling.OnboardingProcessException;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.impl.service.internal.JsonSerializationService;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsRequest;
import com.wultra.security.powerauth.client.v3.ListActivationFlagsResponse;
import io.getlime.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Service implementing document identity verification status services.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationStatusService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationStatusService.class);

    private final IdentityVerificationConfig identityVerificationConfig;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final IdentityVerificationService identityVerificationService;
    private final JsonSerializationService jsonSerializationService;
    private final PresenceCheckService presenceCheckService;
    private final IdentityVerificationFinishService identityVerificationFinishService;
    private final OnboardingServiceImpl onboardingService;
    private final IdentityVerificationOtpService identityVerificationOtpService;
    private final HttpCustomizationService httpCustomizationService;

    private final PowerAuthClient powerAuthClient;

    private static final String ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS = "VERIFICATION_IN_PROGRESS";

    /**
     * Service constructor.
     * @param identityVerificationConfig        Identity verification configuration.
     * @param identityVerificationRepository    Identity verification repository.
     * @param identityVerificationService       Identity verification service.
     * @param jsonSerializationService          JSON serialization service.
     * @param presenceCheckService              Presence check service.
     * @param identityVerificationFinishService Identity verification finish service.
     * @param onboardingService                 Onboarding service.
     * @param identityVerificationOtpService    Identity verification OTP service.
     * @param httpCustomizationService          HTTP customization service.
     * @param powerAuthClient                   PowerAuth client.
     */
    @Autowired
    public IdentityVerificationStatusService(
            IdentityVerificationConfig identityVerificationConfig,
            IdentityVerificationRepository identityVerificationRepository,
            IdentityVerificationService identityVerificationService,
            JsonSerializationService jsonSerializationService,
            PresenceCheckService presenceCheckService,
            IdentityVerificationFinishService identityVerificationFinishService,
            OnboardingServiceImpl onboardingService,
            IdentityVerificationOtpService identityVerificationOtpService, HttpCustomizationService httpCustomizationService, PowerAuthClient powerAuthClient) {
        this.identityVerificationConfig = identityVerificationConfig;
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckService = presenceCheckService;
        this.identityVerificationFinishService = identityVerificationFinishService;
        this.onboardingService = onboardingService;
        this.identityVerificationOtpService = identityVerificationOtpService;
        this.httpCustomizationService = httpCustomizationService;
        this.powerAuthClient = powerAuthClient;
    }

    /**
     * Check status of identity verification.
     *
     * @param request Identity verification status request.
     * @param ownerId Owner identifier.
     * @return Identity verification status response.
     * @throws IdentityVerificationException  Thrown when identity verification could not be started.
     * @throws RemoteCommunicationException   Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessException     Thrown when onboarding process is invalid.
     * @throws OnboardingOtpDeliveryException Thrown when OTP could not be sent when changing status.
     */
    @Transactional
    public IdentityVerificationStatusResponse checkIdentityVerificationStatus(IdentityVerificationStatusRequest request, OwnerId ownerId) throws IdentityVerificationException, RemoteCommunicationException, OnboardingProcessException, OnboardingOtpDeliveryException {
        IdentityVerificationStatusResponse response = new IdentityVerificationStatusResponse();

        Optional<IdentityVerificationEntity> idVerificationOptional =
                identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (!idVerificationOptional.isPresent()) {
            response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            response.setIdentityVerificationPhase(null);
            final OnboardingProcessEntity onboardingProcess = onboardingService.findProcessByActivationId(ownerId.getActivationId());
            response.setProcessId(onboardingProcess.getId());
            return response;
        }

        IdentityVerificationEntity idVerification = idVerificationOptional.get();
        response.setProcessId(idVerification.getProcessId());

        // Check activation flags, the identity verification entity may need to be re-initialized after cleanup
        try {
            final ListActivationFlagsRequest listRequest = new ListActivationFlagsRequest();
            listRequest.setActivationId(ownerId.getActivationId());
            final ListActivationFlagsResponse flagResponse = powerAuthClient.listActivationFlags(
                    listRequest,
                    httpCustomizationService.getQueryParams(),
                    httpCustomizationService.getHttpHeaders()
            );
            List<String> flags = flagResponse.getActivationFlags();
            if (!flags.contains(ACTIVATION_FLAG_VERIFICATION_IN_PROGRESS)) {
                // Initialization is required because verification is not in progress for current identity verification
                response.setIdentityVerificationStatus(IdentityVerificationStatus.NOT_INITIALIZED);
                response.setIdentityVerificationPhase(null);
                return response;
            }
        } catch (PowerAuthClientException ex) {
            logger.warn("Activation flag request failed, error: {}", ex.getMessage());
            logger.debug(ex.getMessage(), ex);
            throw new RemoteCommunicationException("Communication with PowerAuth server failed");
        }

        response.setIdentityVerificationPhase(idVerification.getPhase());

        if (IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            identityVerificationService.checkIdentityDocumentsForVerification(ownerId, idVerification);
        } else if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            response.setIdentityVerificationPhase(IdentityVerificationPhase.PRESENCE_CHECK);

            SessionInfo sessionInfo =
                    jsonSerializationService.deserialize(idVerification.getSessionInfo(), SessionInfo.class);
            if (sessionInfo == null) {
                logger.error("Checking presence verification failed due to invalid session info, {}", ownerId);
                idVerification.setErrorDetail("Unable to deserialize session info");
                idVerification.setStatus(IdentityVerificationStatus.FAILED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            } else {
                PresenceCheckResult presenceCheckResult = null;
                try {
                    presenceCheckResult =
                            presenceCheckService.checkPresenceVerification(ownerId, idVerification, sessionInfo);
                } catch (PresenceCheckException e) {
                    logger.error("Checking presence verification failed, {}", ownerId, e);
                    idVerification.setErrorDetail(e.getMessage());
                    idVerification.setStatus(IdentityVerificationStatus.FAILED);
                    idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                }

                if (presenceCheckResult != null) {
                    evaluatePresenceCheckResult(ownerId, idVerification, presenceCheckResult);
                }
            }
        } else if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase())
                && IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            continueAfterPresenceCheck(ownerId, idVerification);
        } else if (IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())
                && IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            startVerification(ownerId, idVerification);
        } else if (IdentityVerificationPhase.DOCUMENT_VERIFICATION.equals(idVerification.getPhase())
                && IdentityVerificationStatus.ACCEPTED.equals(idVerification.getStatus())) {
            logger.debug("Finished verification of documents for {}, {}", idVerification, ownerId);
            continueWithPresenceCheck(ownerId, idVerification);
        } else if (IdentityVerificationPhase.OTP_VERIFICATION.equals(idVerification.getPhase())
                && IdentityVerificationStatus.OTP_VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            if (identityVerificationOtpService.isUserVerifiedUsingOtp(idVerification.getProcessId())) {
                // OTP verification is complete, process document verification result
                try {
                    processVerificationResult(ownerId, idVerification);
                } catch (OnboardingProcessException e) {
                    logger.error("Updating onboarding process failed, {}", ownerId, e);
                    response.setIdentityVerificationStatus(IdentityVerificationStatus.FAILED);
                    return response;
                }
            } else {
                logger.debug("Pending OTP verification, {}", ownerId);
            }
        }

        response.setIdentityVerificationStatus(idVerification.getStatus());
        response.setIdentityVerificationPhase(idVerification.getPhase());
        return response;
    }

    private void processVerificationResult(
            OwnerId ownerId,
            IdentityVerificationEntity idVerification) throws RemoteCommunicationException, OnboardingProcessException {
        identityVerificationService.processDocumentVerificationResult(ownerId, idVerification, IdentityVerificationPhase.COMPLETED);
        if (idVerification.getStatus() == IdentityVerificationStatus.ACCEPTED) {
            identityVerificationFinishService.finishIdentityVerification(ownerId);
        }
    }

    private void evaluatePresenceCheckResult(OwnerId ownerId,
                                             IdentityVerificationEntity idVerification,
                                             PresenceCheckResult result) {
        switch (result.getStatus()) {
            case ACCEPTED:
                idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.info("Presence check accepted, {}", ownerId);
                break;
            case FAILED:
                idVerification.setErrorDetail(result.getErrorDetail());
                idVerification.setStatus(IdentityVerificationStatus.FAILED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.warn("Presence check failed, {}, errorDetail: '{}'", ownerId, result.getErrorDetail());
                break;
            case IN_PROGRESS:
                logger.debug("Presence check still in progress, {}", ownerId);
                break;
            case REJECTED:
                idVerification.setRejectReason(result.getRejectReason());
                idVerification.setStatus(IdentityVerificationStatus.REJECTED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.warn("Presence check rejected, {}, rejectReason: '{}'", ownerId, result.getRejectReason());
                break;
            default:
                throw new IllegalStateException("Unexpected presence check result status: " + result.getStatus());
        }
    }

    private void continueWithPresenceCheck(OwnerId ownerId, IdentityVerificationEntity idVerification)
            throws OnboardingOtpDeliveryException, OnboardingProcessException, RemoteCommunicationException {
        if (identityVerificationConfig.isPresenceCheckEnabled()) {
            idVerification.setPhase(IdentityVerificationPhase.PRESENCE_CHECK);
            idVerification.setStatus(IdentityVerificationStatus.NOT_INITIALIZED);
            idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            logger.info("Switched to wait for the presence check, {}", ownerId);
        } else {
            continueAfterPresenceCheck(ownerId, idVerification);
        }
    }

    private void continueAfterPresenceCheck(OwnerId ownerId, IdentityVerificationEntity idVerification)
            throws OnboardingOtpDeliveryException, OnboardingProcessException, RemoteCommunicationException {
        if (identityVerificationConfig.isVerificationOtpEnabled()) {
            // OTP verification is pending, switch to OTP verification state and send OTP code even in case identity verification fails
            idVerification.setStatus(IdentityVerificationStatus.OTP_VERIFICATION_PENDING);
            idVerification.setPhase(IdentityVerificationPhase.OTP_VERIFICATION);
            identityVerificationOtpService.sendOtp(ownerId, idVerification);
        } else {
            processVerificationResult(ownerId, idVerification);
        }
    }

    private void startVerification(OwnerId ownerId, IdentityVerificationEntity idVerification)
            throws IdentityVerificationException {
        try {
            identityVerificationService.startVerification(ownerId);
            logger.info("Started document verification process of {}", idVerification);
        } catch (DocumentVerificationException e) {
            idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);
            idVerification.setStatus(IdentityVerificationStatus.FAILED);
            idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            logger.warn("Verification start failed, {}", ownerId, e);
        }
    }

}
