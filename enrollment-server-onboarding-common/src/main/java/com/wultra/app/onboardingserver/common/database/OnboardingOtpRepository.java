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

import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.enrollmentserver.model.enumeration.OtpType;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository for onboarding OTP codes.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface OnboardingOtpRepository extends CrudRepository<OnboardingOtpEntity, String> {

    @Query("SELECT o FROM OnboardingOtpEntity o WHERE o.process.id = :processId AND o.type = :type AND o.timestampCreated = " +
            "(SELECT MAX(o2.timestampCreated) FROM OnboardingOtpEntity o2 WHERE o2.process.id = :processId AND o2.type = :type)")
    Optional<OnboardingOtpEntity> findNewestByProcessIdAndType(String processId, OtpType type);

    /**
     * Return OTP IDs by the given timestamp.
     *
     * @param dateCreatedBefore timestamp created must be before the given value
     * @return OTP IDs
     */
    @Query("SELECT o.id FROM OnboardingOtpEntity o " +
            "WHERE o.status = com.wultra.app.enrollmentserver.model.enumeration.OtpStatus.ACTIVE " +
            "AND o.timestampCreated < :dateCreatedBefore")
    List<String> findExpiredIds(Date dateCreatedBefore);

    /**
     * Mark the given OTPs as failed.
     *
     * @param ids OTP IDs
     * @param timestampExpired last updated and failed timestamp
     */
    @Modifying
    @Query("UPDATE OnboardingOtpEntity o SET " +
            "o.status = com.wultra.app.enrollmentserver.model.enumeration.OtpStatus.FAILED, " +
            "o.timestampLastUpdated = :timestampExpired, " +
            "o.errorDetail = '" + OnboardingOtpEntity.ERROR_EXPIRED + "', " +
            "o.errorOrigin = 'OTP_VERIFICATION', " +
            "o.timestampFailed = :timestampExpired " +
            "WHERE o.id IN :ids")
    void terminate(Collection<String> ids, Date timestampExpired);

    /**
     * Count failed OTP attempts of the given process and OTP type not used for identity verification.
     *
     * @param process process
     * @param type OTP type
     * @return failed attempts count
     * @see #countFailedAttempts(OnboardingProcessEntity, OtpType, IdentityVerificationEntity)
     */
    @Query("SELECT SUM(o.failedAttempts) FROM OnboardingOtpEntity o " +
            "WHERE o.process = :process AND o.identityVerification is null AND o.type = :type")
    int countFailedAttempts(OnboardingProcessEntity process, OtpType type);

    /**
     * Count failed OTP attempts of the given process, OTP type and identity verification.
     *
     * @param process process
     * @param type OTP type
     * @return failed attempts count
     * @see #countFailedAttempts(OnboardingProcessEntity, OtpType)
     */
    @Query("SELECT SUM(o.failedAttempts) FROM OnboardingOtpEntity o " +
            "WHERE o.process = :process AND o.identityVerification = :identityVerification AND o.type = :type")
    int countFailedAttempts(OnboardingProcessEntity process, OtpType type, IdentityVerificationEntity identityVerification);

    @Query("SELECT MAX(o.timestampCreated) FROM OnboardingOtpEntity o WHERE o.process.id = :processId AND o.type = :type")
    Date getNewestOtpCreatedTimestamp(String processId, OtpType type);

    @Query("SELECT COUNT(o.id) FROM OnboardingOtpEntity o WHERE o.process.id = :processId AND o.type = :type")
    int countByProcessIdAndType(String processId, OtpType type);

}