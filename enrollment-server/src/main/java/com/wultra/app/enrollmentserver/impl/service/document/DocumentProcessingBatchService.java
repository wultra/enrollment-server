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
package com.wultra.app.enrollmentserver.impl.service.document;

import com.wultra.app.enrollmentserver.database.DocumentResultRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentResultEntity;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public DocumentProcessingBatchService(DocumentResultRepository documentResultRepository,
                                          DocumentProcessingService documentProcessingService) {
        this.documentResultRepository = documentResultRepository;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Checks in progress document submits on current provider status and data result
     */
    @Transactional
    public void checkInProgressDocumentSubmits() {
        try (Stream<DocumentResultEntity> stream = documentResultRepository.streamAllInProgressDocumentSubmits()) {
            stream.forEach(docResult -> {
                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(docResult.getDocumentVerification().getActivationId());
                ownerId.setUserId("wultra-enrollment-server-task");

                try {
                    this.documentProcessingService.checkDocumentSubmitWithProvider(ownerId, docResult);
                } catch (Exception e) {
                    logger.error("Unable to check submit status of {} at provider, {}", docResult, ownerId);
                }
            });
        }
    }

}
