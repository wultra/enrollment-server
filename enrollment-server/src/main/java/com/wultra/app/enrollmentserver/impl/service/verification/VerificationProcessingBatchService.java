/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.provider.DocumentVerificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service implementing verification processing features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class VerificationProcessingBatchService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationProcessingBatchService.class);

    private final DocumentResultRepository documentResultRepository;

    private final DocumentVerificationProvider documentVerificationProvider;

    private final VerificationProcessingService verificationProcessingService;

    /**
     * Service constructor.
     * @param documentResultRepository Document verification result repository.
     * @param documentVerificationProvider Document verification provider.
     * @param verificationProcessingService Verification processing service.
     */
    @Autowired
    public VerificationProcessingBatchService(
            DocumentResultRepository documentResultRepository,
            DocumentVerificationProvider documentVerificationProvider,
            VerificationProcessingService verificationProcessingService) {
        this.documentResultRepository = documentResultRepository;
        this.documentVerificationProvider = documentVerificationProvider;
        this.verificationProcessingService = verificationProcessingService;
    }

    /**
     * Checks document submit verifications
     */
    @Transactional
    public void checkDocumentSubmitVerifications() {
        AtomicInteger countFinished = new AtomicInteger(0);
        try (Stream<DocumentResultEntity> stream = documentResultRepository.streamAllInProgressDocumentSubmitVerifications()) {
            stream.forEach(docResult -> {
                DocumentVerificationEntity docVerification = docResult.getDocumentVerification();

                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(docVerification.getActivationId());
                ownerId.setUserId("server-task-doc-submit-verifications");

                DocumentsVerificationResult docVerificationResult;
                try {
                    docVerificationResult = documentVerificationProvider.getVerificationResult(ownerId, docVerification.getVerificationId());
                } catch (DocumentVerificationException e) {
                    logger.error("Checking document submit verification failed, " + ownerId, e);
                    return;
                }
                verificationProcessingService.processVerificationResult(ownerId, List.of(docVerification), docVerificationResult);

                if (!DocumentStatus.UPLOAD_IN_PROGRESS.equals(docVerification.getStatus())) {
                    logger.debug("Finished verification of {} during submit at the provider, {}", docVerification, ownerId);
                    countFinished.incrementAndGet();
                }
            });
        }
        if (countFinished.get() > 0) {
            logger.debug("Finished {} documents verifications during submit", countFinished.get());
        }
    }

}
