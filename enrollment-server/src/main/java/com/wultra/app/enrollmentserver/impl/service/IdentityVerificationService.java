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

import com.wultra.app.enrollmentserver.configuration.OnboardingConfig;
import com.wultra.app.enrollmentserver.database.*;
import com.wultra.app.enrollmentserver.database.entity.*;
import com.wultra.app.enrollmentserver.errorhandling.DocumentSubmitException;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.util.PowerAuthUtil;
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.model.request.DocumentStatusRequest;
import com.wultra.app.enrollmentserver.model.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.model.request.IdentityVerificationStatusRequest;
import com.wultra.app.enrollmentserver.model.response.DocumentStatusResponse;
import com.wultra.app.enrollmentserver.model.response.DocumentUploadResponse;
import com.wultra.app.enrollmentserver.model.response.IdentityVerificationStatusResponse;
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

    private final OnboardingConfig onboardingConfig;

    private final DataExtractionService dataExtractionService;
    private final DocumentDataRepository documentDataRepository;
    private final DocumentVerificationRepository documentVerificationRepository;
    private final DocumentVerificationResultRepository documentVerificationResultRepository;
    private final IdentityDocumentRepository identityDocumentRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    private final DocumentVerificationProvider documentVerificationProvider;

    /**
     * Service constructor.
     * @param dataExtractionService Data extraction service.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param documentVerificationResultRepository Document verification result repository.
     * @param identityDocumentRepository Identity document repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentVerificationProvider Document verification provider.
     */
    @Autowired
    public IdentityVerificationService(
            OnboardingConfig onboardingConfig,
            DataExtractionService dataExtractionService,
            DocumentDataRepository documentDataRepository,
            DocumentVerificationRepository documentVerificationRepository,
            DocumentVerificationResultRepository documentVerificationResultRepository,
            IdentityDocumentRepository identityDocumentRepository,
            IdentityVerificationRepository identityVerificationRepository,
            DocumentVerificationProvider documentVerificationProvider) {
        this.onboardingConfig = onboardingConfig;

        this.dataExtractionService = dataExtractionService;
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentVerificationResultRepository = documentVerificationResultRepository;
        this.identityDocumentRepository = identityDocumentRepository;
        this.identityVerificationRepository = identityVerificationRepository;

        this.documentVerificationProvider = documentVerificationProvider;
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

        Optional<IdentityVerificationEntity> optionalIdentityVerification =
                identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (optionalIdentityVerification.isPresent()) {
            IdentityVerificationEntity identityVerificationEntity = optionalIdentityVerification.get();
            response.setIdentityVerificationStatus(identityVerificationEntity.getStatus());
            response.setIdentityVerificationPhase(identityVerificationEntity.getPhase());
        } else {
            logger.error("Checking identity verification status on not existing entity, {}", ownerId);
            response.setIdentityVerificationStatus(IdentityVerificationStatus.FAILED);
        }

        return response;
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document verification entities.
     */
    @Transactional
    public List<DocumentVerificationEntity> submitDocuments(DocumentSubmitRequest request, PowerAuthApiAuthentication apiAuthentication) {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        // TODO resubmit documents, replace existing with the new ones
        // Find an already existing identity verification
        Optional<IdentityVerificationEntity> optionalIdentityVerification =
                identityVerificationRepository.findByActivationIdAndPhaseAndStatus(
                        ownerId.getActivationId(),
                        IdentityVerificationPhase.DOCUMENT_UPLOAD,
                        IdentityVerificationStatus.IN_PROGRESS);

        IdentityVerificationEntity identityVerificationEntity;
        if (optionalIdentityVerification.isPresent()) {
            identityVerificationEntity = optionalIdentityVerification.get();
            // TODO get existing documents
        } else {
            identityVerificationEntity = createIdentityVerification(ownerId);
        }

        List<Document> documents = getDocuments(ownerId, request);

        List<DocumentVerificationEntity> docVerificationEntities = new ArrayList<>();
        List<DocumentResultEntity> docResults = new ArrayList<>();

        List<DocumentSubmitRequest.DocumentMetadata> docsMetadata = request.getDocuments();
        for (DocumentSubmitRequest.DocumentMetadata docMetadata : docsMetadata) {
            DocumentVerificationEntity docVerification = createDocumentVerificationEntity(ownerId, docMetadata);
            docVerificationEntities.add(docVerification);

            SubmittedDocument submittedDoc;
            try {
                submittedDoc = createSubmittedDocument(ownerId, docMetadata, documents);
            } catch (DocumentSubmitException e) {
                docVerification.setStatus(DocumentStatus.FAILED);
                docVerification.setErrorDetail(e.getMessage());
                continue;
            }
            Date now = new Date();

            DocumentSubmitResult docSubmitResult = submitDocumentToProvider(ownerId, docVerification, submittedDoc);

            DocumentResultEntity docResult = createDocumentResult(docVerification, docSubmitResult);
            docResult.setTimestampCreated(now);
            docResults.add(docResult);

            // Delete previously persisted document after a successful upload to the provider
            if (DocumentStatus.VERIFICATION_PENDING.equals(docVerification.getStatus()) && docMetadata.getUploadId() != null) {
                documentDataRepository.deleteById(docMetadata.getUploadId());
            }
        }

        // TODO find the other sides of documents

        documentVerificationRepository.saveAll(docVerificationEntities);

        for (int i = 0; i < docVerificationEntities.size(); i++) {
            DocumentVerificationEntity docVerificationEntity = docVerificationEntities.get(i);
            docResults.get(i).setDocumentVerificationId(docVerificationEntity.getId());
        }
        documentVerificationResultRepository.saveAll(docResults);

        List<IdentityDocumentEntity> identityDocumentEntities = createIdentityDocumentEntities(
                identityVerificationEntity, docVerificationEntities
        );
        identityDocumentRepository.saveAll(identityDocumentEntities);

        return docVerificationEntities;
    }

    public DocumentSubmitResult submitDocumentToProvider(OwnerId ownerId, DocumentVerificationEntity docVerification, SubmittedDocument submittedDoc) {
        DocumentsSubmitResult docsSubmitResults;
        DocumentSubmitResult docSubmitResult;
        try {
            docsSubmitResults = documentVerificationProvider.submitDocuments(ownerId, List.of(submittedDoc));
            docSubmitResult = docsSubmitResults.getResults().get(0);
        } catch (DocumentVerificationException e) {
            docsSubmitResults = new DocumentsSubmitResult();
            docSubmitResult = new DocumentSubmitResult();
            docSubmitResult.setErrorDetail(e.getMessage());
        }

        if (docSubmitResult.getErrorDetail() != null) {
            docVerification.setStatus(DocumentStatus.FAILED);
            docVerification.setErrorDetail(docSubmitResult.getErrorDetail());
        } else if (docSubmitResult.getRejectReason() != null) {
            docVerification.setStatus(DocumentStatus.REJECTED);
            docVerification.setRejectReason(docSubmitResult.getRejectReason());
        } else {
            docVerification.setPhotoId(docsSubmitResults.getExtractedPhotoId());
            docVerification.setProviderName(onboardingConfig.getDocumentVerificationProvider());
            docVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
            docVerification.setTimestampUploaded(new Date());
            docVerification.setUploadId(docSubmitResult.getUploadId());
        }
        return docSubmitResult;
    }

    private DocumentResultEntity createDocumentResult(
            DocumentVerificationEntity docVerificationEntity,
            DocumentSubmitResult docSubmitResult) {
        DocumentResultEntity entity = new DocumentResultEntity();
        entity.setErrorDetail(docVerificationEntity.getErrorDetail());
        entity.setExtractedData(docSubmitResult.getExtractedData());
        entity.setPhase(DocumentProcessingPhase.UPLOAD);
        entity.setRejectReason(docVerificationEntity.getRejectReason());
        return entity;
    }

    /**
     * Upload a single document related to identity verification.
     * @param requestData Binary document data.
     * @return Document upload response.
     * @throws DocumentVerificationException Thrown when document is invalid.
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(byte[] requestData) throws DocumentVerificationException {
        Document document = dataExtractionService.extractDocument(requestData);
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setFilename(document.getFilename());
        response.setId(document.getId());
        return response;
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

        Optional<IdentityVerificationEntity> optionalIdentityVerification =
                identityVerificationRepository.findByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (!optionalIdentityVerification.isPresent()) {
            logger.error("Checking identity verification status on a not existing entity, {}", ownerId);
            response.setStatus(IdentityVerificationStatus.FAILED);
            return response;
        }

        IdentityVerificationEntity identityVerification = optionalIdentityVerification.get();

        // Ensure that all entities are related to the identity verification
        List<DocumentVerificationEntity> entities =
                Streamable.of(documentVerificationRepository.findAllById(documentIds)).toList();
        for (DocumentVerificationEntity entity : entities) {
            if (!entity.getVerificationId().equals(identityVerification.getId())) {
                logger.error("Not related {} to {}, {}", entity, identityVerification, ownerId);
                response.setStatus(IdentityVerificationStatus.FAILED);
                return response;
            }
        }

        // TODO check updated data at provider (as regular job)

        List<DocumentStatusResponse.DocumentMetadata> docsMetadata = createDocsMetadata(entities);
        response.setStatus(identityVerification.getStatus());
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
        documentVerificationRepository.failInProgressVerifications(ownerId.getActivationId());
        // Set status of all in-progress identity verifications to failed
        identityVerificationRepository.failInProgressVerifications(ownerId.getActivationId());
    }

    public Image getPhotoById(String photoId) throws DocumentVerificationException {
        return documentVerificationProvider.getPhoto(photoId);
    }

    private IdentityVerificationEntity createIdentityVerification(OwnerId ownerId) {
        IdentityVerificationEntity entity = new IdentityVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);
        entity.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        entity.setTimestampCreated(new Date());
        entity.setUserId(ownerId.getUserId());
        return identityVerificationRepository.save(entity);
    }

    private List<Document> getDocuments(OwnerId ownerId, DocumentSubmitRequest request) {
        List<Document> documents;
        if (request.getData() == null) {
            documents = Collections.emptyList();
        } else {
            try {
                documents = dataExtractionService.extractDocuments(request.getData());
            } catch (DocumentVerificationException e) {
                logger.error("Unable to extract documents from {}, {}", request, ownerId);
                documents = Collections.emptyList();
            }
        }
        return documents;
    }

    private DocumentVerificationEntity createDocumentVerificationEntity(OwnerId ownerId, DocumentSubmitRequest.DocumentMetadata docMetadata) {
        DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setActivationId(ownerId.getActivationId());
        documentVerification.setFilename(docMetadata.getFilename());
        documentVerification.setOriginalDocumentId(docMetadata.getOriginalDocumentId());
        documentVerification.setSide(docMetadata.getSide());
        documentVerification.setType(docMetadata.getType());
        documentVerification.setStatus(DocumentStatus.UPLOAD_IN_PROGRESS);
        documentVerification.setTimestampCreated(new Date());
        return documentVerificationRepository.save(documentVerification);
    }

    private List<IdentityDocumentEntity> createIdentityDocumentEntities(
            IdentityVerificationEntity identityVerificationEntity,
            List<DocumentVerificationEntity> documentVerificationEntities) {
        return documentVerificationEntities.stream()
                .map(document -> {
                    IdentityDocumentEntity.IdentityDocumentKey key = new IdentityDocumentEntity.IdentityDocumentKey();
                    key.setIdentityId(identityVerificationEntity.getId());
                    key.setDocumentId(document.getId());

                    IdentityDocumentEntity entity = new IdentityDocumentEntity();
                    entity.setId(key);

                    return entity;
                }).collect(Collectors.toList());
    }

    private SubmittedDocument createSubmittedDocument(
            OwnerId ownerId,
            DocumentSubmitRequest.DocumentMetadata docMetadata,
            List<Document> docs) throws DocumentSubmitException {
        Image photo = new Image();
        photo.setFilename(docMetadata.getFilename());

        SubmittedDocument submittedDoc = new SubmittedDocument();
        submittedDoc.setDocumentId(docMetadata.getUploadId());
        submittedDoc.setPhoto(photo);
        submittedDoc.setSide(docMetadata.getSide());
        submittedDoc.setType(docMetadata.getType());

        if (docMetadata.getUploadId() == null) {
            Optional<Document> optionalDoc = docs.stream()
                    .filter(doc -> doc.getFilename().equals(docMetadata.getFilename()))
                    .findFirst();
            if (optionalDoc.isPresent()) {
                photo.setData(optionalDoc.get().getData());
            } else {
                logger.error("Missing {} in data, {}", docMetadata, ownerId);
                throw new DocumentSubmitException("Not found document data in sent request");
            }
        } else {
            Optional<DocumentDataEntity> optionalDocData =
                    documentDataRepository.findById(docMetadata.getUploadId());
            if (optionalDocData.isPresent()) {
                photo.setData(optionalDocData.get().getData());
            } else {
                logger.error("Missing {} in data, {}", docMetadata, ownerId);
                throw new DocumentSubmitException("Not found document data in saved upload");
            }
        }
        return submittedDoc;
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
