/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
import com.wultra.app.onboardingserver.common.database.entity.ScaResultEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link ScaResultEntity}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Repository
public interface ScaResultRepository extends CrudRepository<ScaResultEntity, Long> {

    /**
     * Count SCA attempts for the given identity verification.
     *
     * @param identityVerification Identity verification to identify SCA attempts.
     * @return count of SCA attempts
     */
    int countByIdentityVerification(IdentityVerificationEntity identityVerification);

    /**
     * Find the latest SCA attempt.
     *
     * @param identityVerification Identity verification to identify SCA attempts.
     * @return the latest SCA attempt or empty
     */
    Optional<ScaResultEntity> findTopByIdentityVerificationOrderByTimestampCreatedDesc(IdentityVerificationEntity identityVerification);
}
