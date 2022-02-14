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

import com.google.common.base.Ascii;
import com.wultra.app.enrollmentserver.configuration.IdentityVerificationConfig;
import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.enrollmentserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.enrollmentserver.impl.util.PowerAuthUtil;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.provider.PresenceCheckProvider;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

/**
 * Service implementing presence check.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class PresenceCheckService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceCheckService.class);

    private final IdentityVerificationConfig identityVerificationConfig;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final DocumentProcessingService documentProcessingService;

    private final IdentityVerificationService identityVerificationService;

    private final JsonSerializationService jsonSerializationService;

    private final PresenceCheckProvider presenceCheckProvider;

    /**
     * Service constructor.
     * @param documentVerificationRepository Document verification repository.
     * @param documentProcessingService Document processing service.
     * @param identityVerificationService Identity verification service.
     * @param jsonSerializationService JSON serialization service.
     * @param presenceCheckProvider Presence check provider.
     */
    @Autowired
    public PresenceCheckService(
            IdentityVerificationConfig identityVerificationConfig,
            DocumentVerificationRepository documentVerificationRepository,
            DocumentProcessingService documentProcessingService,
            IdentityVerificationService identityVerificationService,
            JsonSerializationService jsonSerializationService,
            PresenceCheckProvider presenceCheckProvider) {
        this.identityVerificationConfig = identityVerificationConfig;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckProvider = presenceCheckProvider;
    }

    /**
     * Initializes presence check process.
     *
     * @param ownerId Owner identification.
     * @param processId Process identifier.
     * @return Session info with data needed to perform the presence check process
     * @throws DocumentVerificationException When an error during obtaining the user personal image occurred
     * @throws PresenceCheckException When an error during initializing the presence check occurred
     */
    @Transactional
    public SessionInfo init(OwnerId ownerId, String processId)
            throws DocumentVerificationException, PresenceCheckException {
        IdentityVerificationEntity idVerification = fetchIdVerification(ownerId);

        String processIdOnboarding = idVerification.getProcessId();
        if (!processIdOnboarding.equals(processId)) {
            logger.warn("Invalid process ID received in request: {}", processId);
            throw new PresenceCheckException("Invalid process ID");
        }

        if (!idVerification.isPresenceCheckInitialized()) {
            // TODO - use a better way to locate the photo to be used in presence check
            Optional<DocumentVerificationEntity> docVerificationEntityWithPhoto =
                    documentVerificationRepository.findFirstByActivationIdAndPhotoIdNotNull(ownerId.getActivationId());

            if (docVerificationEntityWithPhoto.isPresent()) {
                String photoId = docVerificationEntityWithPhoto.get().getPhotoId();
                Image photo = identityVerificationService.getPhotoById(photoId);
                presenceCheckProvider.initPresenceCheck(ownerId, photo);
            } else {
                logger.error("Missing selfie photo to initialize presence check, {}", ownerId);
                throw new PresenceCheckException("Unable to initialize presence check");
            }
        }
        return startPresenceCheck(ownerId, idVerification);
    }

    /**
     * Checks presence verification result.
     * <p>
     *     When is the presence check accepted the person image is submitted to document verification provider.
     * </p>
     *
     * @param ownerId Owner identifier.
     * @param idVerification Identity verification entity.
     * @param sessionInfo Session info with presence check data.
     * @return Result of the presence check
     * @throws PresenceCheckException When an error during the presence check verification occurred
     */
    @Transactional
    public PresenceCheckResult checkPresenceVerification(OwnerId ownerId,
                                                         IdentityVerificationEntity idVerification,
                                                         SessionInfo sessionInfo) throws PresenceCheckException {
        PresenceCheckResult result = presenceCheckProvider.getResult(ownerId, sessionInfo);

        if (!PresenceCheckStatus.ACCEPTED.equals(result.getStatus())) {
            logger.info("Not accepted presence check, {}", ownerId);
            return result;
        }
        logger.debug("Processing a result of an accepted presence check, {}", ownerId);

        Image photo = result.getPhoto();
        if (photo == null) {
            logger.error("Missing person photo from presence verification, {}", ownerId);
            throw new PresenceCheckException("Missing person photo from presence verification");
        }
        logger.debug("Obtained a photo from the result, {}", ownerId);

        SubmittedDocument submittedDoc = new SubmittedDocument();
        // TODO use different random id approach
        submittedDoc.setDocumentId(
                Ascii.truncate("selfie-photo-" + ownerId.getActivationId(), 36, "...")
        );
        submittedDoc.setPhoto(photo);
        submittedDoc.setType(DocumentType.SELFIE_PHOTO);

        DocumentVerificationEntity docVerificationEntity = new DocumentVerificationEntity();
        docVerificationEntity.setActivationId(ownerId.getActivationId());
        docVerificationEntity.setIdentityVerification(idVerification);
        docVerificationEntity.setFilename(result.getPhoto().getFilename());
        docVerificationEntity.setTimestampCreated(ownerId.getTimestamp());
        docVerificationEntity.setType(DocumentType.SELFIE_PHOTO);
        docVerificationEntity.setUsedForVerification(true);

        if (identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()) {
            docVerificationEntity.setStatus(DocumentStatus.VERIFICATION_PENDING);
        } else {
            docVerificationEntity.setStatus(DocumentStatus.ACCEPTED);
        }

        DocumentSubmitResult documentSubmitResult =
                documentProcessingService.submitDocumentToProvider(ownerId, docVerificationEntity, submittedDoc);
        docVerificationEntity.setTimestampUploaded(ownerId.getTimestamp());
        docVerificationEntity.setUploadId(documentSubmitResult.getUploadId());

        documentVerificationRepository.save(docVerificationEntity);

        documentVerificationRepository.setVerificationPending(ownerId.getActivationId(), ownerId.getTimestamp());

        return result;
    }

    /**
     * Cleans identity data used in the presence check process.
     *
     * @param apiAuthentication Authentication object.
     * @throws PresenceCheckException When an error during cleanup occurred.
     */
    public void cleanup(PowerAuthApiAuthentication apiAuthentication) throws PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        if (identityVerificationConfig.isPresenceCheckCleanupEnabled()) {
            presenceCheckProvider.cleanupIdentityData(ownerId);
        } else {
            logger.debug("Skipped cleanup of presence check data at the provider (not enabled), {}", ownerId);
        }
    }

    /**
     * Starts new presence check process.
     *
     * @param ownerId Owner identification.
     * @param idVerification Verification identity.
     * @return Session info with data needed to perform the presence check process
     * @throws PresenceCheckException When an error during starting the presence check process occurred.
     */
    private SessionInfo startPresenceCheck(OwnerId ownerId, IdentityVerificationEntity idVerification) throws PresenceCheckException {
        SessionInfo sessionInfo = presenceCheckProvider.startPresenceCheck(ownerId);

        String sessionInfoJson = jsonSerializationService.serialize(sessionInfo);
        if (sessionInfoJson == null) {
            logger.error("JSON serialization of session info failed, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        idVerification.setSessionInfo(sessionInfoJson);
        idVerification.setPhase(IdentityVerificationPhase.PRESENCE_CHECK);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());

        return sessionInfo;
    }

    /**
     * Fetches a current identity verification for presence check initialization
     * @param ownerId Owner identification.
     * @return Verification identity ready to be initialized for the presence check.
     * @throws PresenceCheckException When an error during validating the identity verification status occurred.
     */
    private IdentityVerificationEntity fetchIdVerification(OwnerId ownerId) throws PresenceCheckException {
        Optional<IdentityVerificationEntity> idVerificationOptional = identityVerificationService.findBy(ownerId);
        if (!idVerificationOptional.isPresent()) {
            logger.error("No identity verification entity found to initialize the presence check, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        IdentityVerificationEntity idVerification = idVerificationOptional.get();

        if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase()) &&
                IdentityVerificationStatus.ACCEPTED.equals(idVerification.getStatus())) {
            logger.error("The presence check is already accepted, not allowed to initialize it again, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        } else if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase()) &&
                IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            logger.info("The presence check is currently in progress, ready to be initialized again, {}", ownerId);
        } else if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase()) &&
                IdentityVerificationStatus.REJECTED.equals(idVerification.getStatus())) {
            logger.info("The presence check is rejected, ready to be initialized again, {}", ownerId);
        } else if (!IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())) {
            logger.error("The verification phase is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationPhase.DOCUMENT_UPLOAD, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        } else if (!IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            logger.error("The verification status is {} but expected {}, {}",
                    idVerification.getStatus(), IdentityVerificationStatus.VERIFICATION_PENDING, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        return idVerification;
    }

}
