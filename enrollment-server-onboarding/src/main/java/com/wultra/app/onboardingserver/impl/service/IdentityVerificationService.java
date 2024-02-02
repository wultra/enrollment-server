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
import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.DocumentStatusResponse;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.data.DocumentMetadataResponseDto;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.common.service.IdentityVerificationLimitService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationNotFoundException;
import com.wultra.app.onboardingserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationProcessingService;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.statemachine.guard.document.RequiredDocumentTypesCheck;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_UPLOAD;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;

/**
 * Service implementing document identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
@Slf4j
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

    /**
     * Service constructor.
     * @param identityVerificationConfig Identity verification config.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentProcessingService Document processing service.
     * @param verificationProcessingService Verification processing service.
     * @param documentVerificationProvider Document verification provider.
     * @param identityVerificationLimitService Identity verification limit service.
     * @param processService Common onboarding process service.
     * @param processLimitService Onboarding process limit service.
     * @param auditService Audit service.
     */
    @Autowired
    IdentityVerificationService(
            final IdentityVerificationConfig identityVerificationConfig,
            final DocumentDataRepository documentDataRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final IdentityVerificationRepository identityVerificationRepository,
            final DocumentProcessingService documentProcessingService,
            final VerificationProcessingService verificationProcessingService,
            final DocumentVerificationProvider documentVerificationProvider,
            final IdentityVerificationLimitService identityVerificationLimitService,
            final CommonOnboardingService processService,
            final OnboardingProcessLimitService processLimitService,
            final RequiredDocumentTypesCheck requiredDocumentTypesCheck,
            final IdentityVerificationPrecompleteCheck identityVerificationPrecompleteCheck,
            final AuditService auditService) {

        this.identityVerificationConfig = identityVerificationConfig;
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.identityVerificationRepository = identityVerificationRepository;
        this.documentProcessingService = documentProcessingService;
        this.verificationProcessingService = verificationProcessingService;
        this.documentVerificationProvider = documentVerificationProvider;
        this.identityVerificationLimitService = identityVerificationLimitService;
        this.processService = processService;
        this.processLimitService = processLimitService;
        this.requiredDocumentTypesCheck = requiredDocumentTypesCheck;
        this.identityVerificationPrecompleteCheck = identityVerificationPrecompleteCheck;
        this.auditService = auditService;
    }

    /**
     * Finds the current verification identity
     * @param ownerId Owner identification.
     * @return Optional entity of the verification identity
     */
    public Optional<IdentityVerificationEntity> findByOptional(OwnerId ownerId) {
        return identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(ownerId.getActivationId());
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
    public void submitDocuments(final DocumentSubmitRequest request, final OwnerId ownerId)
            throws DocumentSubmitException, IdentityVerificationLimitException, RemoteCommunicationException, IdentityVerificationException, OnboardingProcessLimitException, OnboardingProcessException {

        final IdentityVerificationEntity idVerification = findByOptional(ownerId).orElseThrow(() ->
                new DocumentSubmitException("Identity verification has not been initialized, " + ownerId));

        String processId = idVerification.getProcessId();
        if (!processId.equals(request.getProcessId())) {
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
     * Starts the verification process
     *
     * @param ownerId Owner identification.
     * @param identityVerification Identity verification.
     * @throws RemoteCommunicationException In case of remote communication error.
     * @throws DocumentVerificationException In case of business logic error.
     */
    @Transactional
    public void startVerification(OwnerId ownerId, IdentityVerificationEntity identityVerification) throws DocumentVerificationException, RemoteCommunicationException {
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

        final DocumentsVerificationResult result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);
        final String verificationId = result.getVerificationId();
        final DocumentVerificationStatus status = result.getStatus();
        logger.info("Verified documents upload ID: {}, verification ID: {}, status: {}, {}", uploadIds, verificationId, status, ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Documents verified: {} for user: {}", status, ownerId.getUserId());
        verificationProcessingService.processVerificationResult(ownerId, docVerifications, result);

        moveToDocumentVerificationAndStatusByDocuments(identityVerification, docVerifications, ownerId);

        if (!identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()) {
            logger.debug("Selfie photos verification disabled, changing selfie document status to ACCEPTED, {}", ownerId);
            selfiePhotoVerifications.forEach(selfiePhotoVerification -> {
                selfiePhotoVerification.setStatus(DocumentStatus.ACCEPTED);
                selfiePhotoVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                auditService.audit(selfiePhotoVerification, "Selfie document accepted for user: {}", ownerId.getUserId());
            });
            documentVerificationRepository.saveAll(selfiePhotoVerifications);
        }
    }

    /**
     * Checks verification result and evaluates the final state of the identity verification process
     *
     * @param ownerId Owner identification.
     * @param idVerification Verification identity
     * @throws DocumentVerificationException Thrown when an error during verification check occurred.
     * @throws OnboardingProcessException Thrown when onboarding process is invalid.
     * @throws RemoteCommunicationException In case of remote communication error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkVerificationResult(final OwnerId ownerId, final IdentityVerificationEntity idVerification)
            throws DocumentVerificationException, OnboardingProcessException, RemoteCommunicationException {
        List<DocumentVerificationEntity> allDocVerifications =
                documentVerificationRepository.findAllDocumentVerifications(idVerification,
                        Collections.singletonList(DocumentStatus.VERIFICATION_IN_PROGRESS));
        Map<String, List<DocumentVerificationEntity>> verificationsById = new HashMap<>();

        for (DocumentVerificationEntity docVerification : allDocVerifications) {
            verificationsById.computeIfAbsent(docVerification.getVerificationId(), verificationId -> new ArrayList<>())
                    .add(docVerification);
        }

        for (Map.Entry<String, List<DocumentVerificationEntity>> entry : verificationsById.entrySet()) {
            DocumentsVerificationResult docVerificationResult = documentVerificationProvider.getVerificationResult(ownerId, entry.getKey());
            auditService.auditDocumentVerificationProvider(idVerification, "Got verification result: {} for user: {}", docVerificationResult.getStatus(), ownerId.getUserId());

            processService.findProcessWithLock(idVerification.getProcessId());

            verificationProcessingService.processVerificationResult(ownerId, entry.getValue(), docVerificationResult);
        }

        if (allDocVerifications.stream()
                .anyMatch(docVerification -> docVerification.getStatus() == DocumentStatus.VERIFICATION_IN_PROGRESS)) {
            logger.debug("Some documents still VERIFICATION_IN_PROGRESS for identity verification ID: {}", idVerification.getId());
            return;
        }

        moveToDocumentVerificationAndStatusByDocuments(idVerification, allDocVerifications, ownerId);
    }

    /**
     * Process identity verification result for document verifications which have already been previously processed.
     * @param ownerId Owner identification.
     * @param idVerification Identity verification entity.
     * @throws RemoteCommunicationException Thrown when communication with PowerAuth server fails.
     */
    @Transactional
    public void processDocumentVerificationResult(final OwnerId ownerId, final IdentityVerificationEntity idVerification) throws RemoteCommunicationException {
        final var result = identityVerificationPrecompleteCheck.evaluate(idVerification);
        if (result.isSuccessful()) {
            logger.debug("Final validation passed, {}", ownerId);
            moveToPhaseAndStatus(idVerification, IdentityVerificationPhase.COMPLETED, ACCEPTED, ownerId);
        } else {
            logger.warn("Final validation did not pass, marking identity verification as failed due to '{}', {}", result.getErrorDetail(), ownerId);
            idVerification.setErrorDetail(ErrorDetail.DOCUMENT_VERIFICATION_FAILED);
            idVerification.setTimestampFailed(ownerId.getTimestamp());
            idVerification.setErrorOrigin(ErrorOrigin.FINAL_VALIDATION);
            moveToPhaseAndStatus(idVerification, IdentityVerificationPhase.COMPLETED, FAILED, ownerId);
        }
        idVerification.setTimestampFinished(ownerId.getTimestamp());
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        identityVerificationRepository.save(idVerification);
    }

    /**
     * Move identity verification to {@code DOCUMENT_VERIFICATION} phase and status based on the given document verifications.
     *
     * @param idVerification Identity verification entity.
     * @param docVerificationsToProcess Document verifications to determine identity verification status.
     */
    private void moveToDocumentVerificationAndStatusByDocuments(
            final IdentityVerificationEntity idVerification,
            final List<DocumentVerificationEntity> docVerificationsToProcess,
            final OwnerId ownerId) {

        final String identityVerificationId = idVerification.getId();
        // docVerificationsToProcess have been modified, so idVerification.getDocumentVerifications() must be reloaded to reflect these modifications
        final List<DocumentVerificationEntity> allDocumentVerifications = documentVerificationRepository.findAllUsedForVerification(idVerification);

        final boolean allRequiredDocumentsChecked = requiredDocumentTypesCheck.evaluate(allDocumentVerifications, identityVerificationId);
        if (!allRequiredDocumentsChecked) {
            logger.debug("Not all required document types are present yet for identity verification ID: {}", identityVerificationId);
        } else {
            logger.debug("All required document types are present for identity verification ID: {}", identityVerificationId);
        }

        if (docVerificationsToProcess.stream()
                .map(DocumentVerificationEntity::getStatus)
                .allMatch(it -> it == DocumentStatus.ACCEPTED)) {
            // The timestampFinished parameter is not set yet, there may be other steps ahead
            if (allRequiredDocumentsChecked) {
                // Move to DOCUMENT_VERIFICATION / ACCEPTED only in case all documents were checked
                moveToPhaseAndStatus(idVerification, IdentityVerificationPhase.DOCUMENT_VERIFICATION, ACCEPTED, ownerId);
            } else {
                // Identity verification status is changed to DOCUMENT_UPLOAD / IN_PROGRESS to allow submission of additional documents
                moveToDocumentUpload(ownerId, idVerification, IN_PROGRESS);
            }
        } else {
            // Identity verification status is changed to DOCUMENT_UPLOAD / IN_PROGRESS to allow re-submission of failed documents
            moveToDocumentUpload(ownerId, idVerification, IN_PROGRESS);
            handleDocumentStatus(docVerificationsToProcess, idVerification, DocumentStatus.FAILED, ownerId);
            handleDocumentStatus(docVerificationsToProcess, idVerification, DocumentStatus.REJECTED, ownerId);
        }
    }

    private void handleDocumentStatus(
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
                    handleLimitsForRejectOrFail(idVerification, status, ownerId);
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
     * Update process error score in case of a rejected or a failed verification and check process error limits.
     *
     * @param idVerification Identity verification entity.
     * @param status Identity verification status.
     * @param ownerId Owner identifier.
     */
    private void handleLimitsForRejectOrFail(IdentityVerificationEntity idVerification, DocumentStatus status, OwnerId ownerId) {
        if (status == DocumentStatus.FAILED || status == DocumentStatus.REJECTED) {
            final OnboardingProcessEntity process;
            try {
                process = processService.findProcess(idVerification.getProcessId());
            } catch (OnboardingProcessException e) {
                logger.trace("Onboarding process not found, {}", ownerId, e);
                logger.warn("Onboarding process not found, {}, {}", e.getMessage(), ownerId);
                return;
            }

            if (status == DocumentStatus.FAILED) {
                processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED, ownerId);
            }
            if (status == DocumentStatus.REJECTED) {
                processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_REJECTED, ownerId);
            }
            processLimitService.checkOnboardingProcessErrorLimits(process);
        }
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

        List<String> uploadIds = documentVerificationRepository.findAllUploadIds(ownerId.getActivationId());

        if (identityVerificationConfig.isDocumentVerificationCleanupEnabled()) {
            documentVerificationProvider.cleanupDocuments(ownerId, uploadIds);
            final IdentityVerificationEntity identityVerification = findBy(ownerId);
            auditService.auditDocumentVerificationProvider(identityVerification, "Cleaned up documents for user: {}", ownerId.getUserId());
        } else {
            logger.debug("Skipped cleanup of documents at document verification provider (not enabled), {}", ownerId);
        }

        // Delete all large documents by activation ID
        documentDataRepository.deleteAllByActivationId(ownerId.getActivationId());
        // Set status of all not finished document verifications to failed
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
     * Return all identity verifications eligible for change to next state.
     *
     * @return identity verifications
     */
    public Stream<IdentityVerificationEntity> streamAllIdentityVerificationsToChangeState() {
        return identityVerificationRepository.streamAllIdentityVerificationsToChangeState();
    }

    private void moveToDocumentUpload(final OwnerId ownerId, final IdentityVerificationEntity idVerification, final IdentityVerificationStatus status) {
        logger.debug("Moving phase to DOCUMENT_UPLOAD, {}", ownerId);
        moveToPhaseAndStatus(idVerification, DOCUMENT_UPLOAD, status, ownerId);
    }

    private List<String> collectRejectionErrors(DocumentVerificationEntity entity) {
        List<String> errors = new ArrayList<>();

        // Collect all rejection reasons from the latest document result
        Optional<DocumentResultEntity> docResultOptional = entity.getResults().stream().findFirst();
        if (docResultOptional.isPresent()) {
            DocumentResultEntity docResult = docResultOptional.get();
            List<String> rejectionReasons;
            try {
                rejectionReasons = documentVerificationProvider.parseRejectionReasons(docResult);
                final IdentityVerificationEntity identityVerification = docResult.getDocumentVerification().getIdentityVerification();
                auditService.auditDocumentVerificationProvider(identityVerification, "Check document upload for user: {}", identityVerification.getUserId());
            } catch (DocumentVerificationException e) {
                logger.debug("Parsing rejection reasons failure", e);
                logger.warn("Unable to parse rejection reasons from {} of a rejected {}", docResult, entity);
                return Collections.emptyList();
            }
            if (rejectionReasons.isEmpty()) {
                logger.warn("No rejection reasons found in {} of a rejected {}", docResult, entity);
            } else {
                errors.addAll(rejectionReasons);
            }
        } else {
            logger.warn("Missing document result for {}, defaulting errors to reject reason", entity);
            errors.add(entity.getRejectReason());
        }
        return errors;
    }

    /**
     * Create {@link DocumentMetadataResponseDto} from {@link DocumentVerificationEntity}
     * @param entity Document verification entity.
     * @return Document metadata for response
     */
    private DocumentMetadataResponseDto toDocumentMetadata(DocumentVerificationEntity entity) {
        DocumentMetadataResponseDto docMetadata = new DocumentMetadataResponseDto();
        docMetadata.setId(entity.getId());
        // Hide specific error reason if any.
        if (StringUtils.isNotBlank(entity.getErrorDetail()) || StringUtils.isNotBlank(entity.getRejectReason())) {
            docMetadata.setErrors(List.of("Error verifying the document."));
        }
        docMetadata.setFilename(entity.getFilename());
        docMetadata.setSide(entity.getSide());
        docMetadata.setStatus(entity.getStatus());
        docMetadata.setType(entity.getType());
        return docMetadata;
    }
}
