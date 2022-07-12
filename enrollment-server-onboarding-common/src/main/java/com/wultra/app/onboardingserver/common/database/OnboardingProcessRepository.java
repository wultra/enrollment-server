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

import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

/**
 * Repository for onboarding processes.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface OnboardingProcessRepository extends CrudRepository<OnboardingProcessEntity, String> {

    @Query("SELECT p FROM OnboardingProcessEntity p WHERE p.status = :status " +
            "AND p.userId = :userId " +
            "ORDER BY p.timestampCreated DESC")
    Optional<OnboardingProcessEntity> findExistingProcessForUser(String userId, OnboardingStatus status);

    @Query("SELECT p FROM OnboardingProcessEntity p WHERE p.status = :status " +
            "AND p.activationId = :activationId " +
            "ORDER BY p.timestampCreated DESC")
    Optional<OnboardingProcessEntity> findExistingProcessForActivation(String activationId, OnboardingStatus status);

    @Query("SELECT p FROM OnboardingProcessEntity p " +
            "WHERE p.activationId = :activationId " +
            "ORDER BY p.timestampCreated DESC")
    Optional<OnboardingProcessEntity> findProcessByActivationId(String activationId);

    @Query("SELECT count(p) FROM OnboardingProcessEntity p WHERE p.userId = :userId AND p.timestampCreated > :dateAfter")
    int countProcessesAfterTimestamp(String userId, Date dateAfter);

    @Modifying
    @Query("UPDATE OnboardingProcessEntity p SET " +
            "p.status = com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FAILED, " +
            "p.timestampLastUpdated = CURRENT_TIMESTAMP, " +
            "p.errorDetail = '" + OnboardingProcessEntity.ERROR_PROCESS_EXPIRED + "'" +
            "WHERE p.status = :status " +
            "AND p.timestampCreated < :dateCreatedBefore")
    void terminateExpiredProcessesByStatus(Date dateCreatedBefore, OnboardingStatus status);

    @Modifying
    @Query("UPDATE OnboardingProcessEntity p SET " +
            "p.status = com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FAILED, " +
            "p.timestampLastUpdated = CURRENT_TIMESTAMP, " +
            "p.errorDetail = '" + OnboardingProcessEntity.ERROR_PROCESS_EXPIRED + "'" +
            "WHERE p.timestampCreated < :dateCreatedBefore")
    void terminateExpiredProcesses(Date dateCreatedBefore);

}