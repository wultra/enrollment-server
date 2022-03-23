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

import com.wultra.app.enrollmentserver.configuration.IdentityVerificationConfig;
import com.wultra.app.enrollmentserver.database.DocumentDataRepository;
import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Service implementing background tasks related to document status update.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class DocumentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStatusService.class);

    public static final String MESSAGE_OBSOLETE_VERIFICATION_PROCESS = "Obsolete verification process";

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
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    public void cleanupLargeDocuments() {
        documentDataRepository.cleanupDocumentData(getDataRetentionTimestamp());
    }

    @Transactional
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    public void cleanupObsoleteVerificationProcesses() {
        int count = documentVerificationRepository.failObsoleteVerifications(
                getVerificationExpirationTimestamp(),
                new Date(),
                MESSAGE_OBSOLETE_VERIFICATION_PROCESS,
                DocumentStatus.ALL_NOT_FINISHED
        );
        if (count > 0) {
            logger.info("Failed {} obsolete verification processes", count);
        }
    }

    private Date getDataRetentionTimestamp() {
        int retentionTime = identityVerificationConfig.getDataRetentionTime();
        Calendar dateRetention = GregorianCalendar.getInstance();
        dateRetention.add(Calendar.HOUR, retentionTime);
        return dateRetention.getTime();
    }

    private Date getVerificationExpirationTimestamp() {
        int expirationTime = identityVerificationConfig.getVerificationExpirationTime();
        Calendar dateExpiration = GregorianCalendar.getInstance();
        dateExpiration.add(Calendar.SECOND, expirationTime);
        return dateExpiration.getTime();
    }

}
