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

import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.*;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.onboardingserver.provider.PresenceCheckProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
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
    private final PresenceCheckLimitService presenceCheckLimitService;

    /**
     * Service constructor.
     * @param documentVerificationRepository Document verification repository.
     * @param documentProcessingService Document processing service.
     * @param identityVerificationService Identity verification service.
     * @param jsonSerializationService JSON serialization service.
     * @param presenceCheckProvider Presence check provider.
     * @param presenceCheckLimitService Presence check limit service.
     */
    @Autowired
    public PresenceCheckService(
            IdentityVerificationConfig identityVerificationConfig,
            DocumentVerificationRepository documentVerificationRepository,
            DocumentProcessingService documentProcessingService,
            IdentityVerificationService identityVerificationService,
            JsonSerializationService jsonSerializationService,
            PresenceCheckProvider presenceCheckProvider, PresenceCheckLimitService presenceCheckLimitService) {
        this.identityVerificationConfig = identityVerificationConfig;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckProvider = presenceCheckProvider;
        this.presenceCheckLimitService = presenceCheckLimitService;
    }

    /**
     * Prepares presence check to not initialized state.
     *
     * @param ownerId Owner identification.
     * @param idVerification Identity verification entity.
     */
    @Transactional
    public void prepareNotInitialized(OwnerId ownerId, IdentityVerificationEntity idVerification) {
        idVerification.setPhase(IdentityVerificationPhase.PRESENCE_CHECK);
        idVerification.setStatus(IdentityVerificationStatus.NOT_INITIALIZED);
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        logger.debug("Changed {} to not initialized presence check, {}", idVerification, ownerId);
    }

    /**
     * Initializes presence check process.
     *
     * @param ownerId Owner identification.
     * @param processId Process identifier.
     * @return Session info with data needed to perform the presence check process
     * @throws DocumentVerificationException Thrown when an error during obtaining the user personal image occurred
     * @throws PresenceCheckException Thrown when an error during initializing the presence check occurred
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws PresenceCheckLimitException Thrown when presence check limit is exceeded.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     */
    @Transactional
    public SessionInfo init(OwnerId ownerId, String processId)
            throws DocumentVerificationException, PresenceCheckException, IdentityVerificationException, PresenceCheckLimitException, RemoteCommunicationException, OnboardingProcessLimitException {

        presenceCheckLimitService.checkPresenceCheckMaxAttemptLimit(ownerId, processId);

        IdentityVerificationEntity idVerification = fetchIdVerification(ownerId);

        if (!idVerification.isPresenceCheckInitialized()) {
            List<DocumentVerificationEntity> docsWithPhoto = documentVerificationRepository.findAllWithPhoto(idVerification);
            if (docsWithPhoto.isEmpty()) {
                logger.error("Missing person photo to initialize presence check, {}", ownerId);
                throw new PresenceCheckException("Unable to initialize presence check");
            } else {
                Image photo = selectPhotoForPresenceCheck(ownerId, docsWithPhoto);
                presenceCheckProvider.initPresenceCheck(ownerId, photo);
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
     * @throws PresenceCheckException When an error during the presence check verification occurred
     */
    @Transactional
    public void checkPresenceVerification(OwnerId ownerId,
                                          IdentityVerificationEntity idVerification,
                                          SessionInfo sessionInfo) throws PresenceCheckException {
        PresenceCheckResult result = presenceCheckProvider.getResult(ownerId, sessionInfo);

        if (!PresenceCheckStatus.ACCEPTED.equals(result.getStatus())) {
            logger.info("Not accepted presence check, {}", ownerId);
            evaluatePresenceCheckResult(ownerId, idVerification, result);
            return;
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
        docVerificationEntity.setUsedForVerification(identityVerificationConfig.isVerifySelfieWithDocumentsEnabled());

        DocumentSubmitResult documentSubmitResult =
                documentProcessingService.submitDocumentToProvider(ownerId, docVerificationEntity, submittedDoc);
        docVerificationEntity.setTimestampUploaded(ownerId.getTimestamp());
        docVerificationEntity.setUploadId(documentSubmitResult.getUploadId());

        documentVerificationRepository.save(docVerificationEntity);

        evaluatePresenceCheckResult(ownerId, idVerification, result);
    }

    /**
     * Cleans identity data used in the presence check process.
     *
     * @param ownerId Owner identification.
     * @throws PresenceCheckException When an error during cleanup occurred.
     */
    public void cleanup(OwnerId ownerId) throws PresenceCheckException {
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
     * Selects person photo for the presence check process
     * @param ownerId Owner identification.
     * @param docsWithPhoto Documents with a mined person photography.
     * @return Image with a person photography
     * @throws DocumentVerificationException When an error during obtaining a person photo occurred.
     */
    public Image selectPhotoForPresenceCheck(OwnerId ownerId, List<DocumentVerificationEntity> docsWithPhoto) throws DocumentVerificationException {
        docsWithPhoto.forEach(docWithPhoto ->
                Preconditions.checkNotNull(docWithPhoto.getPhotoId(), "Expected photoId value in " + docWithPhoto)
        );

        DocumentVerificationEntity preferredDocWithPhoto = null;
        for (DocumentType documentType : DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO) {
            Optional<DocumentVerificationEntity> docEntity = docsWithPhoto.stream()
                    .filter(value -> documentType.equals(value.getType()))
                    .findFirst();
            if (docEntity.isPresent()) {
                preferredDocWithPhoto = docEntity.get();
                break;
            }
        }
        if (preferredDocWithPhoto == null) {
            logger.warn("Unable to select a source of person photo to initialize presence check, {}", ownerId);
            preferredDocWithPhoto = docsWithPhoto.get(0);
        }
        logger.info("Selected {} as the source of person photo, {}", preferredDocWithPhoto, ownerId);
        String photoId = preferredDocWithPhoto.getPhotoId();
        return identityVerificationService.getPhotoById(photoId);
    }

    private void evaluatePresenceCheckResult(OwnerId ownerId,
                                             IdentityVerificationEntity idVerification,
                                             PresenceCheckResult result) {
        switch (result.getStatus()) {
            case ACCEPTED:
                idVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.info("Presence check accepted, {}", ownerId);
                break;
            case FAILED:
                idVerification.setErrorDetail(result.getErrorDetail());
                idVerification.setErrorOrigin(ErrorOrigin.PRESENCE_CHECK);
                idVerification.setStatus(IdentityVerificationStatus.FAILED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.warn("Presence check failed, {}, errorDetail: '{}'", ownerId, result.getErrorDetail());
                break;
            case IN_PROGRESS:
                logger.debug("Presence check still in progress, {}", ownerId);
                break;
            case REJECTED:
                idVerification.setRejectReason(result.getRejectReason());
                idVerification.setRejectOrigin(RejectOrigin.PRESENCE_CHECK);
                idVerification.setStatus(IdentityVerificationStatus.REJECTED);
                idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.info("Presence check rejected, {}, rejectReason: '{}'", ownerId, result.getRejectReason());
                break;
            default:
                throw new IllegalStateException("Unexpected presence check result status: " + result.getStatus());
        }
    }

    /**
     * Fetches a current identity verification for presence check initialization
     * @param ownerId Owner identification.
     * @return Verification identity ready to be initialized for the presence check.
     * @throws PresenceCheckException When an error during validating the identity verification status occurred.
     */
    private IdentityVerificationEntity fetchIdVerification(OwnerId ownerId) throws PresenceCheckException {
        Optional<IdentityVerificationEntity> idVerificationOptional = identityVerificationService.findByOptional(ownerId);
        if (idVerificationOptional.isEmpty()) {
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
        } else if (!IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase())) {
            logger.error("The verification phase is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationPhase.PRESENCE_CHECK, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        } else if (!IdentityVerificationStatus.NOT_INITIALIZED.equals(idVerification.getStatus())) {
            logger.error("The verification status is {} but expected {}, {}",
                    idVerification.getStatus(), IdentityVerificationStatus.NOT_INITIALIZED, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        return idVerification;
    }

}
