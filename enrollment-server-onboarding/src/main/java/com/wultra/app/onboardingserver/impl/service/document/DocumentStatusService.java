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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.impl.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Service implementing background tasks related to document status update.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class DocumentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStatusService.class);

    public static final String ERROR_MESSAGE_DOCUMENT_VERIFICATION_EXPIRED = "expired";

    private final DocumentVerificationRepository documentVerificationRepository;
    private final DocumentDataRepository documentDataRepository;
    private final IdentityVerificationConfig identityVerificationConfig;

    /**
     * Service constructor.
     * @param documentVerificationRepository Document verification repository.
     * @param documentDataRepository Document data repository.
     * @param identityVerificationConfig Identity verification configuration.
     */
    @Autowired
    public DocumentStatusService(DocumentVerificationRepository documentVerificationRepository, DocumentDataRepository documentDataRepository, IdentityVerificationConfig identityVerificationConfig) {
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentDataRepository = documentDataRepository;
        this.identityVerificationConfig = identityVerificationConfig;
    }

    /**
     * Cleanup of large documents older than retention time.
     */
    @Transactional
    public void cleanupLargeDocuments() {
        documentDataRepository.cleanupDocumentData(getDataRetentionTime());
    }

    @Transactional
    public void cleanupExpiredVerificationProcesses() {
        int count = documentVerificationRepository.failExpiredVerifications(
                getVerificationExpirationTime(),
                new Date(),
                ERROR_MESSAGE_DOCUMENT_VERIFICATION_EXPIRED,
                ErrorOrigin.PROCESS_LIMIT_CHECK,
                DocumentStatus.ALL_NOT_FINISHED
        );
        if (count > 0) {
            logger.info("Failed {} obsolete verification processes", count);
        }
    }

    private Date getDataRetentionTime() {
        return DateUtil.convertExpirationToCreatedDate(identityVerificationConfig.getDataRetentionTime());
    }

    private Date getVerificationExpirationTime() {
        return DateUtil.convertExpirationToCreatedDate(identityVerificationConfig.getVerificationExpirationTime());
    }

}
