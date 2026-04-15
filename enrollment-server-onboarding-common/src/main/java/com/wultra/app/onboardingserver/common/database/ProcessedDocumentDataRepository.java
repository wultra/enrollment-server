/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Repository for processed document data records.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Repository
public interface ProcessedDocumentDataRepository extends CrudRepository<ProcessedDocumentDataEntity, String> {

    /**
     * Deletes all records created before the specified date.
     *
     * @param dateCleanup Date to clean up records before
     * @return Number of deleted records.
     */
    @Modifying
    @Query("DELETE FROM ProcessedDocumentDataEntity p WHERE p.timestampCreated < :dateCleanup")
    int cleanup(final Date dateCleanup);

    @Query("SELECT p FROM ProcessedDocumentDataEntity p WHERE p.documentVerificationId IN :documentVerificationIds")
    List<ProcessedDocumentDataEntity> findAllByDocumentVerificationIds(final Set<String> documentVerificationIds);

    /**
     * Deletes all records by document verification IDs.
     *
     * @param documentVerificationIds document verification IDs to be deleted
     */
    @Modifying
    @Query("DELETE FROM ProcessedDocumentDataEntity p WHERE p.documentVerificationId IN :documentVerificationIds")
    void deleteAllByDocumentVerificationIds(final List<String> documentVerificationIds);
}
