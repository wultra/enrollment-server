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

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repository for identity verification records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface IdentityVerificationRepository extends CrudRepository<IdentityVerificationEntity, String> {

    Optional<IdentityVerificationEntity> findFirstByActivationIdOrderByTimestampCreatedDesc(String activationId);

    List<IdentityVerificationEntity> findByActivationIdOrderByTimestampCreatedDesc(String activationId);

    /**
     * @return All identity verification entities with in progress verification of uploaded documents
     */
    @Query("SELECT id FROM IdentityVerificationEntity id WHERE" +
            " id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_VERIFICATION" +
            " AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS " +
            " ORDER BY id.timestampLastUpdated ASC")
    Stream<IdentityVerificationEntity> streamAllInProgressDocumentsVerifications();

    /**
     * Return all identity verifications eligible for change to next state.
     *
     * @return identity verifications
     */
    @Query("SELECT id FROM IdentityVerificationEntity id WHERE" +
            " (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_UPLOAD" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_UPLOAD" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.VERIFICATION_PENDING)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_VERIFICATION" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED)" +
            " OR (id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK" +
            "   AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.VERIFICATION_PENDING)"
    )
    Stream<IdentityVerificationEntity> streamAllIdentityVerificationsToChangeState();


    /**
     * Return identity verification IDs by the given process ID. Include only not yet finished entities.
     *
     * @param processIds process IDs
     * @return identity verification IDs
     */
    @Query("SELECT i.id FROM IdentityVerificationEntity i " +
            "WHERE i.processId IN :processIds " +
            "AND i.phase <> com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.COMPLETED")
    List<String> findNotCompletedIdentityVerifications(Collection<String> processIds);

    /**
     * Return identity verification IDs created before the given timestamp.
     * Include only not yet finished entities.
     *
     * @param timestamp created timestamp must be older than the given timestamp
     * @return identity verification IDs
     */
    @Query("SELECT i.id FROM IdentityVerificationEntity i " +
            "WHERE i.timestampCreated < :timestamp " +
            "AND i.phase <> com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.COMPLETED")
    List<String> findNotCompletedIdentityVerifications(Date timestamp);

    /**
     * Mark the given identity verifications as failed.
     *
     * @param ids Identity verification IDs
     * @param timestampExpired last updated and failed timestamp
     * @param errorDetail error detail
     * @param errorOrigin error origin
     */
    @Modifying
    @Query("UPDATE IdentityVerificationEntity i SET " +
            "i.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.COMPLETED, " +
            "i.status = com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FAILED, " +
            "i.timestampLastUpdated = :timestampExpired, " +
            "i.timestampFailed = :timestampExpired, " +
            "i.errorDetail = :errorDetail, " +
            "i.errorOrigin = :errorOrigin " +
            "WHERE i.id IN :ids")
    void terminate(Collection<String> ids, Date timestampExpired, String errorDetail, ErrorOrigin errorOrigin);
}
