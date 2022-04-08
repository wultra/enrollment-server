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
package com.wultra.app.enrollmentserver.impl.service.verification;

import com.wultra.app.enrollmentserver.database.DocumentResultRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentResultEntity;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.integration.DocumentVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service implementing verification processing features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class VerificationProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationProcessingService.class);

    private final DocumentResultRepository documentResultRepository;

    /**
     * Service constructor.
     *
     * @param documentResultRepository Document result repository.
     */
    @Autowired
    public VerificationProcessingService(DocumentResultRepository documentResultRepository) {
        this.documentResultRepository = documentResultRepository;
    }

    /**
     * Processes documents verification result and updates the tracked verification state
     *
     * @param ownerId Owner identification.
     * @param docVerifications Tracked state of documents verification.
     * @param result Verification result from the provider.
     */
    public void processVerificationResult(OwnerId ownerId,
                                          List<DocumentVerificationEntity> docVerifications,
                                          DocumentsVerificationResult result) {
        if (DocumentVerificationStatus.IN_PROGRESS.equals(result.getStatus())) {
            logger.debug("Verification of the identity is still in progress, {}", ownerId);
            return;
        }

        for (DocumentVerificationEntity docVerification : docVerifications) {
            updateDocVerification(ownerId, docVerification, result);

            if (!DocumentStatus.FAILED.equals(docVerification.getStatus())) {
                Optional<DocumentVerificationResult> docVerificationResult = result.getResults().stream()
                        .filter(value -> value.getUploadId().equals(docVerification.getUploadId()))
                        .findFirst();
                if (docVerificationResult.isPresent()) {
                    DocumentResultEntity docResult = null;
                    try {
                        docResult = getDocumentResultEntity(ownerId, docVerification);
                    } catch (DocumentVerificationException e) {
                        logger.warn("Unable to get document result for {}, {}", docVerification, ownerId, e);
                        docVerification.setStatus(DocumentStatus.FAILED);
                    }
                    if (docResult != null) {
                        updateDocumentResult(docResult, docVerificationResult.get());
                        documentResultRepository.save(docResult);
                    }
                } else {
                    logger.error("Missing verification result for {} with uploadId: {}, {}",
                            docVerification, docVerification.getUploadId(), ownerId
                    );
                }
            }
        }
    }

    /**
     * Provides document result entity for verification data update
     *
     * @param ownerId Owner identification.
     * @param docVerification Document verification entity.
     * @return Document result entity to be updated with verification data
     */
    private DocumentResultEntity getDocumentResultEntity(OwnerId ownerId, DocumentVerificationEntity docVerification)
            throws DocumentVerificationException {
        IdentityVerificationPhase phase = docVerification.getIdentityVerification().getPhase();
        DocumentResultEntity docResult;
        if (IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(phase)) {
            List<DocumentResultEntity> docResults =
                    documentResultRepository.findLatestResults(docVerification.getId(), DocumentProcessingPhase.UPLOAD);
            if (docResults.isEmpty()) {
                logger.warn("No document result for upload of {}, creating a new one, {}", docVerification, ownerId);
                docResult = new DocumentResultEntity();
                docResult.setDocumentVerification(docVerification);
                docResult.setPhase(DocumentProcessingPhase.UPLOAD);
                docResult.setTimestampCreated(ownerId.getTimestamp());
            } else {
                docResult = docResults.get(0);
                if (docResults.size() > 1) {
                    logger.warn("Too many document results for upload of {}, count: {}, the latest wins, {}",
                            docVerification, docResults.size(), ownerId);
                }
            }
        } else if (IdentityVerificationPhase.DOCUMENT_VERIFICATION.equals(phase)) {
            docResult = new DocumentResultEntity();
            docResult.setDocumentVerification(docVerification);
            docResult.setPhase(DocumentProcessingPhase.VERIFICATION);
            docResult.setTimestampCreated(ownerId.getTimestamp());
        } else {
            throw new DocumentVerificationException("Unexpected identity verification phase: " + phase);
        }
        return docResult;
    }

    /**
     * Update document verification data
     * @param ownerId Owner identification.
     * @param docVerification Document verification entity.
     * @param docVerificationResult Document verification result.
     */
    private void updateDocVerification(OwnerId ownerId, DocumentVerificationEntity docVerification, DocumentsVerificationResult docVerificationResult) {
        docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        docVerification.setTimestampVerified(ownerId.getTimestamp());
        docVerification.setVerificationScore(docVerificationResult.getVerificationScore());
        switch (docVerificationResult.getStatus()) {
            case ACCEPTED:
                // document during upload is only partially verified and cannot be accepted now, it waits for the standard verification process
                if (IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(docVerification.getIdentityVerification().getPhase())) {
                    docVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
                } else {
                    docVerification.setStatus(DocumentStatus.ACCEPTED);
                }
                break;
            case FAILED:
                docVerification.setStatus(DocumentStatus.FAILED);
                docVerification.setErrorDetail(docVerificationResult.getErrorDetail());
                break;
            case REJECTED:
                docVerification.setStatus(DocumentStatus.REJECTED);
                docVerification.setRejectReason(docVerificationResult.getRejectReason());
                break;
            default:
                throw new IllegalStateException("Unexpected verification result status: " + docVerificationResult.getStatus());
        }
        logger.info("Finished verification of {} with status: {}, {}", docVerification, docVerification.getStatus(), ownerId);
    }

    /**
     * Updates document result with the verification result
     * @param docResult Document result.
     * @param docVerificationResult Document verification result.
     */
    private void updateDocumentResult(DocumentResultEntity docResult,
                                      DocumentVerificationResult docVerificationResult) {
        docResult.setErrorDetail(docVerificationResult.getErrorDetail());
        docResult.setRejectReason(docVerificationResult.getRejectReason());
        docResult.setVerificationResult(docVerificationResult.getVerificationResult());
    }

}
