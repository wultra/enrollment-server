/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.impl.service.document;

import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.provider.DocumentVerificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;
import static java.util.stream.Collectors.toList;

/**
 * Document verification service providing {@link #executeFinalDocumentVerification(IdentityVerificationEntity, OwnerId)}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
public class DocumentVerificationService {

    private final DocumentVerificationProvider documentVerificationProvider;

    private final IdentityVerificationService identityVerificationService;

    private final CommonOnboardingService processService;

    private final OnboardingProcessLimitService processLimitService;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final IdentityVerificationConfig identityVerificationConfig;

    private final DocumentProcessingService documentProcessingService;

    private final AuditService auditService;

    @Autowired
    public DocumentVerificationService(
            final DocumentVerificationProvider documentVerificationProvider,
            final IdentityVerificationService identityVerificationService,
            final CommonOnboardingService processService,
            final OnboardingProcessLimitService processLimitService,
            final DocumentVerificationRepository documentVerificationRepository,
            final IdentityVerificationConfig identityVerificationConfig,
            final DocumentProcessingService documentProcessingService,
            final AuditService auditService) {

        this.documentVerificationProvider = documentVerificationProvider;
        this.identityVerificationService = identityVerificationService;
        this.processService = processService;
        this.processLimitService = processLimitService;
        this.documentVerificationRepository = documentVerificationRepository;
        this.identityVerificationConfig = identityVerificationConfig;
        this.documentProcessingService = documentProcessingService;
        this.auditService = auditService;
    }

    /**
     * Execute final document verification of the given identity verification.
     * <p>
     * Based on the result of calling document verification provider, change the identity verification status.
     * Also change status of the document verifications accordingly.
     *
     * @param identityVerification Identification verification whose documents should be verified
     * @param ownerId Owner identification
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     * @throws OnboardingProcessException When process not found
     */
    public void executeFinalDocumentVerification(final IdentityVerificationEntity identityVerification, final OwnerId ownerId)
            throws RemoteCommunicationException, DocumentVerificationException, OnboardingProcessException {

        final List<DocumentVerificationEntity> documentVerifications = filterDocumentVerifications(identityVerification);

        final List<String> uploadIds = documentVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .collect(toList());

        final DocumentsVerificationResult result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);
        final String verificationId = result.getVerificationId();
        final DocumentVerificationStatus status = result.getStatus();
        logger.info("Cross verified documents upload ID: {}, verification ID: {}, status: {}, {}", uploadIds, verificationId, status, ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Cross verified documents: {} for user: {}", status, ownerId.getUserId());

        changeDocumentVerificationStatusAndVerificationId(documentVerifications, result, identityVerification, ownerId);
        moveIdentityVerificationToDocumentVerificationFinal(result, identityVerification, ownerId);
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
    public void startVerification(OwnerId ownerId, IdentityVerificationEntity identityVerification) throws DocumentVerificationException, RemoteCommunicationException, OnboardingProcessException {
        final List<DocumentVerificationEntity> docVerifications =
                documentVerificationRepository.findAllDocumentVerifications(identityVerification, List.of(DocumentStatus.VERIFICATION_PENDING));

        final List<DocumentVerificationEntity> selfiePhotoVerifications = docVerifications.stream()
                        .filter(entity -> entity.getType() == DocumentType.SELFIE_PHOTO)
                        .collect(toList());

        // If not enabled then remove selfie photos from the verification process
        if (!identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()) {
            docVerifications.removeAll(selfiePhotoVerifications);
        }

        documentProcessingService.pairTwoSidedDocuments(docVerifications);

        List<String> uploadIds = docVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .collect(toList());

        final DocumentsVerificationResult result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);
        final String verificationId = result.getVerificationId();
        final DocumentVerificationStatus status = result.getStatus();
        logger.info("Verified documents upload ID: {}, verification ID: {}, status: {}, {}", uploadIds, verificationId, status, ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Documents verified: {} for user: {}", status, ownerId.getUserId());

        if (!identityVerificationConfig.isVerifySelfieWithDocumentsEnabled()) {
            logger.debug("Selfie photos verification disabled, changing selfie document status to ACCEPTED, {}", ownerId);
            selfiePhotoVerifications.forEach(selfiePhotoVerification -> {
                selfiePhotoVerification.setStatus(DocumentStatus.ACCEPTED);
                selfiePhotoVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                auditService.audit(selfiePhotoVerification, "Selfie document accepted for user: {}", ownerId.getUserId());
            });
            documentVerificationRepository.saveAll(selfiePhotoVerifications);
        }

        changeDocumentVerificationStatusAndVerificationId(docVerifications, result, identityVerification, ownerId);

        identityVerificationService.moveToPhaseAndStatus(identityVerification, IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.IN_PROGRESS, ownerId);
    }

    /**
     * Change status of the given document verification entities base on the give verification result and set the verification ID.
     * Increment error score in case of failed or rejected documents.
     * Async processing is not supported yet.
     */
    private void changeDocumentVerificationStatusAndVerificationId(
            final List<DocumentVerificationEntity> documentVerifications,
            final DocumentsVerificationResult result,
            final IdentityVerificationEntity identityVerification,
            final OwnerId ownerId) throws OnboardingProcessException, DocumentVerificationException {

        documentVerifications.forEach(docVerification -> {
            docVerification.setVerificationId(result.getVerificationId());
            docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        });

        final DocumentVerificationStatus status = result.getStatus();

        switch (status) {
            case ACCEPTED:
                accept(identityVerification, documentVerifications);
                break;
            case FAILED:
                fail(identityVerification, result, documentVerifications, ownerId);
                break;
            case REJECTED:
                reject(identityVerification, result, documentVerifications, ownerId);
                break;
            case IN_PROGRESS:
                throw new DocumentVerificationException("Only sync mode is supported, " + ownerId);
            default:
                throw new DocumentVerificationException(String.format("Not supported status %s, %s", status, ownerId));
        }
    }

    /**
     * Move the given identity verification to {@code DOCUMENT_VERIFICATION_FINAL} phase
     * and set its status according to the given document verification result.
     * Async processing is not supported yet.
     */
    private void moveIdentityVerificationToDocumentVerificationFinal(
            final DocumentsVerificationResult result,
            final IdentityVerificationEntity identityVerification,
            final OwnerId ownerId) throws DocumentVerificationException {

        final DocumentVerificationStatus status = result.getStatus();
        final IdentityVerificationPhase phase = IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL;

        switch (status) {
            case ACCEPTED:
                identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, ACCEPTED, ownerId);
                break;
            case FAILED:
                identityVerification.setErrorDetail(result.getErrorDetail());
                identityVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
                identityVerification.setTimestampFailed(ownerId.getTimestamp());
                identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, FAILED, ownerId);
                break;
            case REJECTED:
                identityVerification.setRejectReason(result.getRejectReason());
                identityVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
                identityVerification.setTimestampFailed(ownerId.getTimestamp());
                identityVerificationService.moveToPhaseAndStatus(identityVerification, phase, REJECTED, ownerId);
                break;
            case IN_PROGRESS:
                throw new DocumentVerificationException("Only sync mode is supported, " + ownerId);
            default:
                throw new DocumentVerificationException(String.format("Not supported status %s, %s", status, ownerId));
        }
    }

    private void accept(
            final IdentityVerificationEntity identityVerification,
            final List<DocumentVerificationEntity> documentVerifications) {

        final IdentityVerificationPhase phase = identityVerification.getPhase();
        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.ACCEPTED);
            auditService.audit(docVerification, "Document accepted at phase {} for user: {}", phase, identityVerification.getUserId());
        });
    }

    private void reject(
            final IdentityVerificationEntity identityVerification,
            final DocumentsVerificationResult result,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) throws OnboardingProcessException {

        final IdentityVerificationPhase phase = identityVerification.getPhase();

        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.REJECTED);
            docVerification.setRejectReason(result.getRejectReason());
            docVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
            auditService.audit(docVerification, "Document rejected at phase {} for user: {}", phase, identityVerification.getUserId());
        });

        incrementErrorScore(identityVerification, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_REJECTED, ownerId);
    }

    private void fail(
            final IdentityVerificationEntity identityVerification,
            final DocumentsVerificationResult result,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) throws OnboardingProcessException {

        final IdentityVerificationPhase phase = identityVerification.getPhase();

        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.FAILED);
            docVerification.setErrorDetail(result.getErrorDetail());
            docVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
            auditService.audit(docVerification, "Document failed at phase {} for user: {}", phase, identityVerification.getUserId());
        });

        incrementErrorScore(identityVerification, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED, ownerId);
    }

    private void incrementErrorScore(
            final IdentityVerificationEntity identityVerification,
            final OnboardingProcessError error,
            final OwnerId ownerId) throws OnboardingProcessException {

        final OnboardingProcessEntity process = processService.findProcess(identityVerification.getProcessId());
        processLimitService.incrementErrorScore(process, error, ownerId);
        processLimitService.checkOnboardingProcessErrorLimits(process);
    }

    private static List<DocumentVerificationEntity> filterDocumentVerifications(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(toList());
    }
}
