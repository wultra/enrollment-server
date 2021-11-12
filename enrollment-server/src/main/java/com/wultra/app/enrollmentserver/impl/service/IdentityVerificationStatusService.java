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

import com.wultra.app.enrollmentserver.database.IdentityVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.enrollmentserver.impl.util.PowerAuthUtil;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.enrollmentserver.model.request.IdentityVerificationStatusRequest;
import com.wultra.app.enrollmentserver.model.response.IdentityVerificationStatusResponse;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

/**
 * Service implementing document identity verification status services.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class IdentityVerificationStatusService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationService.class);

    private final IdentityVerificationRepository identityVerificationRepository;

    private final IdentityVerificationService identityVerificationService;

    private final JsonSerializationService jsonSerializationService;

    private final PresenceCheckService presenceCheckService;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param identityVerificationService Identity verification service.
     * @param jsonSerializationService JSON serialization service.
     * @param presenceCheckService Presence check service.
     */
    @Autowired
    public IdentityVerificationStatusService(
            IdentityVerificationRepository identityVerificationRepository,
            IdentityVerificationService identityVerificationService,
            JsonSerializationService jsonSerializationService,
            PresenceCheckService presenceCheckService) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckService = presenceCheckService;
    }

    /**
     * Check status of identity verification.
     * @param request Identity verification status request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Identity verification status response.
     */
    @Transactional
    public IdentityVerificationStatusResponse checkIdentityVerificationStatus(IdentityVerificationStatusRequest request, PowerAuthApiAuthentication apiAuthentication) {
        IdentityVerificationStatusResponse response = new IdentityVerificationStatusResponse();

        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        Optional<IdentityVerificationEntity> idVerificationOptional =
                identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (!idVerificationOptional.isPresent()) {
            logger.error("Checking identity verification status on not existing entity, {}", ownerId);
            response.setIdentityVerificationStatus(IdentityVerificationStatus.FAILED);
            return response;
        }
        IdentityVerificationEntity idVerification = idVerificationOptional.get();

        if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            response.setIdentityVerificationPhase(IdentityVerificationPhase.PRESENCE_CHECK);

            SessionInfo sessionInfo =
                    jsonSerializationService.deserialize(idVerification.getSessionInfo(), SessionInfo.class);
            if (sessionInfo == null) {
                logger.error("Checking presence verification failed due to invalid session info, " + ownerId);
                idVerification.setErrorDetail("Unable to deserialize session info");
                idVerification.setStatus(IdentityVerificationStatus.FAILED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            } else {
                PresenceCheckResult presenceCheckResult = null;
                try {
                    presenceCheckResult =
                            presenceCheckService.checkPresenceVerification(apiAuthentication, sessionInfo);
                } catch (DocumentVerificationException | PresenceCheckException e) {
                    logger.error("Checking presence verification failed, " + ownerId, e);
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
            try {
                identityVerificationService.startVerification(ownerId);
            } catch (DocumentVerificationException e) {
                idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);
                idVerification.setStatus(IdentityVerificationStatus.FAILED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.warn("Verification start failed, " + ownerId, e);
            }

        } else if (IdentityVerificationPhase.DOCUMENT_VERIFICATION.equals(idVerification.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            // TODO check verification result, set final status to identity and finish documents verification
        }

        response.setIdentityVerificationStatus(idVerification.getStatus());
        response.setIdentityVerificationPhase(idVerification.getPhase());
        return response;
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

}
