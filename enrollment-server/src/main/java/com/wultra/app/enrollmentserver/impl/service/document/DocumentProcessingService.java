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
package com.wultra.app.enrollmentserver.impl.service.document;

import com.wultra.app.enrollmentserver.configuration.IdentityVerificationConfig;
import com.wultra.app.enrollmentserver.database.DocumentDataRepository;
import com.wultra.app.enrollmentserver.database.DocumentResultRepository;
import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.*;
import com.wultra.app.enrollmentserver.errorhandling.DocumentSubmitException;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.impl.service.DataExtractionService;
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.DocumentMetadata;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.model.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.provider.DocumentVerificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementing document processing features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final IdentityVerificationConfig identityVerificationConfig;

    private final DocumentDataRepository documentDataRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final DocumentResultRepository documentResultRepository;

    private final DataExtractionService dataExtractionService;

    private final DocumentVerificationProvider documentVerificationProvider;

    /**
     * Service constructor.
     * @param identityVerificationConfig Identity verification configuration.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param documentResultRepository Document verification result repository.
     * @param dataExtractionService Data extraction service.
     * @param documentVerificationProvider Document verification provider.
     */
    @Autowired
    public DocumentProcessingService(
            IdentityVerificationConfig identityVerificationConfig,
            DocumentDataRepository documentDataRepository,
            DocumentVerificationRepository documentVerificationRepository,
            DocumentResultRepository documentResultRepository,
            DataExtractionService dataExtractionService,
            DocumentVerificationProvider documentVerificationProvider) {
        this.identityVerificationConfig = identityVerificationConfig;
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentResultRepository = documentResultRepository;
        this.dataExtractionService = dataExtractionService;
        this.documentVerificationProvider = documentVerificationProvider;
    }

    /**
     * Submit identity-related documents for verification.
     * @param idVerification Identity verification entity.
     * @param request Document submit request.
     * @param ownerId Owner identification.
     * @return Document verification entities.
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public List<DocumentVerificationEntity> submitDocuments(
            IdentityVerificationEntity idVerification,
            DocumentSubmitRequest request,
            OwnerId ownerId) throws DocumentSubmitException {

        List<Document> documents = getDocuments(ownerId, request);

        List<DocumentVerificationEntity> docVerifications = new ArrayList<>();
        List<DocumentResultEntity> docResults = new ArrayList<>();

        List<DocumentSubmitRequest.DocumentMetadata> docsMetadata = request.getDocuments();
        for (DocumentSubmitRequest.DocumentMetadata docMetadata : docsMetadata) {
            DocumentVerificationEntity docVerification = createDocumentVerification(ownerId, docMetadata);
            docVerification.setIdentityVerification(idVerification);
            docVerifications.add(docVerification);

            checkDocumentResubmit(ownerId, request, docVerification);

            SubmittedDocument submittedDoc;
            try {
                submittedDoc = createSubmittedDocument(ownerId, docMetadata, documents);
            } catch (DocumentSubmitException e) {
                docVerification.setStatus(DocumentStatus.FAILED);
                docVerification.setErrorDetail(e.getMessage());
                return docVerifications;
            }

            DocumentSubmitResult docSubmitResult = submitDocumentToProvider(ownerId, docVerification, submittedDoc);

            DocumentResultEntity docResult = createDocumentResult(docVerification, docSubmitResult);
            docResult.setTimestampCreated(ownerId.getTimestamp());
            docResults.add(docResult);

            // Delete previously persisted document after a successful upload to the provider
            if (docVerification.getUploadId() != null && docMetadata.getUploadId() != null) {
                documentDataRepository.deleteById(docMetadata.getUploadId());
            }
        }

        documentVerificationRepository.saveAll(docVerifications);

        for (int i = 0; i < docVerifications.size(); i++) {
            DocumentVerificationEntity docVerificationEntity = docVerifications.get(i);
            docResults.get(i).setDocumentVerification(docVerificationEntity);
        }
        documentResultRepository.saveAll(docResults);

        return docVerifications;
    }

    public void checkDocumentResubmit(OwnerId ownerId,
                                       DocumentSubmitRequest request,
                                       DocumentVerificationEntity docVerification) throws DocumentSubmitException {
        if (request.isResubmit() && docVerification.getOriginalDocumentId() == null) {
            logger.error("Detected a resubmit request without specified originalDocumentId for {}, {}", docVerification, ownerId);
            throw new DocumentSubmitException("Missing originalDocumentId in a resubmit request");
        } else if (request.isResubmit()) {
            Optional<DocumentVerificationEntity> originalDocOptional =
                    documentVerificationRepository.findById(docVerification.getOriginalDocumentId());
            if (!originalDocOptional.isPresent()) {
                logger.warn("Missing previous DocumentVerificationEntity(originalDocumentId={}), {}",
                        docVerification.getOriginalDocumentId(), ownerId);
            } else {
                DocumentVerificationEntity originalDoc = originalDocOptional.get();
                originalDoc.setStatus(DocumentStatus.DISPOSED);
                originalDoc.setUsedForVerification(false);
                originalDoc.setTimestampDisposed(ownerId.getTimestamp());
                originalDoc.setTimestampLastUpdated(ownerId.getTimestamp());
                logger.info("Replaced previous {} with new {}, {}", originalDocOptional, docVerification, ownerId);
            }
        } else if (!request.isResubmit() && docVerification.getOriginalDocumentId() != null) {
            logger.error("Detected a submit request with specified originalDocumentId={} for {}, {}",
                    docVerification.getOriginalDocumentId(), docVerification, ownerId);
            throw new DocumentSubmitException("Specified originalDocumentId in a submit request");
        }
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
            docVerification.setProviderName(identityVerificationConfig.getDocumentVerificationProvider());
            docVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
            docVerification.setTimestampUploaded(ownerId.getTimestamp());
            docVerification.setUploadId(docSubmitResult.getUploadId());
        }
        return docSubmitResult;
    }

    /**
     * Upload a single document related to identity verification.
     * @param requestData Binary document data.
     * @param ownerId Owner identification.
     * @return Persisted document metadata of the uploaded document.
     * @throws DocumentVerificationException Thrown when document is invalid.
     */
    @Transactional
    public DocumentMetadata uploadDocument(byte[] requestData, OwnerId ownerId) throws DocumentVerificationException {
        // TODO consider limiting the amount (count, space) of currently uploaded documents per ownerId
        Document document = dataExtractionService.extractDocument(requestData);
        return persistDocumentData(ownerId, document);
    }

    /**
     * Persist a document into database.
     * @param ownerId Owner identification
     * @param document Document to be persisted.
     * @return Persisted document metadata.
     */
    private DocumentMetadata persistDocumentData(OwnerId ownerId, Document document) {
        DocumentDataEntity entity = new DocumentDataEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setFilename(document.getFilename());
        entity.setData(document.getData());
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity = documentDataRepository.save(entity);

        document.setId(entity.getId());

        // Return document metadata only
        DocumentMetadata persistedDocument = new DocumentMetadata();
        persistedDocument.setId(entity.getId());
        persistedDocument.setFilename(entity.getFilename());
        return persistedDocument;
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

    private DocumentVerificationEntity createDocumentVerification(OwnerId ownerId, DocumentSubmitRequest.DocumentMetadata docMetadata) {
        DocumentVerificationEntity entity = new DocumentVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setFilename(docMetadata.getFilename());
        entity.setOriginalDocumentId(docMetadata.getOriginalDocumentId());
        entity.setSide(docMetadata.getSide());
        entity.setType(docMetadata.getType());
        entity.setStatus(DocumentStatus.UPLOAD_IN_PROGRESS);
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUsedForVerification(true);
        return documentVerificationRepository.save(entity);
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
            Optional<Document> docOptional = docs.stream()
                    .filter(doc -> doc.getFilename().equals(docMetadata.getFilename()))
                    .findFirst();
            if (docOptional.isPresent()) {
                photo.setData(docOptional.get().getData());
            } else {
                logger.error("Missing {} in data, {}", docMetadata, ownerId);
                throw new DocumentSubmitException("Not found document data in sent request");
            }
        } else {
            Optional<DocumentDataEntity> docDataOptional =
                    documentDataRepository.findById(docMetadata.getUploadId());
            if (docDataOptional.isPresent()) {
                DocumentDataEntity documentData = docDataOptional.get();
                if (!ownerId.getActivationId().equals(documentData.getActivationId())) {
                    logger.error("The referenced document data uploadId={} are from different activation, {}",
                            docMetadata, ownerId);
                    throw new DocumentSubmitException("Invalid reference on uploaded document data in sent request");
                }
                photo.setData(documentData.getData());
            } else {
                logger.error("Missing {} in data, {}", docMetadata, ownerId);
                throw new DocumentSubmitException("Not found document data in saved upload");
            }
        }
        return submittedDoc;
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

}
