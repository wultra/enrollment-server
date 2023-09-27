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

import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin.PROCESS_LIMIT_CHECK;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link CleaningService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Transactional
class CleaningServiceTest {

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

        tested.terminateExpiredIdentityVerifications();

        assertPhaseAndStatus(id1, IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS);
        assertPhaseAndStatus(id2, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED);
        assertPhaseAndStatus(id3, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.ACCEPTED);

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

        tested.terminateExpiredDocumentVerifications();

        assertStatus(id1, DocumentStatus.UPLOAD_IN_PROGRESS);
        assertStatus(id2, DocumentStatus.FAILED);
        assertStatus(id3, DocumentStatus.ACCEPTED);

        final DocumentVerificationEntity documentVerification = fetchDocumentVerification(id2);
        assertEquals("expired", documentVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, documentVerification.getErrorOrigin());
    }

    @Test
    @Sql
    void testCleanupLargeDocuments() {
        final String id1 = "93a41939-a808-4fe4-a673-f527a294f33e";
        final String id2 = "54bcf744-3e78-4a17-b84e-eea065d733a6";

        tested.cleanupLargeDocuments();

        assertNotNull(fetchDocumentData(id1));
        assertNull(fetchDocumentData(id2), "document data ID: " + id2 + " should be deleted");
    }

    @Test
    @Sql
    void testTerminateExpiredProcesses() {
        final String processId1 = "11111111-df91-4053-bb3d-3970979baf5d";
        final String processId2 = "22222222-df91-4053-bb3d-3970979baf5d";
        final String processId3 = "33333333-df91-4053-bb3d-3970979baf5d";

        tested.terminateExpiredProcesses();

        assertStatus(processId1, OnboardingStatus.FAILED);
        assertStatus(processId2, OnboardingStatus.ACTIVATION_IN_PROGRESS);
        assertStatus(processId3, OnboardingStatus.FINISHED);

        final OnboardingProcessEntity onboardingProcess = fetchOnboardingProcess(processId1);
        assertEquals("expiredProcessOnboarding", onboardingProcess.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, onboardingProcess.getErrorOrigin());
    }

    @Test
    @Sql
    void testTerminateExpiredOtpCodes() {
        final String id1 = "f50b8c04-649d-43a7-8079-4dbf9b0bbc72";
        final String id2 = "6560a85d-7d97-44c0-bd29-04c57051aa57";
        final String id3 = "e4974ef6-135a-4ae1-be91-a2c0f674c8fd";

        tested.terminateExpiredOtpCodes();

        assertStatus(id1, OtpStatus.ACTIVE);
        assertStatus(id2, OtpStatus.FAILED);
        assertStatus(id3, OtpStatus.VERIFIED);

        final OnboardingOtpEntity onboardingOtp = fetchOnboardingOtp(id2);
        assertEquals(OnboardingOtpEntity.ERROR_EXPIRED, onboardingOtp.getErrorDetail());
        assertEquals(ErrorOrigin.OTP_VERIFICATION, onboardingOtp.getErrorOrigin());
    }

    @Test
    @Sql
    void testTerminateExpiredProcessVerifications() {
        final String processId1 = "11111111-df91-4053-bb3d-3970979baf5d";
        final String processId2 = "22222222-df91-4053-bb3d-3970979baf5d";
        final String processId3 = "33333333-df91-4053-bb3d-3970979baf5d";
        final String processId4 = "44444444-df91-4053-bb3d-3970979baf5d";

        final String identityVerificationId1 = "11111111-4ac0-45dd-b68e-29f4cd991a5c";
        final String identityVerificationId2 = "22222222-4ac0-45dd-b68e-29f4cd991a5c";
        final String identityVerificationId3 = "33333333-4ac0-45dd-b68e-29f4cd991a5c";
        final String identityVerificationId4 = "44444444-4ac0-45dd-b68e-29f4cd991a5c";

        final String documentId1 = "11111111-f51f-4a30-92cd-04876172ebca";
        final String documentId2 = "22222222-f51f-4a30-92cd-04876172ebca";
        final String documentId3 = "33333333-f51f-4a30-92cd-04876172ebca";
        final String documentId4 = "44444444-f51f-4a30-92cd-04876172ebca";

        tested.terminateExpiredProcessVerifications();

        assertStatus(processId1, OnboardingStatus.FAILED);
        assertStatus(processId2, OnboardingStatus.FAILED);
        assertStatus(processId3, OnboardingStatus.FAILED);
        assertStatus(processId4, OnboardingStatus.VERIFICATION_IN_PROGRESS);

        final OnboardingProcessEntity onboardingProcess = fetchOnboardingProcess(processId1);
        assertEquals("expiredProcessIdentityVerification", onboardingProcess.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, onboardingProcess.getErrorOrigin());

        assertPhaseAndStatus(identityVerificationId1, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED);
        assertPhaseAndStatus(identityVerificationId2, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED);
        assertPhaseAndStatus(identityVerificationId3, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.ACCEPTED);
        assertPhaseAndStatus(identityVerificationId4, IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS);

        final IdentityVerificationEntity identityVerification = fetchIdentityVerification(identityVerificationId1);
        assertEquals("expiredProcessIdentityVerification", identityVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, identityVerification.getErrorOrigin());

        assertStatus(documentId1, DocumentStatus.FAILED);
        assertStatus(documentId2, DocumentStatus.ACCEPTED);
        assertStatus(documentId3, DocumentStatus.UPLOAD_IN_PROGRESS);
        assertStatus(documentId4, DocumentStatus.ACCEPTED);

        final DocumentVerificationEntity documentVerification = fetchDocumentVerification(documentId1);
        assertEquals("expiredProcessIdentityVerification", documentVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, documentVerification.getErrorOrigin());
    }

    @Test
    @Sql
    void testTerminateExpiredProcessActivations() {
        final String processId1 = "11111111-df91-4053-bb3d-3970979baf5d";
        final String processId2 = "22222222-df91-4053-bb3d-3970979baf5d";
        final String processId3 = "33333333-df91-4053-bb3d-3970979baf5d";
        final String processId4 = "44444444-df91-4053-bb3d-3970979baf5d";

        final String identityVerificationId1 = "11111111-4ac0-45dd-b68e-29f4cd991a5c";
        final String identityVerificationId2 = "22222222-4ac0-45dd-b68e-29f4cd991a5c";
        final String identityVerificationId3 = "33333333-4ac0-45dd-b68e-29f4cd991a5c";
        final String identityVerificationId4 = "44444444-4ac0-45dd-b68e-29f4cd991a5c";

        final String documentId1 = "11111111-f51f-4a30-92cd-04876172ebca";
        final String documentId2 = "22222222-f51f-4a30-92cd-04876172ebca";
        final String documentId3 = "33333333-f51f-4a30-92cd-04876172ebca";
        final String documentId4 = "44444444-f51f-4a30-92cd-04876172ebca";

        tested.terminateExpiredProcessActivations();

        assertStatus(processId1, OnboardingStatus.FAILED);
        assertStatus(processId2, OnboardingStatus.FAILED);
        assertStatus(processId3, OnboardingStatus.FAILED);
        assertStatus(processId4, OnboardingStatus.ACTIVATION_IN_PROGRESS);

        final OnboardingProcessEntity onboardingProcess = fetchOnboardingProcess(processId1);
        assertEquals("expiredProcessActivation", onboardingProcess.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, onboardingProcess.getErrorOrigin());

        assertPhaseAndStatus(identityVerificationId1, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED);
        assertPhaseAndStatus(identityVerificationId2, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.FAILED);
        assertPhaseAndStatus(identityVerificationId3, IdentityVerificationPhase.COMPLETED, IdentityVerificationStatus.ACCEPTED);
        assertPhaseAndStatus(identityVerificationId4, IdentityVerificationPhase.PRESENCE_CHECK, IdentityVerificationStatus.IN_PROGRESS);

        final IdentityVerificationEntity identityVerification = fetchIdentityVerification(identityVerificationId1);
        assertEquals("expiredProcessActivation", identityVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, identityVerification.getErrorOrigin());

        assertStatus(documentId1, DocumentStatus.FAILED);
        assertStatus(documentId2, DocumentStatus.ACCEPTED);
        assertStatus(documentId3, DocumentStatus.UPLOAD_IN_PROGRESS);
        assertStatus(documentId4, DocumentStatus.ACCEPTED);

        final DocumentVerificationEntity documentVerification = fetchDocumentVerification(documentId1);
        assertEquals("expiredProcessActivation", documentVerification.getErrorDetail());
        assertEquals(PROCESS_LIMIT_CHECK, documentVerification.getErrorOrigin());
    }

    private void assertStatus(final String id, final DocumentStatus status) {
        final DocumentVerificationEntity documentVerification = fetchDocumentVerification(id);
        assertEquals(status, documentVerification.getStatus(), "status of " + id);
    }

    private void assertStatus(final String id, final OnboardingStatus status) {
        final OnboardingProcessEntity onboardingProcess = fetchOnboardingProcess(id);
        assertEquals(status, onboardingProcess.getStatus(), "status of " + id);
    }

    private void assertStatus(final String id, final OtpStatus status) {
        final OnboardingOtpEntity documentVerification = fetchOnboardingOtp(id);
        assertEquals(status, documentVerification.getStatus(), "status of " + id);
    }

    private DocumentVerificationEntity fetchDocumentVerification(final String id) {
        return entityManager.find(DocumentVerificationEntity.class, id);
    }

    private OnboardingOtpEntity fetchOnboardingOtp(final String id) {
        return entityManager.find(OnboardingOtpEntity.class, id);
    }

    private OnboardingProcessEntity fetchOnboardingProcess(final String id) {
        return entityManager.find(OnboardingProcessEntity.class, id);
    }

    private void assertPhaseAndStatus(final String id, final IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        final IdentityVerificationEntity identityVerification = fetchIdentityVerification(id);
        assertEquals(phase, identityVerification.getPhase(), "phase of id " + id);
        assertEquals(status, identityVerification.getStatus(), "status of id " + id);
    }

    private IdentityVerificationEntity fetchIdentityVerification(final String id) {
        return entityManager.find(IdentityVerificationEntity.class, id);
    }

    private DocumentDataEntity fetchDocumentData(final String id) {
        return entityManager.find(DocumentDataEntity.class, id);
    }
}
