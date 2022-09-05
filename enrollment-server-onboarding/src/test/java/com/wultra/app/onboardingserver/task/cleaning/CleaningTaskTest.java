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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
class CleaningTaskTest {

    @Autowired
    private CleaningService tested;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Sql
    void testTerminateExpiredIdentityVerifications() {

        final String id1 = "a6055e8b-4ac0-45dd-b68e-29f4cd991a5c";
        final String id2 = "8d036a18-f51f-4a30-92cd-04876172ebca";
        final String id3 = "c918e1c4-5ca7-47da-8765-afc92082f717";

        assertPhaseAndStatus(id1, PRESENCE_CHECK, IN_PROGRESS);
        assertPhaseAndStatus(id2, PRESENCE_CHECK, IN_PROGRESS);
        assertPhaseAndStatus(id3, COMPLETED, ACCEPTED);

        tested.terminateExpiredIdentityVerifications();
        flushAndClear();

        assertPhaseAndStatus(id1, PRESENCE_CHECK, IN_PROGRESS);
        assertPhaseAndStatus(id2, COMPLETED, FAILED);
        assertPhaseAndStatus(id3, COMPLETED, ACCEPTED);

        final IdentityVerificationEntity identityVerification = fetchIdentityVerification(id2);
        assertEquals("expiredProcessOnboarding", identityVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, identityVerification.getErrorOrigin());
    }

    @Test
    @Sql
    void testTerminateExpiredDocumentVerifications() {
        final String id1 = "16055e8b-4ac0-45dd-b68e-29f4cd991a5c";
        final String id2 = "2d036a18-f51f-4a30-92cd-04876172ebca";
        final String id3 = "3918e1c4-5ca7-47da-8765-afc92082f717";

        assertStatus(id1, DocumentStatus.UPLOAD_IN_PROGRESS);
        assertStatus(id2, DocumentStatus.UPLOAD_IN_PROGRESS);
        assertStatus(id3, DocumentStatus.ACCEPTED);

        tested.terminateExpiredDocumentVerifications();
        flushAndClear();

        assertStatus(id1, DocumentStatus.UPLOAD_IN_PROGRESS);
        assertStatus(id2, DocumentStatus.FAILED);
        assertStatus(id3, DocumentStatus.ACCEPTED);

        final DocumentVerificationEntity documentVerification = fetchDocumentVerification(id2);
        assertEquals("expired", documentVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, documentVerification.getErrorOrigin());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertStatus(final String id, final DocumentStatus status) {
        final DocumentVerificationEntity documentVerification = fetchDocumentVerification(id);
        assertEquals(status, documentVerification.getStatus(), "status of " + id);
    }

    private DocumentVerificationEntity fetchDocumentVerification(final String id) {
        return entityManager.find(DocumentVerificationEntity.class, id);
    }

    private void assertPhaseAndStatus(final String id, final IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        final IdentityVerificationEntity identityVerification = fetchIdentityVerification(id);
        assertEquals(phase, identityVerification.getPhase(), "phase of id " + id);
        assertEquals(status, identityVerification.getStatus(), "status of id " + id);
    }

    private IdentityVerificationEntity fetchIdentityVerification(String id) {
        return entityManager.find(IdentityVerificationEntity.class, id);
    }
}
