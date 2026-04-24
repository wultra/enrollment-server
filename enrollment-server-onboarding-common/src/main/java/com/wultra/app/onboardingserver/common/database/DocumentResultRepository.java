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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Repository for document verification result records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface DocumentResultRepository extends CrudRepository<DocumentResultEntity, Long> {

    /**
     * @return All document results for the specified document verification and processing phase
     */
    @Query("SELECT doc FROM DocumentResultEntity doc WHERE" +
            " doc.documentVerification.id = :docVerificationId" +
            " AND doc.phase = :phase" +
            " ORDER BY doc.timestampCreated DESC")
    List<DocumentResultEntity> findLatestResults(String docVerificationId, DocumentProcessingPhase phase);

    /**
     * Clean personal data for records with given document verification IDs.
     *
     * @param documentVerificationIds Document verification IDs
     */
    @Modifying
    @Query("""
            UPDATE DocumentResultEntity d
            SET d.verificationResult = null,
                d.extractedData = null,
                d.anonymized = true
            WHERE d.documentVerification.id IN :documentVerificationIds
    """)
    void clean(final Collection<String> documentVerificationIds);

    /**
     * Clean personal data for records older than the specified date.
     *
     * @param dateCleanup Date older than which the records should be cleaned.
     * @return Number of records cleaned.
     */
    @Modifying
    @Query("""
            UPDATE DocumentResultEntity d
            SET d.verificationResult = null,
                d.extractedData = null,
                d.anonymized = true
            WHERE d.anonymized = false
            AND d.timestampCreated < :dateCleanup
    """)
    int cleanPersonalData(final Date dateCleanup);
}
