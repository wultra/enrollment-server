/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
