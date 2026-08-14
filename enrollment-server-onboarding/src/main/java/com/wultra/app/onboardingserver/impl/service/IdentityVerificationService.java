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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentStatusRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitV2Request;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.DocumentStatusResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.DocumentMetadataResponseDto;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.*;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.common.service.IdentityVerificationLimitService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationNotFoundException;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationProcessingService;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesCheck;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;

/**
 * Service implementing document identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
public class IdentityVerificationService {

    private final IdentityVerificationConfig identityVerificationConfig;
    private final DocumentDataRepository documentDataRepository;
    private final DocumentVerificationRepository documentVerificationRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final DocumentProcessingService documentProcessingService;
    private final VerificationProcessingService verificationProcessingService;
    private final DocumentVerificationProvider documentVerificationProvider;
    private final IdentityVerificationLimitService identityVerificationLimitService;
    private final CommonOnboardingService processService;
    private final OnboardingProcessLimitService processLimitService;

    private final RequiredDocumentTypesCheck requiredDocumentTypesCheck;
    private final IdentityVerificationPrecompleteCheck identityVerificationPrecompleteCheck;

    private final AuditService auditService;
    private final ProcessedDocumentDataRepository processedDocumentDataRepository;
    private final DocumentResultRepository documentResultRepository;

    /**
     * Finds the current verification identity
     * @param ownerId Owner identification.
     * @return Optional entity of the verification identity
     */
    public Optional<IdentityVerificationEntity> findByOptional(OwnerId ownerId) {
        return identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());
    }

    /**
     * Finds identity verification for the given onboarding process.
     *
     * @param processId Onboarding process identifier.
     * @return Optional entity of the verification identity.
     */
    public Optional<IdentityVerificationEntity> findByProcessIdOptional(final String processId) {
        return identityVerificationRepository.findByProcessId(processId);
    }

    /**
     * Finds the current verification identity
     * @param ownerId Owner identification.
     * @return Entity of the verification identity
     * @throws IdentityVerificationNotFoundException When the verification identity entity was not found
     */
    public IdentityVerificationEntity findBy(OwnerId ownerId) throws IdentityVerificationNotFoundException {
        return findByOptional(ownerId).orElseThrow(() ->
                new IdentityVerificationNotFoundException("No identity verification entity found, " + ownerId));
    }

    /**
     * Update an identity verification entity in database.
     * @param identityVerification Identity verification identity.
     * @return Updated identity verification entity.
     */
    public IdentityVerificationEntity updateIdentityVerification(IdentityVerificationEntity identityVerification) {
        return identityVerificationRepository.save(identityVerification);
    }

    /**
     * Move the given identity verification to the given phase and status.
     *
     * @param identityVerification Identity verification identity.
     * @param phase Target phase.
     * @param status Target status.
     * @param ownerId Owner identification.
     * @return saved identity verification
     */
    @Transactional
    public IdentityVerificationEntity moveToPhaseAndStatus(final IdentityVerificationEntity identityVerification,
                                                           final IdentityVerificationPhase phase,
                                                           final IdentityVerificationStatus status,
                                                           final OwnerId ownerId) {

        identityVerification.setPhase(phase);
        identityVerification.setStatus(status);
        identityVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        final IdentityVerificationEntity savedIdentityVerification = identityVerificationRepository.save(identityVerification);
        logger.info("Switched to {}/{}; {}", phase, status, ownerId);
        auditService.audit(identityVerification, "Switched to {}/{}; user ID: {}", phase, status, ownerId.getUserId());
        return savedIdentityVerification;
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param ownerId Owner identification.
     * @throws DocumentSubmitException Thrown when document submission fails.
     * @throws IdentityVerificationLimitException Thrown when document upload limit is reached.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when identity verification is invalid.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    public void submitDocuments(final DocumentSubmitV2Request request, final OwnerId ownerId)
            throws DocumentSubmitException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException, OnboardingProcessException {

        final IdentityVerificationEntity idVerification = findByOptional(ownerId).orElseThrow(() ->
                new DocumentSubmitException("Identity verification has not been initialized, " + ownerId));

        String processId = idVerification.getProcessId();
        if (!processId.equals(request.processId())) {
            throw new DocumentSubmitException("Invalid process ID: " + processId);
        }

        final IdentityVerificationPhase phase = idVerification.getPhase();
        final IdentityVerificationStatus status = idVerification.getStatus();
        if (phase != IdentityVerificationPhase.DOCUMENT_UPLOAD || status != IdentityVerificationStatus.IN_PROGRESS) {
            throw new DocumentSubmitException(
                    String.format("Not allowed submit of documents during not upload phase %s/%s, %s", phase, status, ownerId));
        }

        identityVerificationLimitService.checkDocumentUploadLimit(ownerId, idVerification);

        final List<DocumentVerificationEntity> docsVerifications = documentProcessingService.submitDocuments(idVerification, request, ownerId);
        documentProcessingService.pairTwoSidedDocuments(docsVerifications);

        identityVerificationRepository.save(idVerification);
    }

    /**
     * Starts the document verification.
     *
     * @param ownerId Owner identification.
     * @param identityVerification Identity verification.
     * @return Document evaluation status.
     */
    @Transactional
    public VerificationDocumentActionResult startDocumentVerification(OwnerId ownerId, IdentityVerificationEntity identityVerification) {
        logger.info("action: startDocumentVerification, state: initiated");
        List<DocumentVerificationEntity> docVerifications =
                documentVerificationRepository.findAllDocumentVerifications(identityVerification,
                        Collections.singletonList(DocumentStatus.VERIFICATION_PENDING));

        final List<DocumentVerificationEntity> selfiePhotoVerifications =
                docVerifications.stream()
                        .filter(entity -> DocumentType.SELFIE_PHOTO.equals(entity.getType()))
                        .toList();

        // If not enabled then remove selfie photos from the verification process
        if (!identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()) {
            docVerifications.removeAll(selfiePhotoVerifications);
        }

        documentProcessingService.pairTwoSidedDocuments(docVerifications);

        final List<String> uploadIds = docVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .toList();

        final DocumentsVerificationResult result;
        try {
            result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);
        } catch (RemoteCommunicationException | DocumentVerificationException e) {
            logger.warn("action: startDocumentVerification, state: failed, exceptionMessage: {}", e.getMessage(), e);
            return VerificationDocumentActionResult.FAILED;
        }

        final String verificationId = result.getVerificationId();
        final DocumentVerificationStatus status = result.getStatus();
        logger.info("Verified documents upload ID: {}, verification ID: {}, status: {}, {}", uploadIds, verificationId, status, ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Documents verified: {} for user: {}", status, ownerId.getUserId());
        verificationProcessingService.processVerificationResult(ownerId, docVerifications, result);

        final var documentEvaluationResult = evaluateRequiredDocuments(identityVerification, docVerifications, ownerId);

        if (!identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()) {
            logger.debug("Selfie photos verification disabled, changing selfie document status to ACCEPTED, {}", ownerId);
            selfiePhotoVerifications.forEach(selfiePhotoVerification -> {
                selfiePhotoVerification.setStatus(DocumentStatus.ACCEPTED);
                selfiePhotoVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                auditService.audit(selfiePhotoVerification, "Selfie document accepted for user: {}", ownerId.getUserId());
            });
            documentVerificationRepository.saveAll(selfiePhotoVerifications);
        }

        logger.info("action: startDocumentVerification, state: succeeded, result: {}", documentEvaluationResult);
        return documentEvaluationResult;
    }

    /**
     * Process identity verification result, check the verifications which have already been previously processed.
     *
     * @param ownerId Owner identification.
     * @param idVerification Identity verification entity.
     * @return final verification result
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    @Transactional
    public FinalVerificationResult processVerificationResult(final OwnerId ownerId, final IdentityVerificationEntity idVerification) throws RemoteCommunicationException {
        final var result = identityVerificationPrecompleteCheck.evaluate(idVerification);

        if (!result.isSuccessful()) {
            logger.warn("Final validation did not pass, marking identity verification as failed due to '{}', {}", result.getErrorDetail(), ownerId);
            idVerification.setErrorDetail(result.getErrorDetail());
            idVerification.setTimestampFailed(ownerId.getTimestamp());
            idVerification.setErrorOrigin(ErrorOrigin.FINAL_VALIDATION);
        }

        idVerification.setTimestampFinished(ownerId.getTimestamp());
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        identityVerificationRepository.save(idVerification);

        return result.isSuccessful() ? FinalVerificationResult.OK : FinalVerificationResult.FAILED;
    }

    /**
     * Evaluate whether the required documents are satisfied and decide whether the identity verification may proceed
     * to the next stage.
     * <p>
     * The process proceeds to the next stage ({@link VerificationDocumentActionResult#REQUIRED_DOCUMENTS_VERIFIED})
     * as soon as the required number of accepted documents is met, even when some documents in the current batch
     * failed. Otherwise it stays in the current stage
     * ({@link VerificationDocumentActionResult#INSUFFICIENT_DOCUMENT_COUNT}) to allow submission or resubmission of
     * additional documents.
     * <p>
     * Regardless of the outcome, every failed or rejected document from the current batch is counted toward the
     * process error score (each document at most once over its lifetime).
     *
     * @param idVerification Identity verification entity.
     * @param docVerificationsToProcess Documents from the current processing batch, with their statuses already updated
     *                                  by the verification provider.
     * @param ownerId Owner identification.
     * @return action result indicating whether the process may proceed to the next stage
     */
    private VerificationDocumentActionResult evaluateRequiredDocuments(
            final IdentityVerificationEntity idVerification,
            final List<DocumentVerificationEntity> docVerificationsToProcess,
            final OwnerId ownerId) {

        final String identityVerificationId = idVerification.getId();
        final var processId = idVerification.getProcessId();
        // docVerificationsToProcess contains only documents from the current batch, but the check of required documents needs to account for all documents related to the identity verification
        final List<DocumentVerificationEntity> allDocumentVerifications = documentVerificationRepository.findAllUsedForVerification(idVerification);


        incrementErrorScoreForFailures(idVerification, docVerificationsToProcess, ownerId);

        final boolean allRequiredDocumentsChecked = requiredDocumentTypesCheck.evaluate(allDocumentVerifications, processId);
        if (allRequiredDocumentsChecked) {
            logger.debug("Required documents are accepted, proceeding for identity verification ID: {}", identityVerificationId);
            return VerificationDocumentActionResult.REQUIRED_DOCUMENTS_VERIFIED;
        }

        logger.debug("Not enough accepted documents, allow submission of additional documents for identity verification ID: {}", identityVerificationId);
        markIdentityVerificationError(docVerificationsToProcess, idVerification, DocumentStatus.FAILED, ownerId);
        markIdentityVerificationError(docVerificationsToProcess, idVerification, DocumentStatus.REJECTED, ownerId);
        return VerificationDocumentActionResult.INSUFFICIENT_DOCUMENT_COUNT;
    }

    private void markIdentityVerificationError(
            final List<DocumentVerificationEntity> docVerifications,
            final IdentityVerificationEntity idVerification,
            final DocumentStatus status,
            final OwnerId ownerId) {

        docVerifications.stream()
                .filter(docVerification -> docVerification.getStatus() == status)
                .findAny()
                .ifPresent(docVerification -> {
                    logger.debug("At least one document is {}, ID: {}, {}", status, docVerification.getId(), ownerId);
                    idVerification.setErrorDetail(fetchErrorDetail(docVerification.getStatus()));
                    idVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
                });
    }


    private static String fetchErrorDetail(final DocumentStatus status) {
        if (status == DocumentStatus.REJECTED) {
            return ErrorDetail.DOCUMENT_VERIFICATION_REJECTED;
        } else if (status == DocumentStatus.FAILED) {
            return ErrorDetail.DOCUMENT_VERIFICATION_FAILED;
        } else {
            return "";
        }
    }

    /**
     * Increment the process error score for each failed or rejected document in the given batch and check process
     * error limits. Each document is counted at most once over its lifetime, because a document transitions to a
     * terminal status and enters a processing batch exactly once.
     *
     * @param idVerification Identity verification entity.
     * @param docVerifications Documents from the current processing batch.
     * @param ownerId Owner identifier.
     */
    private void incrementErrorScoreForFailures(
            final IdentityVerificationEntity idVerification,
            final List<DocumentVerificationEntity> docVerifications,
            final OwnerId ownerId) {

        final long failedCount = docVerifications.stream()
                .filter(it -> it.getStatus() == DocumentStatus.FAILED)
                .count();
        final long rejectedCount = docVerifications.stream()
                .filter(it -> it.getStatus() == DocumentStatus.REJECTED)
                .count();

        if (failedCount == 0 && rejectedCount == 0) {
            return;
        }

        final OnboardingProcessEntity process;
        try {
            process = processService.findProcess(idVerification.getProcessId());
        } catch (OnboardingProcessException e) {
            logger.warn("Onboarding process not found, {}", ownerId, e);
            return;
        }

        for (long i = 0; i < failedCount; i++) {
            processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED, ownerId);
        }
        for (long i = 0; i < rejectedCount; i++) {
            processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_REJECTED, ownerId);
        }
        processLimitService.checkOnboardingProcessErrorLimits(process);
    }

    /**
     * Fetch status of document verification related to identity.
     *
     * @param request Document status request.
     * @param ownerId Owner identification.
     * @return Document status response.
     */
    @Transactional
    public DocumentStatusResponse fetchDocumentStatusResponse(final DocumentStatusRequest request, final OwnerId ownerId) {
        DocumentStatusResponse response = new DocumentStatusResponse();

        Optional<IdentityVerificationEntity> idVerificationOptional =
                identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());

        if (idVerificationOptional.isEmpty()) {
            logger.error("Checking identity verification status on a not existing entity, {}", ownerId);
            response.setStatus(FAILED);
            return response;
        }

        final IdentityVerificationEntity idVerification = idVerificationOptional.get();

        final List<DocumentVerificationEntity> entities;
        if (request.getFilter() != null) {
            final List<String> documentIds = request.getFilter().stream()
                    .map(DocumentStatusRequest.DocumentFilter::getDocumentId)
                    .toList();
            entities = Streamable.of(documentVerificationRepository.findAllById(documentIds)).toList();
        } else {
            entities = idVerification.getDocumentVerifications().stream()
                    .filter(DocumentVerificationEntity::isUsedForVerification)
                    .toList();
        }

        // Ensure that all entities are related to the identity verification
        if (!entities.isEmpty()) {
            for (DocumentVerificationEntity entity : entities) {
                if (!entity.getActivationId().equals(idVerification.getActivationId())) {
                    logger.error("Not related {} to {}, {}", entity, idVerification, ownerId);
                    response.setStatus(FAILED);
                    return response;
                }
            }
        }

        List<DocumentMetadataResponseDto> docsMetadata = createDocsMetadata(entities);
        response.setStatus(idVerification.getStatus());
        response.setDocuments(docsMetadata);

        return response;
    }

    /**
     * Cleanup documents related to identity verification.
     * @param ownerId Owner identification.
     * @throws DocumentVerificationException Thrown when document cleanup fails
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     * @throws IdentityVerificationException Thrown when identity verification reset fails.
     * @throws OnboardingProcessLimitException Thrown when maximum failed attempts for identity verification have been reached.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     */
    @Transactional
    public void cleanup(OwnerId ownerId)
            throws DocumentVerificationException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException, OnboardingProcessException {
        final var idsViews = documentVerificationRepository.findAllUploadIdPhotoIdByActivationId(ownerId.getActivationId());

        if (identityVerificationConfig.isDocumentVerificationCleanupEnabled()) {
            deleteDocumentData(ownerId, idsViews);
        } else {
            logger.debug("Skipped cleanup of documents at document verification provider (not enabled), {}", ownerId);
        }

        documentVerificationRepository.failVerifications(ownerId.getActivationId(), ownerId.getTimestamp(), DocumentStatus.ALL_NOT_FINISHED);

        // Reset identity verification, the client is expected to call /api/identity/init for the next round of verification
        identityVerificationLimitService.resetIdentityVerification(ownerId, ErrorOrigin.CLEANUP, "reset due to cleanup");
    }

    /**
     * Provides photo data
     * @param photoId Identification of the photo
     * @param ownerId Owner identification.
     * @return Photo image
     * @throws RemoteCommunicationException In case of remote communication error.
     * @throws DocumentVerificationException In case of business logic error.
     */
    public Image getPhotoById(final String photoId, final OwnerId ownerId) throws DocumentVerificationException, RemoteCommunicationException {
        final Image result = documentVerificationProvider.getPhoto(photoId);
        final IdentityVerificationEntity identityVerification = findByOptional(ownerId).orElseThrow(() ->
                new DocumentVerificationException("Unable to find identity verification for " + ownerId));
        auditService.auditDocumentVerificationProvider(identityVerification, "Check document upload for user: {}", ownerId.getUserId());
        return result;
    }

    public List<DocumentMetadataResponseDto> createDocsMetadata(List<DocumentVerificationEntity> entities) {
        return entities.stream()
                .map(this::toDocumentMetadata)
                .toList();
    }

    /**
     * Initializes verification SDK.
     * @param ownerId Owner identification.
     * @param initAttributes SDK initialization attributes.
     * @return Verification SDK info.
     * @throws RemoteCommunicationException In case of remote communication error.
     * @throws DocumentVerificationException In case of business logic error.
     */
    public VerificationSdkInfo initVerificationSdk(OwnerId ownerId, Map<String, String> initAttributes)
            throws DocumentVerificationException, RemoteCommunicationException {
        VerificationSdkInfo verificationSdkInfo = documentVerificationProvider.initVerificationSdk(ownerId, initAttributes);
        final IdentityVerificationEntity identityVerification = findByOptional(ownerId).orElseThrow(() ->
                new DocumentVerificationException("Unable to find identity verification for " + ownerId));
        auditService.auditDocumentVerificationProvider(identityVerification, "Sdk initialized for user: {}", ownerId.getUserId());
        logger.info("Initialized verification SDK, {}", ownerId);
        return verificationSdkInfo;
    }

    /**
     * Return all identity verifications eligible for change to the next state.
     *
     * @return identity verifications
     */
    public List<IdentityVerificationEntity> findAllIdentityVerificationsToChangeState() {
        final var provider = identityVerificationConfig.getDocumentVerificationProvider();
        final var batchSize = identityVerificationConfig.getNextStateBatchSize();

        return identityVerificationRepository.findAllIdentityVerificationsToChangeState(provider, Pageable.ofSize(batchSize));
    }

    /**
     * Create {@link DocumentMetadataResponseDto} from {@link DocumentVerificationEntity}
     * @param entity Document verification entity.
     * @return Document metadata for response
     */
    private DocumentMetadataResponseDto toDocumentMetadata(DocumentVerificationEntity entity) {
        final DocumentMetadataResponseDto docMetadata = new DocumentMetadataResponseDto();
        docMetadata.setId(entity.getId());
        // Hide specific error reason if any.
        if (StringUtils.isNotBlank(entity.getErrorDetail()) || StringUtils.isNotBlank(entity.getRejectReason())) {
            docMetadata.setErrors(List.of("Error verifying the document."));
        }
        docMetadata.setFilename(entity.getFilename());
        docMetadata.setSide(entity.getSide());
        docMetadata.setStatus(entity.getStatus());
        docMetadata.setType(entity.getType());
        docMetadata.setCountry(entity.getCountry());
        return docMetadata;
    }

    private void deleteDocumentData(final OwnerId ownerId, final List<DocumentVerificationIdsView> idsViews) throws RemoteCommunicationException, DocumentVerificationException, IdentityVerificationNotFoundException {
        final var uploadIds = idsViews.stream()
                .map(DocumentVerificationIdsView::getUploadId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        logger.info("Deleting DocumentData ids: {}", uploadIds);
        documentDataRepository.deleteAllById(uploadIds);

        final var processedDocumentIds = idsViews.stream()
                .map(DocumentVerificationIdsView::getPhotoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        logger.info("Deleting ProcessedDocumentData ids: {}", processedDocumentIds);
        processedDocumentDataRepository.deleteAllById(processedDocumentIds);

        documentVerificationProvider.cleanupDocuments(ownerId, uploadIds.stream().toList());

        final var documentVerificationIds = idsViews.stream()
                .map(DocumentVerificationIdsView::getId)
                .collect(Collectors.toSet());

        logger.info("Cleaning DocumentResult documentVerificationIds: {}", documentVerificationIds);
        documentResultRepository.clean(documentVerificationIds);

        final IdentityVerificationEntity identityVerification = findBy(ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Cleaned up documents for user: {}", ownerId.getUserId());
        logger.info("All document data successfully deleted");
    }

    public enum VerificationDocumentActionResult {

        /**
         * All documents are accepted.
         */
        REQUIRED_DOCUMENTS_VERIFIED,

        /**
         * Some documents are not accepted or not all required documents are accepted yet.
         */
        INSUFFICIENT_DOCUMENT_COUNT,

        FAILED
    }

    public enum FinalVerificationResult {
        OK,
        FAILED
    }
}
