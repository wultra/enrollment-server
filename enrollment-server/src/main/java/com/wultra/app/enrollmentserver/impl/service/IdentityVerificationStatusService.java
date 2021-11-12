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
import com.wultra.app.enrollmentserver.impl.util.PowerAuthUtil;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
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

    private final PresenceCheckService presenceCheckService;

    /**
     * Service constructor.
     * @param identityVerificationRepository Identity verification repository.
     * @param identityVerificationService Identity verification service.
     * @param presenceCheckService Presence check service.
     */
    @Autowired
    public IdentityVerificationStatusService(
            IdentityVerificationRepository identityVerificationRepository,
            IdentityVerificationService identityVerificationService,
            PresenceCheckService presenceCheckService) {
        this.identityVerificationRepository = identityVerificationRepository;
        this.identityVerificationService = identityVerificationService;
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
        IdentityVerificationEntity identityVerificationEntity = idVerificationOptional.get();

        if (IdentityVerificationPhase.PRESENCE_CHECK.equals(identityVerificationEntity.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(identityVerificationEntity.getStatus())) {
            SessionInfo sessionInfo = new SessionInfo();
            // TODO use previously stored session info
            PresenceCheckResult presenceCheckResult;
            try {
                presenceCheckResult =
                        presenceCheckService.checkPresenceVerification(apiAuthentication, sessionInfo);
            } catch (DocumentVerificationException | PresenceCheckException e) {
                logger.error("Checking presence verification failed, " + ownerId, e);
                response.setIdentityVerificationStatus(IdentityVerificationStatus.FAILED);
                return response;
            }

            PresenceCheckStatus presenceCheckStatus = presenceCheckResult.getStatus();

            if (PresenceCheckStatus.IN_PROGRESS.equals(presenceCheckStatus)) {
                logger.debug("Presence check still in progress, {}", ownerId);
            } else if (PresenceCheckStatus.ACCEPTED.equals(presenceCheckStatus)) {
                identityVerificationEntity.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
                logger.info("Presence check accepted, {}", ownerId);
            } else if (PresenceCheckStatus.FAILED.equals(presenceCheckStatus)) {
                identityVerificationEntity.setErrorDetail(presenceCheckResult.getErrorDetail());
                identityVerificationEntity.setStatus(IdentityVerificationStatus.FAILED);
                logger.warn("Presence check failed, {}", ownerId);
            } else if (PresenceCheckStatus.REJECTED.equals(presenceCheckStatus)) {
                identityVerificationEntity.setRejectReason(presenceCheckResult.getRejectReason());
                identityVerificationEntity.setStatus(IdentityVerificationStatus.REJECTED);
                logger.warn("Presence check rejected, {}", ownerId);
            }
        } else if (IdentityVerificationPhase.PRESENCE_CHECK.equals(identityVerificationEntity.getPhase())
                && IdentityVerificationStatus.VERIFICATION_PENDING.equals(identityVerificationEntity.getStatus())) {
            try {
                identityVerificationService.startVerification(ownerId);
            } catch (DocumentVerificationException e) {
                identityVerificationEntity.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);
                identityVerificationEntity.setStatus(IdentityVerificationStatus.FAILED);
                logger.warn("Verification start failed, " + ownerId, e);
            }
        } else if (IdentityVerificationPhase.DOCUMENT_VERIFICATION.equals(identityVerificationEntity.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(identityVerificationEntity.getStatus())) {
            // TODO check verification result, set final status to identity and finish documents verification
        }

        response.setIdentityVerificationStatus(identityVerificationEntity.getStatus());
        response.setIdentityVerificationPhase(identityVerificationEntity.getPhase());
        return response;
    }

}
