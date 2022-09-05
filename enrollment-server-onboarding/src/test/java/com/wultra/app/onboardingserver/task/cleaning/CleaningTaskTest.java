/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.task.cleaning;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import javax.persistence.EntityManager;

import static com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin.PROCESS_LIMIT_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.COMPLETED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link CleaningTask}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("mock")
@Sql
class CleaningTaskTest {

    @Autowired
    private CleaningService tested;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testTerminateExpiredIdentityVerifications() {

        final String id1 = "a6055e8b-4ac0-45dd-b68e-29f4cd991a5c";
        final String id2 = "8d036a18-f51f-4a30-92cd-04876172ebca";
        final String id3 = "c918e1c4-5ca7-47da-8765-afc92082f717";

        assertPhaseAndStatus(id1, PRESENCE_CHECK, IN_PROGRESS);
        assertPhaseAndStatus(id2, PRESENCE_CHECK, IN_PROGRESS);
        assertPhaseAndStatus(id3, COMPLETED, ACCEPTED);

        tested.terminateExpiredIdentityVerifications();

        assertPhaseAndStatus(id1, PRESENCE_CHECK, IN_PROGRESS);
        assertPhaseAndStatus(id2, COMPLETED, FAILED);
        assertPhaseAndStatus(id3, COMPLETED, ACCEPTED);

        final IdentityVerificationEntity identityVerification = getIdentityVerification(id2);
        assertEquals("expiredProcessOnboarding", identityVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, identityVerification.getErrorOrigin());
    }

    void assertPhaseAndStatus(final String id, final IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        final IdentityVerificationEntity identityVerification = getIdentityVerification(id);
        assertEquals(phase, identityVerification.getPhase(), "phase of id " + id);
        assertEquals(status, identityVerification.getStatus(), "status of id " + id);
    }

    private IdentityVerificationEntity getIdentityVerification(String id) {
        return entityManager.find(IdentityVerificationEntity.class, id);
    }
}
