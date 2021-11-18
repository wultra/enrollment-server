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

import com.wultra.app.enrollmentserver.database.DocumentDataRepository;
import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.IdentityVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentSubmitException;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.enrollmentserver.impl.service.verification.VerificationProcessingService;
import com.wultra.app.enrollmentserver.impl.util.PowerAuthUtil;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.request.DocumentStatusRequest;
import com.wultra.app.enrollmentserver.model.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.model.response.DocumentStatusResponse;
import com.wultra.app.enrollmentserver.provider.DocumentVerificationProvider;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementing document identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class IdentityVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationService.class);

    private final DocumentDataRepository documentDataRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final DocumentProcessingService documentProcessingService;

    private final IdentityVerificationCreateService identityVerificationCreateService;

    private final VerificationProcessingService verificationProcessingService;

    private final DocumentVerificationProvider documentVerificationProvider;

    /**
     * Service constructor.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentProcessingService Document processing service.
     * @param identityVerificationCreateService Identity verification create service.
     * @param verificationProcessingService Verification processing service.
     * @param documentVerificationProvider Document verification provider.
     */
    @Autowired
    public IdentityVerificationService(
            DocumentDataRepository documentDataRepository,
            DocumentVerificationRepository documentVerificationRepository,
            IdentityVerificationRepository identityVerificationRepository,
            DocumentProcessingService documentProcessingService,
            IdentityVerificationCreateService identityVerificationCreateService,
            VerificationProcessingService verificationProcessingService,
            DocumentVerificationProvider documentVerificationProvider) {
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.identityVerificationRepository = identityVerificationRepository;

        this.documentProcessingService = documentProcessingService;
        this.identityVerificationCreateService = identityVerificationCreateService;
        this.verificationProcessingService = verificationProcessingService;
        this.documentVerificationProvider = documentVerificationProvider;
    }

    /**
     * Finds the current verification identity
     * @param ownerId Owner identification.
     * @return Optional entity of the verification identity
     */
    public Optional<IdentityVerificationEntity> findBy(OwnerId ownerId) {
        return identityVerificationRepository.findByActivationId(ownerId.getActivationId());
    }

    /**
     * Initialize identity verification.
     * @param ownerId Owner identification.
     */
    public void initializeIdentityVerification(OwnerId ownerId) throws DocumentVerificationException {
        identityVerificationCreateService.createIdentityVerification(ownerId);
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param ownerId Owner identification.
     * @return Document verification entities.
     */
    public List<DocumentVerificationEntity> submitDocuments(DocumentSubmitRequest request,
                                                            OwnerId ownerId)
            throws DocumentSubmitException {

        // Find an already existing identity verification
        Optional<IdentityVerificationEntity> idVerificationOptional = findBy(ownerId);

        if (!idVerificationOptional.isPresent()) {
            logger.error("Identity verification has not been initialized, {}", ownerId);
            throw new DocumentSubmitException("Identity verification has not been initialized");
        }

        IdentityVerificationEntity idVerification = idVerificationOptional.get();

        if (!IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())) {
            logger.error("The verification phase is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationPhase.DOCUMENT_UPLOAD, ownerId
            );
            throw new DocumentSubmitException("Not allowed submit of documents during not upload phase");
        } else if (IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            logger.info("Switching {} from {} to {} due to new documents submit, {}",
                    idVerification, IdentityVerificationStatus.VERIFICATION_PENDING, IdentityVerificationStatus.IN_PROGRESS, ownerId
            );
            idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
            idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
            idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            identityVerificationRepository.save(idVerification);
        } else if (!IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            logger.error("The verification status is {} but expected {}, {}",
                    idVerification.getStatus(), IdentityVerificationStatus.IN_PROGRESS, ownerId
            );
            throw new DocumentSubmitException("Not allowed submit of documents during not in progress status");
        }

        List<DocumentVerificationEntity> docsVerifications =
                documentProcessingService.submitDocuments(idVerification, request, ownerId);
        checkIdentityDocumentsForVerification(ownerId, idVerification);
        identityVerificationRepository.save(idVerification);
        return docsVerifications;
    }

    /**
     * Starts the verification process
     *
     * @param ownerId Owner identification.
     * @throws DocumentVerificationException When an error occurred
     */
    @Transactional
    public void startVerification(OwnerId ownerId) throws DocumentVerificationException {
        Optional<IdentityVerificationEntity> identityVerificationOptional =
                identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (!identityVerificationOptional.isPresent()) {
            logger.error("No identity verification entity found to start the verification, {}", ownerId);
            throw new DocumentVerificationException("Unable to start verification");
        }
        IdentityVerificationEntity identityVerification = identityVerificationOptional.get();

        List<DocumentVerificationEntity> docVerifications =
                documentVerificationRepository.findAllDocumentVerifications(ownerId.getActivationId(), DocumentStatus.VERIFICATION_PENDING);
        List<String> uploadIds = docVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .collect(Collectors.toList());

        // TODO find and fill relations between both sides of an id document

        DocumentsVerificationResult result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);

        identityVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);
        identityVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        identityVerification.setTimestampLastUpdated(ownerId.getTimestamp());

        docVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.VERIFICATION_IN_PROGRESS);
            docVerification.setVerificationId(result.getVerificationId());
            docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        });
        documentVerificationRepository.saveAll(docVerifications);
    }

    /**
     * Checks verification result and evaluates the final state of the identity verification process
     *
     * @param ownerId Owner identification.
     * @param idVerification Verification identity
     * @throws DocumentVerificationException When an error during verification check occurred
     */
    @Transactional
    public void checkVerificationResult(OwnerId ownerId, IdentityVerificationEntity idVerification)
            throws DocumentVerificationException {
        List<DocumentVerificationEntity> allDocVerifications =
                documentVerificationRepository.findAllDocumentVerifications(ownerId.getActivationId(), DocumentStatus.VERIFICATION_IN_PROGRESS);
        Map<String, List<DocumentVerificationEntity>> verificationsById = new HashMap<>();

        for (DocumentVerificationEntity docVerification : allDocVerifications) {
            verificationsById.computeIfAbsent(docVerification.getVerificationId(), (verificationId) -> new ArrayList<>())
                    .add(docVerification);
        }

        for (String verificationId: verificationsById.keySet()) {
            DocumentsVerificationResult docVerificationResult =
                    documentVerificationProvider.getVerificationResult(ownerId, verificationId);
            List<DocumentVerificationEntity> docVerifications = verificationsById.get(verificationId);
            verificationProcessingService.processVerificationResult(ownerId, docVerifications, docVerificationResult);
        }

        if (allDocVerifications.stream()
                .anyMatch(docVerification -> DocumentStatus.VERIFICATION_IN_PROGRESS.equals(docVerification.getStatus()))) {
            return;
        }
        if (allDocVerifications.stream()
                .allMatch(docVerification -> DocumentStatus.ACCEPTED.equals(docVerification.getStatus()))) {
            idVerification.setStatus(IdentityVerificationStatus.ACCEPTED);
        } else {
            allDocVerifications.stream()
                    .filter(docVerification -> DocumentStatus.FAILED.equals(docVerification.getStatus()))
                    .findAny()
                    .ifPresent(failed -> {
                        idVerification.setStatus(IdentityVerificationStatus.FAILED);
                        idVerification.setErrorDetail(failed.getErrorDetail());
                    });

            allDocVerifications.stream()
                    .filter(docVerification -> DocumentStatus.REJECTED.equals(docVerification.getStatus()))
                    .findAny()
                    .ifPresent(failed -> {
                        idVerification.setStatus(IdentityVerificationStatus.REJECTED);
                        idVerification.setErrorDetail(failed.getRejectReason());
                    });
        }
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        identityVerificationRepository.save(idVerification);
    }

    /**
     * Check status of document verification related to identity.
     * @param request Document status request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     */
    @Transactional
    public DocumentStatusResponse checkIdentityVerificationStatus(DocumentStatusRequest request, PowerAuthApiAuthentication apiAuthentication) {
        DocumentStatusResponse response = new DocumentStatusResponse();

        List<String> documentIds;
        if (request.getFilter() != null) {
            documentIds = request.getFilter().stream()
                    .map(DocumentStatusRequest.DocumentFilter::getDocumentId)
                    .collect(Collectors.toList());
        } else {
            documentIds = Collections.emptyList();
        }

        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        Optional<IdentityVerificationEntity> idVerificationOptional =
                identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (!idVerificationOptional.isPresent()) {
            logger.error("Checking identity verification status on a not existing entity, {}", ownerId);
            response.setStatus(IdentityVerificationStatus.FAILED);
            return response;
        }

        IdentityVerificationEntity idVerification = idVerificationOptional.get();

        // Ensure that all entities are related to the identity verification
        List<DocumentVerificationEntity> entities = Collections.emptyList();
        if (!documentIds.isEmpty()) {
            entities = Streamable.of(documentVerificationRepository.findAllById(documentIds)).toList();
            for (DocumentVerificationEntity entity : entities) {
                if (entity.getVerificationId() != null && !entity.getVerificationId().equals(idVerification.getId())) {
                    logger.error("Not related {} to {}, {}", entity, idVerification, ownerId);
                    response.setStatus(IdentityVerificationStatus.FAILED);
                    return response;
                }
            }
        }

        // Check statuses of all documents used for the verification, update identity verification status accordingly
        if (IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            checkIdentityDocumentsForVerification(ownerId, idVerification);
        }

        List<DocumentStatusResponse.DocumentMetadata> docsMetadata = createDocsMetadata(entities);
        response.setStatus(idVerification.getStatus());
        response.setDocuments(docsMetadata);

        return response;
    }

    /**
     * Cleanup documents related to identity verification.
     * @param apiAuthentication PowerAuth authentication.
     */
    @Transactional
    public void cleanup(PowerAuthApiAuthentication apiAuthentication)
            throws DocumentVerificationException, PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        List<String> uploadIds = documentVerificationRepository.findAllUploadIds(ownerId.getActivationId());

        documentVerificationProvider.cleanupDocuments(ownerId, uploadIds);

        // Delete all large documents by activation ID
        documentDataRepository.deleteAllByActivationId(ownerId.getActivationId());
        // Set status of all in-progress document verifications to failed
        documentVerificationRepository.failInProgressVerifications(ownerId.getActivationId(), ownerId.getTimestamp());
        // Set status of all in-progress identity verifications to failed
        identityVerificationRepository.failInProgressVerifications(ownerId.getActivationId(), ownerId.getTimestamp());
    }

    /**
     * Provides photo data
     * @param photoId Identification of the photo
     * @return Photo image
     * @throws DocumentVerificationException When an error occurred during
     */
    public Image getPhotoById(String photoId) throws DocumentVerificationException {
        return documentVerificationProvider.getPhoto(photoId);
    }

    /**
     * Check documents used for verification on their status
     * <p>
     *     When all of the documents are {@link DocumentStatus#VERIFICATION_PENDING} the identity verification is set
     *     also to {@link IdentityVerificationStatus#VERIFICATION_PENDING}
     * </p>
     * @param idVerification Identity verification entity.
     * @param ownerId Owner identification
     */
    @Transactional
    public void checkIdentityDocumentsForVerification(OwnerId ownerId, IdentityVerificationEntity idVerification) {
        List<DocumentVerificationEntity> docVerifications =
                documentVerificationRepository.findAllUsedForVerification(ownerId.getActivationId());

        if (docVerifications.stream()
                .allMatch(docVerification ->
                        DocumentStatus.VERIFICATION_PENDING.equals(docVerification.getStatus())
                )
        ) {
            logger.info("All documents are pending verification, changing status of {} to {}",
                    idVerification, IdentityVerificationStatus.VERIFICATION_PENDING
            );
            idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
            idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
            idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        }
    }

    private List<DocumentStatusResponse.DocumentMetadata> createDocsMetadata(List<DocumentVerificationEntity> entities) {
        List<DocumentStatusResponse.DocumentMetadata> docsMetadata = new ArrayList<>();
        entities.forEach(entity -> {
            DocumentStatusResponse.DocumentMetadata docMetadata = new DocumentStatusResponse.DocumentMetadata();
            docMetadata.setId(entity.getId());
            if (entity.getErrorDetail() != null) {
                docMetadata.setErrors(List.of(entity.getErrorDetail()));
            }
            docMetadata.setFilename(entity.getFilename());
            docMetadata.setStatus(entity.getStatus());
            docsMetadata.add(docMetadata);
        });
        return docsMetadata;
    }

}
