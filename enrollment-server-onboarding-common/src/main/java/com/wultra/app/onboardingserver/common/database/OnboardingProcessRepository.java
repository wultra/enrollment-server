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
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
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
 * Repository for onboarding processes.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Repository
public interface OnboardingProcessRepository extends CrudRepository<OnboardingProcessEntity, String> {

    @Query("SELECT p FROM OnboardingProcessEntity p WHERE p.status = :status AND p.identificationData = :identificationData")
    Optional<OnboardingProcessEntity> findExistingProcessByIdentificationData(String identificationData, OnboardingStatus status);

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

    /**
     * Return onboarding process IDs by the given timestamp and status.
     *
     * @param dateCreatedBefore timestamp created must be before the given value
     * @param status onboarding status
     * @return onboarding process IDs
     */
    @Query("SELECT p.id FROM OnboardingProcessEntity p " +
            "WHERE p.status = :status " +
            "AND p.timestampCreated < :dateCreatedBefore")
    List<String> findExpiredProcessIdsByStatusAndCreatedDate(Date dateCreatedBefore, OnboardingStatus status);

    /**
     * Mark the given onboarding processes as failed.
     *
     * @param ids Onboarding process IDs
     * @param timestampExpired last updated and failed timestamp
     * @param errorDetail error detail
     * @param errorOrigin error origin
     */
    @Modifying
    @Query("UPDATE OnboardingProcessEntity p SET " +
            "p.status = com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FAILED, " +
            "p.timestampLastUpdated = :timestampExpired, " +
            "p.timestampFailed = :timestampExpired, " +
            "p.errorDetail = :errorDetail, " +
            "p.errorOrigin = :errorOrigin " +
            "WHERE p.id IN :ids")
    void terminate(Collection<String> ids, Date timestampExpired, String errorDetail, ErrorOrigin errorOrigin);

    /**
     * Return onboarding process IDs by the given timestamp. Include only not yet finished entities.
     *
     * @param dateCreatedBefore timestamp created must be before the given value
     * @return onboarding process IDs
     */
    @Query("SELECT p.id FROM OnboardingProcessEntity p " +
            "WHERE p.status <> com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FINISHED " +
            "AND p.status <> com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FAILED " +
            "AND p.timestampCreated < :dateCreatedBefore")
    List<String> findExpiredProcessIdsByCreatedDate(Date dateCreatedBefore);

    /**
     * Return onboarding processes to remove activation.
     *
     * @return onboarding processes
     */
    @Query("SELECT p FROM OnboardingProcessEntity p " +
            "WHERE p.status = com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus.FAILED " +
            "AND p.activationId IS NOT NULL " +
            "AND p.activationRemoved = false")
    Stream<OnboardingProcessEntity> findProcessesToRemoveActivation();
}
