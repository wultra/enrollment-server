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

package com.wultra.app.onboardingserver.common.database;

import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

/**
 * Repository for document verification result records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface DocumentResultRepository extends CrudRepository<DocumentResultEntity, Long> {

    /**
     * @return All not finished document uploads (upload is in progress and no extracted data filled)
     */
    @Query("SELECT doc FROM DocumentResultEntity doc WHERE" +
            " doc.documentVerification.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.UPLOAD_IN_PROGRESS" +
            " AND doc.documentVerification.providerName = :providerName " +
            " AND doc.extractedData IS NULL " +
            " ORDER BY doc.timestampCreated ASC")
    Stream<DocumentResultEntity> streamAllInProgressDocumentSubmits(String providerName);

    /**
     * @return All not finished document submit verifications (upload is in progress and verification id exists)
     */
    @Query("SELECT doc FROM DocumentResultEntity doc WHERE" +
            " doc.documentVerification.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.UPLOAD_IN_PROGRESS" +
            " AND doc.documentVerification.providerName = :providerName " +
            " AND doc.documentVerification.verificationId IS NOT NULL" +
            " ORDER BY doc.timestampCreated ASC")
    Stream<DocumentResultEntity> streamAllInProgressDocumentSubmitVerifications(String providerName);

    /**
     * @return All document results for the specified document verification and processing phase
     */
    @Query("SELECT doc FROM DocumentResultEntity doc WHERE" +
            " doc.documentVerification.id = :docVerificationId" +
            " AND doc.phase = :phase" +
            " ORDER BY doc.timestampCreated DESC")
    List<DocumentResultEntity> findLatestResults(String docVerificationId, DocumentProcessingPhase phase);

}
