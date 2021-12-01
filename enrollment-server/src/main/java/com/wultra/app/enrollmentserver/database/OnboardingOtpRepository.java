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

import com.wultra.app.enrollmentserver.database.entity.OnboardingOtpEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

/**
 * Repository for onboarding OTP codes.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface OnboardingOtpRepository extends CrudRepository<OnboardingOtpEntity, String> {

    Optional<OnboardingOtpEntity> findFirstByProcessIdOrderByTimestampCreatedDesc(String processId);

    @Modifying
    @Query("UPDATE OnboardingOtpEntity o SET o.status = com.wultra.app.enrollmentserver.model.enumeration.OtpStatus.FAILED, " +
            "o.timestampLastUpdated = CURRENT_TIMESTAMP, " +
            "o.errorDetail = 'expired' " +
            "WHERE o.status = com.wultra.app.enrollmentserver.model.enumeration.OtpStatus.ACTIVE " +
            "AND o.timestampCreated < :dateCreatedBefore")
    void terminateOldOtps(Date dateCreatedBefore);

    @Query("SELECT SUM(o.failedAttempts) FROM OnboardingOtpEntity o WHERE o.process.id = :processId")
    int getFailedAttemptsByProcess(String processId);

    @Query("SELECT MAX(o.timestampCreated) FROM OnboardingOtpEntity o WHERE o.process.id = :processId")
    Date getNewestOtpCreatedTimestamp(String processId);

}