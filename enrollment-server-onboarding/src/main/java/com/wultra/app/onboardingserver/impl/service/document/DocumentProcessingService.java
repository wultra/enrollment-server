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
package com.wultra.app.onboardingserver.impl.service.document;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.DocumentMetadata;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.DocumentResultRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.impl.service.DataExtractionService;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;

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

    private final AuditService auditService;

    private final CommonOnboardingService commonOnboardingService;

    /**
     * Service constructor.
     * @param identityVerificationConfig Identity verification configuration.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param documentResultRepository Document verification result repository.
     * @param dataExtractionService Data extraction service.
     * @param documentVerificationProvider Document verification provider.
     * @param auditService Audit service.
     * @param commonOnboardingService Onboarding process service (common).
     */
    @Autowired
    public DocumentProcessingService(
            final IdentityVerificationConfig identityVerificationConfig,
            final DocumentDataRepository documentDataRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final DocumentResultRepository documentResultRepository,
            final DataExtractionService dataExtractionService,
            final DocumentVerificationProvider documentVerificationProvider,
            final AuditService auditService,
            final CommonOnboardingService commonOnboardingService) {

        this.identityVerificationConfig = identityVerificationConfig;
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentResultRepository = documentResultRepository;
        this.dataExtractionService = dataExtractionService;
        this.documentVerificationProvider = documentVerificationProvider;
        this.auditService = auditService;
        this.commonOnboardingService = commonOnboardingService;
    }

    /**
     * Submit identity-related documents for verification.
     * @param idVerification Identity verification entity.
     * @param request Document submit request.
     * @param ownerId Owner identification.
     * @return Document verification entities.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DocumentVerificationEntity> submitDocuments(
            IdentityVerificationEntity idVerification,
            DocumentSubmitRequest request,
            OwnerId ownerId) throws DocumentSubmitException {

        checkDocumentResubmit(ownerId, request);
        final List<Document> documents = getDocuments(ownerId, request);
        final var documentsByType = request.getDocuments().stream()
                .collect(groupingBy(DocumentSubmitRequest.DocumentMetadata::getType));

        final List<DocumentVerificationEntity> docVerifications = new ArrayList<>();
        for (var docMetadataList : documentsByType.values()) {
            docVerifications.addAll(submitDocument(docMetadataList, documents, idVerification, ownerId));
        }
        return docVerifications;
    }

    /**
     * Submit pages of a document to document verify provider.
     * @param pagesMetadata Pages metadata from request.
     * @param pagesData Pages data.
     * @param idVerification Identity verification entity.
     * @param ownerId Owner identification.
     * @return
     */
    private List<DocumentVerificationEntity> submitDocument(final List<DocumentSubmitRequest.DocumentMetadata> pagesMetadata,
                                                            final List<Document> pagesData,
                                                            final IdentityVerificationEntity idVerification,
                                                            final OwnerId ownerId) {

        // Maps are used to associate DocumentsSubmitResult - DocumentVerificationEntities - DocumentMetadata
        final Map<String, DocumentVerificationEntity> docVerifications = new HashMap<>();
        final Map<String, DocumentSubmitRequest.DocumentMetadata> docMetadataMap = new HashMap<>();

        final List<SubmittedDocument> submittedDocuments = new ArrayList<>();
        for (var metadata : pagesMetadata) {
            final DocumentVerificationEntity docVerification = createDocumentVerification(ownerId, idVerification, metadata);
            docVerifications.put(docVerification.getId(), docVerification);
            docMetadataMap.put(docVerification.getId(), metadata);
            handleResubmit(ownerId, metadata.getOriginalDocumentId(), docVerification);

            try {
                submittedDocuments.add(createSubmittedDocument(ownerId, metadata, pagesData, docVerification));
            } catch (DocumentSubmitException e) {
                logger.warn("Document verification ID: {}, failed: {}", docVerification.getId(), e.getMessage());
                logger.debug("Document verification ID: {}, failed", docVerification.getId(), e);
                docVerification.setStatus(DocumentStatus.FAILED);
                docVerification.setErrorDetail(ErrorDetail.DOCUMENT_VERIFICATION_FAILED);
                docVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
                auditService.audit(docVerification, "Document verification failed for user: {}", ownerId.getUserId());
                return docVerifications.values().stream().toList();
            }
        }

        final List<DocumentVerificationEntity> docVerificationsList = docVerifications.values().stream().toList();
        final DocumentsSubmitResult results = submitDocumentToProvider(submittedDocuments, docVerificationsList, idVerification, ownerId);
        processSubmitResults(results, docVerifications, ownerId);

        docVerificationsList.stream()
                .filter(doc -> StringUtils.isNotBlank(doc.getUploadId()))
                .map(doc -> docMetadataMap.get(doc.getId()).getUploadId())
                .filter(StringUtils::isNotBlank)
                .forEach(fileUploadId -> {
                    documentDataRepository.deleteById(fileUploadId);
                    logger.info("Deleted stored document data with id={}, {}", fileUploadId, ownerId);
                });

        return docVerificationsList;
    }

    /**
     * Process submit results.
     * @param results Document submit result from provider.
     * @param docVerificationsMap To pair results with corresponding DocumentVerificationEntity.
     * @param ownerId Owner identification.
     */
    private void processSubmitResults(final DocumentsSubmitResult results,
                                      final Map<String, DocumentVerificationEntity> docVerificationsMap,
                                      final OwnerId ownerId) {

        final List<DocumentResultEntity> docResults = new ArrayList<>();

        for (final DocumentSubmitResult result : results.getResults()) {
            final DocumentVerificationEntity docVerification = docVerificationsMap.get(result.getDocumentId());
            processDocsSubmitResults(ownerId, docVerification, results, result);

            final DocumentResultEntity docResult = createDocumentResult(docVerification, result);
            docResult.setTimestampCreated(ownerId.getTimestamp());
            docResult.setDocumentVerification(docVerification);

            docResults.add(docResult);
        }

        documentVerificationRepository.saveAll(docVerificationsMap.values());
        documentResultRepository.saveAll(docResults);
        logger.debug("Processed submit result of documents {}, {}", docVerificationsMap.values(), ownerId);
    }

    /**
     * Validates resubmit parameters of DocumentSubmitRequest.
     * @param ownerId Owner identification.
     * @param request Request body.
     * @throws DocumentSubmitException If request is resubmit without original document ID, or is not resubmit with original document ID
     */
    private void checkDocumentResubmit(final OwnerId ownerId, final DocumentSubmitRequest request) throws DocumentSubmitException {
        final boolean isResubmit = request.isResubmit();
        for (var metadata : request.getDocuments()) {
            final String originalDocumentId = metadata.getOriginalDocumentId();
            if (isResubmit && StringUtils.isBlank(originalDocumentId)) {
                logger.debug("Request has resubmit flag but misses originalDocumentId {}, {}", metadata, ownerId);
                throw new DocumentSubmitException("Detected a resubmit request without specified originalDocumentId, %s".formatted(ownerId));
            } else if (!isResubmit && StringUtils.isNotBlank(originalDocumentId)) {
                logger.debug("Request has originalDocumentId but is not flagged as resubmit {}, {}", metadata, ownerId);
                throw new DocumentSubmitException("Detected a submit request with specified originalDocumentId=%s, %s".formatted(originalDocumentId, ownerId));
            }
        }
    }

    /**
     * Sets document with originalDocumentId as disposed. If passed originalDocumentId does not exist, no further action is taken.
     * @param ownerId Owner identification.
     * @param originalDocumentId Id of the original document.
     * @param docVerification Resubmitted document.
     */
    private void handleResubmit(final OwnerId ownerId, final String originalDocumentId, final DocumentVerificationEntity docVerification) {
        if (StringUtils.isBlank(originalDocumentId)) {
            logger.debug("Document {} is not a resubmit {}", docVerification, ownerId);
            return;
        }

        logger.debug("Document {} is a resubmit, {}", docVerification, ownerId);
        final Optional<DocumentVerificationEntity> originalDocOptional = documentVerificationRepository.findById(originalDocumentId);
        if (originalDocOptional.isEmpty()) {
            logger.warn("Missing previous DocumentVerificationEntity(originalDocumentId={}), {}", originalDocumentId, ownerId);
        } else {
            final DocumentVerificationEntity originalDoc = originalDocOptional.get();
            originalDoc.setStatus(DocumentStatus.DISPOSED);
            originalDoc.setUsedForVerification(false);
            originalDoc.setTimestampDisposed(ownerId.getTimestamp());
            originalDoc.setTimestampLastUpdated(ownerId.getTimestamp());
            logger.info("Replaced previous {} with new {}, {}", originalDoc, docVerification, ownerId);
            auditService.audit(docVerification, "Document replaced with new one for user: {}", ownerId.getUserId());
        }
    }

    /**
     * Checks document submit status and data at the provider.
     * @param ownerId Owner identification.
     * @param documentResultEntity Document result entity to be checked at the provider.
     * @throws OnboardingProcessException Thrown when onboarding process is not found.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkDocumentSubmitWithProvider(OwnerId ownerId, DocumentResultEntity documentResultEntity) throws OnboardingProcessException {
        DocumentVerificationEntity docVerification = documentResultEntity.getDocumentVerification();
        DocumentsSubmitResult docsSubmitResults;
        DocumentSubmitResult docSubmitResult;
        try {
            docsSubmitResults = documentVerificationProvider.checkDocumentUpload(ownerId, docVerification);
            final IdentityVerificationEntity identityVerification = documentResultEntity.getDocumentVerification().getIdentityVerification();
            auditService.auditDocumentVerificationProvider(identityVerification, "Check document upload for user: {}", ownerId.getUserId());
            docSubmitResult = docsSubmitResults.getResults().get(0);
        } catch (DocumentVerificationException | RemoteCommunicationException e) {
            logger.warn("Document verification ID: {}, failed: {}", docVerification.getId(), e.getMessage());
            logger.debug("Document verification ID: {}, failed", docVerification.getId(), e);
            docsSubmitResults = new DocumentsSubmitResult();
            docSubmitResult = new DocumentSubmitResult();
            docSubmitResult.setErrorDetail(e.getMessage());
        }

        final String processId = documentResultEntity.getDocumentVerification().getIdentityVerification().getProcessId();
        commonOnboardingService.findProcessWithLock(processId);

        if (StringUtils.isNotBlank(docSubmitResult.getErrorDetail())) {
            logger.debug("Document result ID: {}, error detail: {}, {}",
                    documentResultEntity.getId(), docSubmitResult.getErrorDetail(), ownerId);
            documentResultEntity.setErrorDetail(ErrorDetail.DOCUMENT_VERIFICATION_FAILED);
            documentResultEntity.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
        }
        if (StringUtils.isNotBlank(docSubmitResult.getRejectReason())) {
            logger.debug("Document result ID: {}, reject reason: {}, {}",
                    documentResultEntity.getId(), docSubmitResult.getRejectReason(), ownerId);
            documentResultEntity.setRejectReason(ErrorDetail.DOCUMENT_VERIFICATION_REJECTED);
            documentResultEntity.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
        }

        documentResultEntity.setExtractedData(docSubmitResult.getExtractedData());
        processDocsSubmitResults(ownerId, docVerification, docsSubmitResults, docSubmitResult);
    }

    /**
     * Pass all pages of a document to document verification provider at a single call.
     * @param submittedDocs Document pages to submit.
     * @param docVerifications Entities associated with the document pages to submit.
     * @param identityVerification Identity verification entity.
     * @param ownerId Owner identification.
     * @return document submit result
     */
    private DocumentsSubmitResult submitDocumentToProvider(final List<SubmittedDocument> submittedDocs,
                                                           final List<DocumentVerificationEntity> docVerifications,
                                                           final IdentityVerificationEntity identityVerification,
                                                           final OwnerId ownerId) {

        final List<String> docVerificationIds = docVerifications.stream().map(DocumentVerificationEntity::getId).toList();

        try {
            final DocumentsSubmitResult results = documentVerificationProvider.submitDocuments(ownerId, submittedDocs);
            logger.debug("Documents {} submitted to provider, {}", docVerifications, ownerId);
            auditService.auditDocumentVerificationProvider(identityVerification, "Submit documents for user: {}, document IDs: {}", ownerId.getUserId(), docVerificationIds);
            return results;
        } catch (DocumentVerificationException | RemoteCommunicationException e) {
            logger.warn("Document verification ID: {}, failed: {}", docVerificationIds, e.getMessage());
            logger.debug("Document verification ID: {}, failed", docVerificationIds, e);
            final DocumentsSubmitResult results = new DocumentsSubmitResult();
            submittedDocs.forEach(doc -> {
                final DocumentSubmitResult result = new DocumentSubmitResult();
                result.setDocumentId(doc.getDocumentId());
                result.setErrorDetail(e.getMessage());
                results.getResults().add(result);
            });
            return results;
        }

    }

    public DocumentSubmitResult submitDocumentToProvider(OwnerId ownerId, DocumentVerificationEntity docVerification, SubmittedDocument submittedDoc) {
        DocumentsSubmitResult docsSubmitResults;
        DocumentSubmitResult docSubmitResult;
        try {
            docsSubmitResults = documentVerificationProvider.submitDocuments(ownerId, List.of(submittedDoc));
            final IdentityVerificationEntity identityVerification = docVerification.getIdentityVerification();
            auditService.auditDocumentVerificationProvider(identityVerification, "Submit documents for user: {}, document ID: {}", ownerId.getUserId(), docVerification.getId());
            docSubmitResult = docsSubmitResults.getResults().get(0);
        } catch (DocumentVerificationException | RemoteCommunicationException e) {
            logger.warn("Document verification ID: {}, failed: {}", docVerification.getId(), e.getMessage());
            logger.debug("Document verification ID: {}, failed", docVerification.getId(), e);
            docsSubmitResults = new DocumentsSubmitResult();
            docSubmitResult = new DocumentSubmitResult();
            docSubmitResult.setErrorDetail(e.getMessage());
        }

        processDocsSubmitResults(ownerId, docVerification, docsSubmitResults, docSubmitResult);
        return docSubmitResult;
    }

    public boolean shouldDocumentProviderStoreSelfie() {
        return documentVerificationProvider.shouldStoreSelfie();
    }

    /**
     * Upload a single document related to identity verification.
     * @param idVerification Identity verification entity.
     * @param requestData Binary document data.
     * @param ownerId Owner identification.
     * @return Persisted document metadata of the uploaded document.
     * @throws DocumentVerificationException Thrown when document is invalid.
     */
    @Transactional
    public DocumentMetadata uploadDocument(IdentityVerificationEntity idVerification, byte[] requestData, OwnerId ownerId) throws DocumentVerificationException {
        // TODO consider limiting the amount (count, space) of currently uploaded documents per ownerId
        Document document = dataExtractionService.extractDocument(requestData);
        return persistDocumentData(idVerification, ownerId, document);
    }

    /**
     * Pairs documents with two sides, front side will be linked to the back side and vice versa.
     * @param documents Documents to be checked on two sides linkin
     */
    @Transactional
    public void pairTwoSidedDocuments(List<DocumentVerificationEntity> documents) {
        for (DocumentVerificationEntity document : documents) {
            if (!document.getType().isTwoSided()) {
                continue;
            }
            documents.stream()
                    .filter(item -> item.getType() == document.getType())
                    .filter(item -> item.getSide() != document.getSide())
                    .forEach(item -> {
                        logger.debug("Found other side {} for {}", item, document);
                        item.setOtherSideId(document.getId());
                        documentVerificationRepository.setOtherDocumentSide(item.getId(), document.getId());
                    });
        }
    }

    /**
     * Persist a document into database.
     * @param idVerification Identity verification entity.
     * @param ownerId Owner identification
     * @param document Document to be persisted.
     * @return Persisted document metadata.
     */
    private DocumentMetadata persistDocumentData(IdentityVerificationEntity idVerification, OwnerId ownerId, Document document) {
        DocumentDataEntity entity = new DocumentDataEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setIdentityVerification(idVerification);
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
        entity.setErrorOrigin(docVerificationEntity.getErrorOrigin());
        entity.setExtractedData(docSubmitResult.getExtractedData());
        entity.setPhase(DocumentProcessingPhase.UPLOAD);
        entity.setRejectReason(docVerificationEntity.getRejectReason());
        entity.setRejectOrigin(docVerificationEntity.getRejectOrigin());
        return entity;
    }

    private DocumentVerificationEntity createDocumentVerification(OwnerId ownerId, IdentityVerificationEntity identityVerification, DocumentSubmitRequest.DocumentMetadata docMetadata) {
        DocumentVerificationEntity entity = new DocumentVerificationEntity();
        entity.setActivationId(ownerId.getActivationId());
        entity.setIdentityVerification(identityVerification);
        entity.setFilename(docMetadata.getFilename());
        entity.setOriginalDocumentId(docMetadata.getOriginalDocumentId());
        entity.setSide(docMetadata.getSide());
        entity.setType(docMetadata.getType());
        entity.setStatus(DocumentStatus.UPLOAD_IN_PROGRESS);
        entity.setTimestampCreated(ownerId.getTimestamp());
        entity.setUsedForVerification(true);
        final DocumentVerificationEntity saveEntity = documentVerificationRepository.save(entity);
        logger.debug("Created {} for {}", saveEntity, ownerId);
        auditService.auditDebug(entity, "Document created for user: {}", ownerId.getUserId());
        return saveEntity;
    }

    private SubmittedDocument createSubmittedDocument(
            OwnerId ownerId,
            DocumentSubmitRequest.DocumentMetadata docMetadata,
            List<Document> docs,
            DocumentVerificationEntity docVerification) throws DocumentSubmitException {
        final Image photo = Image.builder()
                .filename(docMetadata.getFilename())
                .build();

        SubmittedDocument submittedDoc = new SubmittedDocument();
        submittedDoc.setDocumentId(docVerification.getId());
        submittedDoc.setPhoto(photo);
        submittedDoc.setSide(docMetadata.getSide());
        submittedDoc.setType(docMetadata.getType());

        if (docMetadata.getUploadId() == null) {
            final Document document = docs.stream()
                    .filter(doc -> doc.getFilename().equals(docMetadata.getFilename()))
                    .findFirst()
                    .orElseThrow(() ->
                            new DocumentSubmitException(String.format("Missing %s in data, %s", docMetadata, ownerId)));
            photo.setData(document.getData());
        } else {
            final DocumentDataEntity documentData = documentDataRepository.findById(docMetadata.getUploadId())
                    .orElseThrow(() ->
                            new DocumentSubmitException(String.format("Missing %s in data, %s", docMetadata, ownerId)));
            if (!ownerId.getActivationId().equals(documentData.getActivationId())) {
                throw new DocumentSubmitException(
                        String.format("The referenced document data uploadId=%s are from different activation, %s", docMetadata, ownerId));
            }
            photo.setData(documentData.getData());
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

    private void processDocsSubmitResults(OwnerId ownerId, DocumentVerificationEntity docVerification,
                                          DocumentsSubmitResult docsSubmitResults, DocumentSubmitResult docSubmitResult) {
        if (StringUtils.isNotBlank(docSubmitResult.getErrorDetail())) {
            docVerification.setStatus(DocumentStatus.FAILED);
            docVerification.setErrorDetail(ErrorDetail.DOCUMENT_VERIFICATION_FAILED);
            docVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
            logger.info("Document verification ID: {}, failed: {}, {}",
                    docVerification.getId(), docSubmitResult.getErrorDetail(), ownerId);
            auditService.audit(docVerification, "Document verification failed for user: {}, detail: {}", ownerId.getUserId(), docSubmitResult.getErrorDetail());
        } else if (StringUtils.isNotBlank(docSubmitResult.getRejectReason())) {
            docVerification.setStatus(DocumentStatus.REJECTED);
            docVerification.setRejectReason(ErrorDetail.DOCUMENT_VERIFICATION_REJECTED);
            docVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
            logger.info("Document verification ID: {}, rejected: {}, {}",
                    docVerification.getId(), docSubmitResult.getRejectReason(), ownerId);
            auditService.audit(docVerification, "Document verification rejected for user: {}, reason: {}", ownerId.getUserId(), docSubmitResult.getRejectReason());
        } else {
            docVerification.setPhotoId(docsSubmitResults.getExtractedPhotoId());
            docVerification.setProviderName(identityVerificationConfig.getDocumentVerificationProvider());
            if (docVerification.getTimestampUploaded() == null) {
                docVerification.setTimestampUploaded(ownerId.getTimestamp());
            }
            docVerification.setUploadId(docSubmitResult.getUploadId());

            if (docVerification.getType() == DocumentType.SELFIE_PHOTO) {
                final DocumentStatus status = identityVerificationConfig.isVerifySelfieWithDocumentsEnabled() ? DocumentStatus.VERIFICATION_PENDING : DocumentStatus.ACCEPTED;
                docVerification.setStatus(status);
                logger.info("Document selfie changed status to {}, {}", status, ownerId);
                auditService.audit(docVerification, "Document selfie changed status to {} for user: {}", status, ownerId.getUserId());
            } else if (docSubmitResult.getExtractedData() == null) { // only finished upload contains extracted data
                docVerification.setStatus(DocumentStatus.UPLOAD_IN_PROGRESS);
                logger.info("Document upload ID: {} in progress, {}", docSubmitResult.getUploadId(), ownerId);
                auditService.auditDebug(docVerification, "Document upload in progress for user: {}", ownerId.getUserId());
            } else { // no document verification during upload, wait for the final all documents verification
                docVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
                logger.info("Document upload ID: {} verification pending, {}", docSubmitResult.getUploadId(), ownerId);
                auditService.audit(docVerification, "Document verification pending for user: {}", ownerId.getUserId());
            }
        }
    }

}
