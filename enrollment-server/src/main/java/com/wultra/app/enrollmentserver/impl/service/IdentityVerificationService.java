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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private final DocumentVerificationProvider documentVerificationProvider;

    /**
     * Service constructor.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentProcessingService Document processing service.
     * @param documentVerificationProvider Document verification provider.
     */
    @Autowired
    public IdentityVerificationService(
            DocumentDataRepository documentDataRepository,
            DocumentVerificationRepository documentVerificationRepository,
            IdentityVerificationRepository identityVerificationRepository,
            DocumentProcessingService documentProcessingService,
            DocumentVerificationProvider documentVerificationProvider) {
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.identityVerificationRepository = identityVerificationRepository;

        this.documentProcessingService = documentProcessingService;

        this.documentVerificationProvider = documentVerificationProvider;
    }

    Optional<IdentityVerificationEntity> findBy(OwnerId ownerId) {
        return identityVerificationRepository.findByActivationId(ownerId.getActivationId());
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document verification entities.
     */
    @Transactional
    public List<DocumentVerificationEntity> submitDocuments(DocumentSubmitRequest request,
                                                            PowerAuthApiAuthentication apiAuthentication)
            throws DocumentSubmitException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        // Find an already existing identity verification
        Optional<IdentityVerificationEntity> idVerificationOptional = findBy(ownerId);

        IdentityVerificationEntity idVerification;
        idVerification = idVerificationOptional.orElseGet(() -> createIdentityVerification(ownerId));

        if (!IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())) {
            logger.error("The verification phase is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationPhase.DOCUMENT_UPLOAD, ownerId
            );
            throw new DocumentSubmitException("Not allowed submit of documents during not upload phase");
        }

        if (IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            logger.info("Switching {} from {} to {} due to new documents submit, {}",
                    idVerification, IdentityVerificationStatus.VERIFICATION_PENDING, IdentityVerificationStatus.IN_PROGRESS, ownerId
            );
            identityVerificationRepository.setVerificationPhaseAndStatus(
                    ownerId.getActivationId(),
                    IdentityVerificationPhase.DOCUMENT_UPLOAD,
                    IdentityVerificationStatus.IN_PROGRESS,
                    ownerId.getTimestamp()
            );
        } else if (!IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
            logger.error("The verification status is {} but expected {}, {}",
                    idVerification.getStatus(), IdentityVerificationStatus.IN_PROGRESS, ownerId
            );
            throw new DocumentSubmitException("Not allowed submit of documents during not in progress status");
        }

        return documentProcessingService.submitDocuments(idVerification, request, apiAuthentication);
    }

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
                documentVerificationRepository.findAllPendingVerifications(ownerId.getActivationId());
        List<String> uploadIds = docVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .collect(Collectors.toList());

        // TODO find and fill relations between both sides of an id document

        DocumentsVerificationResult result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);

        identityVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);
        identityVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        identityVerificationRepository.save(identityVerification);

        docVerifications.forEach(docVerification -> {
            docVerification.setVerificationId(result.getVerificationId());
            docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        });
        documentVerificationRepository.saveAll(docVerifications);
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

        List<String> documentIds = request.getFilter().stream()
                .map(DocumentStatusRequest.DocumentFilter::getDocumentId)
                .collect(Collectors.toList());

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
        List<DocumentVerificationEntity> entities =
                Streamable.of(documentVerificationRepository.findAllById(documentIds)).toList();
        for (DocumentVerificationEntity entity : entities) {
            if (!entity.getVerificationId().equals(idVerification.getId())) {
                logger.error("Not related {} to {}, {}", entity, idVerification, ownerId);
                response.setStatus(IdentityVerificationStatus.FAILED);
                return response;
            }
        }

        // Check statuses of all documents used for the verification, update identity verification status accordingly
        if (IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())
                && IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
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
            }
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

    public Image getPhotoById(String photoId) throws DocumentVerificationException {
        return documentVerificationProvider.getPhoto(photoId);
    }

    private IdentityVerificationEntity createIdentityVerification(OwnerId ownerId) {
        IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
        entity.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUserId(ownerId.getUserId());
        return identityVerificationRepository.save(entity);
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
