/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.DocumentVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationTargetActivationService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationResultService;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.UnexpectedRollbackException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * Test for {@link StateMachineService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class StateMachineServiceTest {

    @Autowired
    private StateMachineService tested;

    @Autowired
    private IdentityVerificationRepository repository;

    @MockitoBean
    private IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    @MockitoBean
    private VerificationResultService verificationResultService;

    @MockitoBean
    private DocumentVerificationProvider documentVerificationProvider;

    @Sql
    @Test
    void testProcessStateMachineEvent_fail() throws Exception {
        when(documentVerificationProvider.verifyDocuments(any(), any()))
                .thenReturn(DocumentsVerificationResult.builder()
                        .verificationId("verification-id-1")
                        .status(DocumentVerificationStatus.ACCEPTED)
                        .results(List.of(DocumentVerificationResult.builder().uploadId(null).build()))
                        .build());

        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("a1");
        ownerId.setUserId("u1");

        assertThrows(UnexpectedRollbackException.class,
                () -> tested.processStateMachineEvent(ownerId, "p1", OnboardingEvent.DOCUMENT_UPLOADED));

        // Transaction is rolled back, so identity verification remains in its original status.
        assertEquals(IdentityVerificationStatus.IN_PROGRESS, repository.findById("v1").orElseThrow().getStatus());
    }

    @Sql
    @Test
    void testProcessStateMachineEvent_success() throws Exception {
        when(identityVerificationTargetActivationService.isTargetActivationFinished("p2")).thenReturn(true);
        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.OK);

        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("a2");
        ownerId.setUserId("u2");

        tested.processStateMachineEvent(ownerId, "p2", OnboardingEvent.EVENT_NEXT_STATE);

        final var identityVerification = repository.findById("v2").orElseThrow();
        assertEquals(IdentityVerificationPhase.COMPLETED, identityVerification.getPhase());
        assertEquals(IdentityVerificationStatus.ACCEPTED, identityVerification.getStatus());
    }
}