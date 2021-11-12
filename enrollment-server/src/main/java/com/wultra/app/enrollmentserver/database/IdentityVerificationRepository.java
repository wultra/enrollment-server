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

package com.wultra.app.enrollmentserver.database;

import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

/**
 * Repository for identity verification records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface IdentityVerificationRepository extends CrudRepository<IdentityVerificationEntity, String> {

    @Query("UPDATE IdentityVerificationEntity i " +
            "SET i.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED, " +
            "    i.timestampLastUpdated = :timestamp " +
            "WHERE i.activationId = :activationId " +
            "AND i.status = com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS")
    int failInProgressVerifications(String activationId, Date timestamp);

    @Modifying
    @Query("UPDATE IdentityVerificationEntity i " +
            "SET i.phase = :phase," +
            "    i.status = :status, " +
            "    i.timestampLastUpdated = :timestamp " +
            "WHERE i.activationId = :activationId")
    void setVerificationPhaseAndStatus(String activationId, IdentityVerificationPhase phase, IdentityVerificationStatus status, Date timestamp);

    Optional<IdentityVerificationEntity> findByActivationId(String activationId);

    Optional<IdentityVerificationEntity> findByActivationIdOrderByTimestampCreatedDesc(
            String activationId
    );

}
