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
package com.wultra.app.onboardingserver.statemachine.service;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link StateMachineService}
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class StateMachineServiceTest {

    @Autowired
    private StateMachineService tested;

    @Autowired
    private IdentityVerificationRepository repository;

    @Test
    @Sql
    void testChangeMachineStatesInBatch() {
        tested.changeMachineStatesInBatch();

        assertEquals(IdentityVerificationStatus.VERIFICATION_PENDING, repository.findById("v1").get().getStatus());
    }

    @Test
    @Sql
    void testChangeMachineStatesInBatch_submitting() {
        tested.changeMachineStatesInBatch();

        assertEquals(IdentityVerificationStatus.IN_PROGRESS, repository.findById("v2").get().getStatus());
    }

    @Test
    @Sql
    void testChangeMachineStatesInBatch_noDocuments() {
        tested.changeMachineStatesInBatch();

        assertEquals(IdentityVerificationStatus.IN_PROGRESS, repository.findById("v3").get().getStatus());
    }

}
