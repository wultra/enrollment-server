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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Repository for document verification records.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface DocumentVerificationRepository extends JpaRepository<DocumentVerificationEntity, String> {

    @Modifying
    @Query("UPDATE DocumentVerificationEntity d " +
            "SET d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.FAILED, " +
            "    d.usedForVerification = false, " +
            "    d.timestampLastUpdated = :timestamp " +
            "WHERE d.activationId = :activationId " +
            "AND d.status IN :statuses")
    int failVerifications(String activationId, Date timestamp, List<DocumentStatus> statuses);

    @Query("SELECT d.id FROM DocumentVerificationEntity d " +
            "WHERE d.timestampCreated < :cleanupDate " +
            "AND d.status IN :statuses")
    List<String> findExpiredVerifications(Date cleanupDate, List<DocumentStatus> statuses);

    @Modifying
    @Query("UPDATE DocumentVerificationEntity d " +
            "SET d.otherSideId = :otherSideId " +
            "WHERE d.id = :id")
    int setOtherDocumentSide(String id, String otherSideId);

    @Query("SELECT d " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.identityVerification = :identityVerification " +
            "AND d.status IN :statuses")
    List<DocumentVerificationEntity> findAllDocumentVerifications(IdentityVerificationEntity identityVerification, List<DocumentStatus> statuses);

    @Query("SELECT d " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.identityVerification = :identityVerification " +
            "AND d.usedForVerification = true")
    List<DocumentVerificationEntity> findAllUsedForVerification(IdentityVerificationEntity identityVerification);

    /**
     * Find all upload identifiers related to a verification.
     * @param verificationId Identification of the verification at the provider side.
     * @return List of remote uploadIds related to the specified verification id
     */
    @Query("SELECT d.uploadId " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.verificationId = :verificationId")
    List<String> findAllUploadIds(String verificationId);

    @Query("SELECT d " +
            "FROM DocumentVerificationEntity d " +
            "WHERE d.identityVerification = :identityVerification " +
            "AND d.photoId IS NOT NULL " +
            "AND d.usedForVerification = true")
    List<DocumentVerificationEntity> findAllWithPhoto(IdentityVerificationEntity identityVerification);

    /**
     * Return document verification IDs by the given identity verification ID and document statuses.
     *
     * @param identityVerificationIds identity verification IDs
     * @param statuses document statuses
     * @return identity verification IDs
     */
    @Query("SELECT d.id FROM DocumentVerificationEntity d " +
            "WHERE d.identityVerification.id IN :identityVerificationIds " +
            "AND d.status IN :statuses")
    List<String> findDocumentVerifications(Collection<String> identityVerificationIds, Collection<DocumentStatus> statuses);

    /**
     * Mark the given document verifications as failed.
     *
     * @param ids Document verification IDs
     * @param timestamp last updated and failed timestamp
     * @param errorDetail error detail
     * @param errorOrigin error origin
     */
    @Modifying
    @Query("UPDATE DocumentVerificationEntity d " +
            "SET d.status = com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus.FAILED, " +
            "    d.errorDetail = :errorDetail, " +
            "    d.errorOrigin = :errorOrigin, " +
            "    d.timestampLastUpdated = :timestamp " +
            "WHERE d.id IN :ids")
    void terminate(Collection<String> ids, Date timestamp, String errorDetail, ErrorOrigin errorOrigin);
}
