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

import com.wultra.app.onboardingserver.database.DocumentResultRepository;
import com.wultra.app.onboardingserver.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service implementing document processing features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class DocumentProcessingBatchService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingBatchService.class);

    private final DocumentResultRepository documentResultRepository;

    private final DocumentProcessingService documentProcessingService;

    /**
     * Service constructor.
     * @param documentResultRepository Document verification result repository.
     * @param documentProcessingService Document processing service.
     */
    @Autowired
    public DocumentProcessingBatchService(
            DocumentResultRepository documentResultRepository,
            DocumentProcessingService documentProcessingService) {
        this.documentResultRepository = documentResultRepository;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Checks in progress document submits on current provider status and data result
     */
    @Transactional
    public void checkInProgressDocumentSubmits() {
        AtomicInteger countFinished = new AtomicInteger(0);
        try (Stream<DocumentResultEntity> stream = documentResultRepository.streamAllInProgressDocumentSubmits()) {
            stream.forEach(docResult -> {
                DocumentVerificationEntity docVerification = docResult.getDocumentVerification();
                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(docVerification.getActivationId());
                ownerId.setUserId("server-task-in-progress-submits");

                try {
                    this.documentProcessingService.checkDocumentSubmitWithProvider(ownerId, docResult);
                } catch (Exception e) {
                    logger.error("Unable to check submit status of {} at provider, {}", docResult, ownerId);
                }

                if (!DocumentStatus.UPLOAD_IN_PROGRESS.equals(docVerification.getStatus())) {
                    logger.debug("Synced {} status to {} with the provider, {}", docVerification, docVerification.getStatus(), ownerId);
                    countFinished.incrementAndGet();
                }
            });
        }
        if (countFinished.get() > 0) {
            logger.debug("Finished {} documents which were in progress", countFinished.get());
        }
    }

}
