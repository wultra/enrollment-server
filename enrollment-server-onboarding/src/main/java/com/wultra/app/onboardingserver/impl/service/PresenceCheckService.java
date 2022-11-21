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
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessLimitException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckLimitException;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.onboardingserver.provider.PresenceCheckProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;

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
    private final AuditService auditService;

    /**
     * Service constructor.
     * @param documentVerificationRepository Document verification repository.
     * @param documentProcessingService Document processing service.
     * @param identityVerificationService Identity verification service.
     * @param jsonSerializationService JSON serialization service.
     * @param presenceCheckProvider Presence check provider.
     * @param presenceCheckLimitService Presence check limit service.
     * @param auditService Audit service.
     */
    @Autowired
    public PresenceCheckService(
            final IdentityVerificationConfig identityVerificationConfig,
            final DocumentVerificationRepository documentVerificationRepository,
            final DocumentProcessingService documentProcessingService,
            final IdentityVerificationService identityVerificationService,
            final JsonSerializationService jsonSerializationService,
            final PresenceCheckProvider presenceCheckProvider,
            final PresenceCheckLimitService presenceCheckLimitService,
            final AuditService auditService) {

        this.identityVerificationConfig = identityVerificationConfig;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckProvider = presenceCheckProvider;
        this.presenceCheckLimitService = presenceCheckLimitService;
        this.auditService = auditService;
    }

    /**
     * Prepares presence check to not initialized state.
     *
     * @param ownerId Owner identification.
     * @param idVerification Identity verification entity.
     */
    @Transactional
    public void prepareNotInitialized(OwnerId ownerId, IdentityVerificationEntity idVerification) {
        identityVerificationService.moveToPhaseAndStatus(idVerification, PRESENCE_CHECK, NOT_INITIALIZED, ownerId);
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
                throw new PresenceCheckException("Unable to initialize presence check - missing person photo, " + ownerId);
            } else {
                final Image photo = selectPhotoForPresenceCheck(ownerId, docsWithPhoto);
                presenceCheckProvider.initPresenceCheck(ownerId, photo);
                logger.info("Presence check initialized, {}", ownerId);
                auditService.auditPresenceCheckProvider(idVerification, "Presence check initialized for user: {}", ownerId.getUserId());
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
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    @Transactional
    public void checkPresenceVerification(OwnerId ownerId,
                                          IdentityVerificationEntity idVerification,
                                          SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException {
        final PresenceCheckResult result = presenceCheckProvider.getResult(ownerId, updateSessionInfo(ownerId, idVerification, sessionInfo));
        auditService.auditPresenceCheckProvider(idVerification, "Got presence check result: {} for user: {}", result.getStatus(), ownerId.getUserId());

        if (result.getStatus() != PresenceCheckStatus.ACCEPTED) {
            logger.info("Not accepted presence check, status: {}, {}", result.getStatus(), ownerId);
            evaluatePresenceCheckResult(ownerId, idVerification, result);
            return;
        }
        logger.debug("Processing a result of an accepted presence check, {}", ownerId);

        final Image photo = result.getPhoto();
        if (photo == null) {
            throw new PresenceCheckException("Missing person photo from presence verification, " + ownerId);
        }
        logger.debug("Obtained a photo from the result, {}", ownerId);

        final SubmittedDocument submittedDoc = new SubmittedDocument();
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
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    public void cleanup(OwnerId ownerId) throws PresenceCheckException, RemoteCommunicationException {
        if (identityVerificationConfig.isPresenceCheckCleanupEnabled()) {
            presenceCheckProvider.cleanupIdentityData(ownerId);
            final IdentityVerificationEntity identityVerification = identityVerificationService.findByOptional(ownerId).orElseThrow(() ->
                    new PresenceCheckException("Unable to find identity verification for " + ownerId));
            auditService.auditPresenceCheckProvider(identityVerification, "Clean up presence check data for user: {}", ownerId.getUserId());
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
     * @throws PresenceCheckException In case of business logic error.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    private SessionInfo startPresenceCheck(OwnerId ownerId, IdentityVerificationEntity idVerification) throws PresenceCheckException, RemoteCommunicationException {
        final SessionInfo sessionInfo = presenceCheckProvider.startPresenceCheck(ownerId);
        logger.info("Presence check started, {}", ownerId);
        auditService.auditPresenceCheckProvider(idVerification, "Presence check started for user: {}", ownerId.getUserId());

        String sessionInfoJson = jsonSerializationService.serialize(sessionInfo);
        if (sessionInfoJson == null) {
            throw new PresenceCheckException("JSON serialization of session info failed, " + ownerId);
        }

        idVerification.setSessionInfo(sessionInfoJson);
        identityVerificationService.moveToPhaseAndStatus(idVerification, PRESENCE_CHECK, IN_PROGRESS, ownerId);

        return sessionInfo;
    }

    /**
     * Selects person photo for the presence check process
     * @param ownerId Owner identification.
     * @param docsWithPhoto Documents with a mined person photography.
     * @return Image with a person photography
     * @throws RemoteCommunicationException In case of remote communication error.
     * @throws DocumentVerificationException In case of business logic error.
     */
    public Image selectPhotoForPresenceCheck(OwnerId ownerId, List<DocumentVerificationEntity> docsWithPhoto) throws DocumentVerificationException, RemoteCommunicationException {
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
        return identityVerificationService.getPhotoById(photoId, ownerId);
    }

    private void evaluatePresenceCheckResult(OwnerId ownerId,
                                             IdentityVerificationEntity idVerification,
                                             PresenceCheckResult result) {

        final IdentityVerificationPhase phase = idVerification.getPhase();
        switch (result.getStatus()) {
            case ACCEPTED:
                // The timestampFinished parameter is not set yet, there may be other steps ahead
                identityVerificationService.moveToPhaseAndStatus(idVerification, phase, ACCEPTED, ownerId);
                break;
            case FAILED:
                idVerification.setErrorDetail(IdentityVerificationEntity.PRESENCE_CHECK_REJECTED);
                idVerification.setErrorOrigin(ErrorOrigin.PRESENCE_CHECK);
                idVerification.setTimestampFailed(ownerId.getTimestamp());
                identityVerificationService.moveToPhaseAndStatus(idVerification, phase, FAILED, ownerId);
                logger.warn("Presence check failed, {}, errorDetail: '{}'", ownerId, result.getErrorDetail());
                break;
            case IN_PROGRESS:
                logger.debug("Presence check still in progress, {}", ownerId);
                break;
            case REJECTED:
                idVerification.setRejectReason(IdentityVerificationEntity.PRESENCE_CHECK_REJECTED);
                idVerification.setRejectOrigin(RejectOrigin.PRESENCE_CHECK);
                idVerification.setTimestampFinished(ownerId.getTimestamp());
                logger.info("Presence check rejected, {}, rejectReason: '{}'", ownerId, result.getRejectReason());
                identityVerificationService.moveToPhaseAndStatus(idVerification, phase, REJECTED, ownerId);
                break;
            default:
                throw new IllegalStateException(String.format("Unexpected presence check result status: %s, identity verification ID: %s",
                        result.getStatus(), idVerification.getId()));
        }
    }

    /**
     * Fetches a current identity verification for presence check initialization
     * @param ownerId Owner identification.
     * @return Verification identity ready to be initialized for the presence check.
     * @throws PresenceCheckException When an error during validating the identity verification status occurred.
     */
    private IdentityVerificationEntity fetchIdVerification(OwnerId ownerId) throws PresenceCheckException {
        final IdentityVerificationEntity idVerification = identityVerificationService.findByOptional(ownerId).orElseThrow(() ->
                new PresenceCheckException("No identity verification entity found to initialize the presence check, " + ownerId));

        final IdentityVerificationPhase phase = idVerification.getPhase();
        final IdentityVerificationStatus status = idVerification.getStatus();

        if (phase == PRESENCE_CHECK && status == ACCEPTED) {
            throw new PresenceCheckException("The presence check is already accepted, not allowed to initialize it again, " + ownerId);
        } else if (phase == PRESENCE_CHECK && status == IN_PROGRESS) {
            logger.info("The presence check is currently in progress, ready to be initialized again, {}", ownerId);
        } else if (phase == PRESENCE_CHECK && status == REJECTED) {
            logger.info("The presence check is rejected, ready to be initialized again, {}", ownerId);
        } else if (phase != PRESENCE_CHECK) {
            throw new PresenceCheckException(String.format("The verification phase is %s but expected PRESENCE_CHECK, %s", phase, ownerId));
        } else if (status != NOT_INITIALIZED) {
            throw new PresenceCheckException(String.format("The verification status is %s but expected NOT_INITIALIZED, %s", status, ownerId));
        }

        return idVerification;
    }

    private SessionInfo updateSessionInfo(final OwnerId ownerId, final IdentityVerificationEntity identityVerification, final SessionInfo sessionInfo) {
        sessionInfo.getSessionAttributes().put("timestampLastUsed", ownerId.getTimestamp());
        identityVerification.setSessionInfo(jsonSerializationService.serialize(sessionInfo));
        return sessionInfo;
    }

}
