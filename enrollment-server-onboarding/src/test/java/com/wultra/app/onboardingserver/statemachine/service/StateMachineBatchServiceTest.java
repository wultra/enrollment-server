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
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;

/**
 * Test for {@link StateMachineService}
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class StateMachineBatchServiceTest {

    @Autowired
    private StateMachineBatchService tested;

    @MockitoSpyBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @Autowired
    private DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    private IdentityVerificationRepository identityVerificationRepository;

    @AfterEach
    void cleanUp() {
        documentVerificationRepository.deleteAll();
        identityVerificationRepository.deleteAll();
        onboardingProcessRepository.deleteAll();
    }

    @Test
    @Sql
    void testChangeMachineStatesInBatch() {
        tested.changeMachineStatesInBatch();

        assertEquals(IdentityVerificationStatus.VERIFICATION_PENDING, identityVerificationRepository.findById("v1").get().getStatus());
    }

    @Test
    @Sql
    void testChangeMachineStatesInBatch_submitting() {
        tested.changeMachineStatesInBatch();

        assertEquals(IdentityVerificationStatus.IN_PROGRESS, identityVerificationRepository.findById("v2").get().getStatus());
    }

    @Test
    @Sql
    void testChangeMachineStatesInBatch_noDocuments() {
        tested.changeMachineStatesInBatch();

        assertEquals(IdentityVerificationStatus.IN_PROGRESS, identityVerificationRepository.findById("v3").get().getStatus());
    }

    /**
     * Tests that {@link RuntimeException} is handled. Propagated exception would break the for loop.
     */
    @Test
    @Sql
    void testChangeMachineStatesInBatch_exception() {
        doThrow(new RuntimeException("Test exception"))
                .when(onboardingProcessRepository)
                .findByIdWithLock("p_fail");

        tested.changeMachineStatesInBatch();

        final var actualIdentityStatus = identityVerificationRepository.findById("v_fail")
                .orElseThrow()
                .getStatus();

        assertEquals(IdentityVerificationStatus.IN_PROGRESS, actualIdentityStatus);
    }
}
