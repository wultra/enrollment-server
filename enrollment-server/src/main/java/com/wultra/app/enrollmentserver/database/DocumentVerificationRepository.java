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

import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository for document verification records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface DocumentVerificationRepository extends JpaRepository<DocumentVerificationEntity, String> {

    // TODO update timestampLastUpdated

    @Modifying
    @Query("UPDATE DocumentVerificationEntity d SET d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.FAILED " +
            "WHERE d.activationId = :activationId " +
            "AND d.status IN (com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.UPLOAD_IN_PROGRESS, com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.VERIFICATION_IN_PROGRESS)")
    int failInProgressVerifications(String activationId);

    @Modifying
    @Query("UPDATE DocumentVerificationEntity d SET d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.FAILED " +
            "WHERE d.timestampLastUpdated < :cleanupDate " +
            "AND d.status IN (com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.UPLOAD_IN_PROGRESS, com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.VERIFICATION_IN_PROGRESS)")
    int failObsoleteVerifications(Date cleanupDate);

    @Modifying
    @Query("UPDATE DocumentVerificationEntity d " +
            "SET d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.VERIFICATION_PENDING " +
            "WHERE d.activationId = :activationId")
    int setVerificationPending(String activationId);

    @Modifying
    @Query("UPDATE DocumentVerificationEntity d " +
            "SET d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.VERIFICATION_IN_PROGRESS " +
            "WHERE d.id IN :docVerificationIds")
    int setVerificationInProgress(List<String> docVerificationIds);

    @Query("SELECT d " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.activationId = :activationId " +
            "AND d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.VERIFICATION_PENDING")
    List<DocumentVerificationEntity> findAllPendingVerifications(String activationId);

    @Query("SELECT d " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.activationId = :activationId " +
            "AND d.usedForVerification = true")
    List<DocumentVerificationEntity> findAllUsedForVerification(String activationId);

    @Query("SELECT d.uploadId " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.activationId = :activationId")
    List<String> findAllUploadIds(String activationId);

    Optional<DocumentVerificationEntity> findByActivationIdAndPhotoIdNotNull(String activationId);

}
