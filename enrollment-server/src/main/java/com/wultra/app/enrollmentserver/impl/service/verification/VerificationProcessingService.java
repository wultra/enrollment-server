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
import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
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
            docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            docVerification.setTimestampVerified(ownerId.getTimestamp());
            docVerification.setVerificationScore(result.getVerificationScore());
            switch (result.getStatus()) {
                case ACCEPTED:
                    docVerification.setStatus(DocumentStatus.ACCEPTED);
                    break;
                case FAILED:
                    docVerification.setStatus(DocumentStatus.FAILED);
                    docVerification.setErrorDetail(result.getErrorDetail());
                    break;
                case REJECTED:
                    docVerification.setStatus(DocumentStatus.REJECTED);
                    docVerification.setRejectReason(result.getRejectReason());
                    break;
                default:
                    throw new IllegalStateException("Unexpected verification result status: " + result.getStatus());
            }
            logger.info("Finished verification of {} with status: {}, {}", docVerification, result.getStatus(), ownerId);

            Optional<DocumentVerificationResult> docResult = result.getResults().stream()
                    .filter(value -> value.getUploadId().equals(docVerification.getUploadId()))
                    .findFirst();
            if (docResult.isPresent()) {
                DocumentResultEntity docResultEntity = createDocumentResult(docResult.get());
                docResultEntity.setDocumentVerificationId(docVerification.getId());
                docResultEntity.setTimestampCreated(ownerId.getTimestamp());
                documentResultRepository.save(docResultEntity);
            } else {
                logger.error("Missing verification result for {} with uploadId: {}, {}",
                        docVerification, docVerification.getUploadId(), ownerId
                );
            }
        }
    }

    private DocumentResultEntity createDocumentResult(
            DocumentVerificationResult docVerificationResult) {
        DocumentResultEntity entity = new DocumentResultEntity();
        entity.setErrorDetail(docVerificationResult.getErrorDetail());
        entity.setPhase(DocumentProcessingPhase.VERIFICATION);
        entity.setRejectReason(docVerificationResult.getRejectReason());
        entity.setVerificationResult(docVerificationResult.getVerificationResult());
        return entity;
    }

}
