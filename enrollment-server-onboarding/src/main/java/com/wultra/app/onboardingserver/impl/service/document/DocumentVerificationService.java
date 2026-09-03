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
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ErrorDetail;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.impl.service.OnboardingEventService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Document verification service providing {@link #executeFinalDocumentVerification(IdentityVerificationEntity, OwnerId)}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class DocumentVerificationService {

    private final DocumentVerificationProvider documentVerificationProvider;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final OnboardingProcessLimitService processLimitService;

    private final AuditService auditService;

    private final OnboardingEventService onboardingEventService;

    /**
     * Execute final document verification of the given identity verification.
     * <p>
     * Based on the result of calling a document verification provider, change the identity verification status.
     * Also change status of the document verifications accordingly.
     *
     * @param identityVerification Identification verification whose documents should be verified
     * @param ownerId Owner identification
     * @return Result of the final document verification.
     */
    public FinalDocumentVerificationResult executeFinalDocumentVerification(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        logger.info("action: executeFinalDocumentVerification, state: initiated, identityVerificationId: {}", identityVerification.getId());

        final List<DocumentVerificationEntity> documentVerifications = filterDocumentVerifications(identityVerification);

        final List<String> uploadIds = documentVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .toList();

        final DocumentsVerificationResult documentsVerificationResult;
        try {
            documentsVerificationResult = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);
        } catch (RemoteCommunicationException | DocumentVerificationException e) {
            logger.error("action: executeFinalDocumentVerification, state: failed, exceptionMessage: {}", e.getMessage(), e);
            return fail(identityVerification, e.getMessage(), documentVerifications, ownerId);
        }

        final String verificationId = documentsVerificationResult.getVerificationId();
        final DocumentVerificationStatus status = documentsVerificationResult.getStatus();
        logger.info("Cross verified documents upload ID: {}, verification ID: {}, status: {}, {}", uploadIds, verificationId, status, ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Cross verified documents: {} for user: {}", status, ownerId.getUserId());

        documentVerifications.forEach(docVerification -> {
            docVerification.setVerificationId(documentsVerificationResult.getVerificationId());
            docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        });

        final var result = switch (status) {
            case ACCEPTED -> accept(identityVerification, documentVerifications);
            case FAILED -> fail(identityVerification, documentsVerificationResult.getErrorDetail(), documentVerifications, ownerId);
            case REJECTED -> reject(identityVerification, documentsVerificationResult, documentVerifications, ownerId);
            // Only sync mode is supported
            case IN_PROGRESS -> FinalDocumentVerificationResult.FAILED;
        };
        logger.info("action: executeFinalDocumentVerification, state: succeeded, identityVerificationId: {}, documentsVerificationResult: {}", identityVerification.getId(), result);
        return result;
    }

    /**
     * Check if there are any document verifications in status {@code VERIFICATION_PENDING}.
     *
     * @param identityVerification Identification verification whose documents should be checked
     * @return {@code true} if there are any document verifications in status {@code VERIFICATION_PENDING}, {@code false} otherwise.
     */
    @Transactional(readOnly = true)
    public boolean hasDocumentsVerificationPending(final IdentityVerificationEntity identityVerification) {
        final List<DocumentVerificationEntity> documentVerifications = documentVerificationRepository.findAllUsedForVerification(identityVerification);
        return documentVerifications.stream()
                .anyMatch(it -> it.getStatus() == DocumentStatus.VERIFICATION_PENDING);
    }

    private FinalDocumentVerificationResult accept(
            final IdentityVerificationEntity identityVerification,
            final List<DocumentVerificationEntity> documentVerifications) {

        documentVerifications.forEach(docVerification ->
            auditService.audit(docVerification, "Document accepted at final verification for user: {}", identityVerification.getUserId()));
        onboardingEventService.publishFinalDocumentVerificationAccepted(identityVerification);
        return FinalDocumentVerificationResult.OK;
    }

    private FinalDocumentVerificationResult reject(
            final IdentityVerificationEntity identityVerification,
            final DocumentsVerificationResult result,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) {

        final String rejectReason = StringUtils.defaultIfBlank(
                result.getRejectReason(),
                ErrorDetail.DOCUMENT_VERIFICATION_REJECTED);
        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.REJECTED);
            docVerification.setRejectReason(rejectReason);
            docVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
            logger.info("Document verification ID: {} rejected: {}, {}", docVerification.getId(), rejectReason, ownerId);
            auditService.audit(docVerification, "Document rejected at final verification for user: {}", identityVerification.getUserId());
        });

        logger.info("Identity verification ID: {} rejected: {}, {}", identityVerification.getId(), rejectReason, ownerId);
        identityVerification.setRejectReason(ErrorDetail.DOCUMENT_VERIFICATION_REJECTED);
        identityVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());

        incrementErrorScore(identityVerification, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_REJECTED, ownerId);

        onboardingEventService.publishFinalDocumentVerificationRejected(identityVerification, rejectReason);
        return FinalDocumentVerificationResult.REJECTED;
    }

    private FinalDocumentVerificationResult fail(
            final IdentityVerificationEntity identityVerification,
            final String errorDetail,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) {

        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.FAILED);
            docVerification.setErrorDetail(errorDetail);
            docVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
            logger.info("Document verification ID: {}, failed: {}, {}", docVerification.getId(), errorDetail, ownerId);
            auditService.audit(docVerification, "Document failed at final verification for user: {}", identityVerification.getUserId());
        });

        identityVerification.setErrorDetail(errorDetail);
        identityVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
        logger.info("Identity verification ID: {}, failed: {}, {}", identityVerification.getId(), errorDetail, ownerId);

        incrementErrorScore(identityVerification, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED, ownerId);

        onboardingEventService.publishFinalDocumentVerificationFailed(identityVerification, errorDetail);
        return FinalDocumentVerificationResult.FAILED;
    }

    private void incrementErrorScore(
            final IdentityVerificationEntity identityVerification,
            final OnboardingProcessError error,
            final OwnerId ownerId) {

        final Optional<OnboardingProcessEntity> process = onboardingProcessRepository.findById(identityVerification.getProcessId());
        if (process.isEmpty()) {
            // it should never happen in this workflow phase, but make it robust
            logger.error("action: incrementErrorScore, state: failed, reason: process not found, processId: {}", identityVerification.getProcessId());
            return;
        }

        processLimitService.incrementErrorScore(process.get(), error, ownerId);
        processLimitService.checkOnboardingProcessErrorLimits(process.get());
    }

    private static List<DocumentVerificationEntity> filterDocumentVerifications(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .toList();
    }

    public enum FinalDocumentVerificationResult {
        OK,
        REJECTED,
        FAILED
    }
}
