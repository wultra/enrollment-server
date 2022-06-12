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

package com.wultra.app.onboardingserver.database;

import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repository for identity verification records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface IdentityVerificationRepository extends CrudRepository<IdentityVerificationEntity, String> {

    @Modifying
    @Query("UPDATE IdentityVerificationEntity i " +
            "SET i.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED, " +
            "    i.timestampLastUpdated = :timestamp " +
            "WHERE i.activationId = :activationId " +
            "AND i.status IN (" +
                "com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS, " +
                "com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.VERIFICATION_PENDING, " +
                "com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.OTP_VERIFICATION_PENDING" +
            ")")
    int failRunningVerifications(String activationId, Date timestamp);

    Optional<IdentityVerificationEntity> findFirstByActivationIdOrderByTimestampCreatedDesc(
            String activationId
    );

    /**
     * @return All identity verification entities with in progress verification of uploaded documents
     */
    @Query("SELECT id FROM IdentityVerificationEntity id WHERE" +
            " id.phase = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_VERIFICATION" +
            " AND id.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS " +
            " ORDER BY id.timestampLastUpdated ASC")
    Stream<IdentityVerificationEntity> streamAllInProgressDocumentsVerifications();

}
