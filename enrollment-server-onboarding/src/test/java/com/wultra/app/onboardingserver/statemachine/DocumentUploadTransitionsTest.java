/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DocumentUploadTransitionsTest extends AbstractTransitionTest {

    @MockBean
    private IdentityVerificationService identityVerificationService;

    @Test
    public void testDocumentUploadInProgress() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId(idVerification);

        doNothing().when(identityVerificationService).checkIdentityDocumentsForVerification(eq(ownerId), eq(idVerification));

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.DOCUMENT_UPLOAD_IN_PROGRESS)
                        .and()
                        .build();
        expected.test();
    }

    @Test
    public void testDocumentVerificationPending() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine = createStateMachine(idVerification);

        OwnerId ownerId = createOwnerId(idVerification);

        doAnswer(args -> {
            idVerification.setStatus(IdentityVerificationStatus.VERIFICATION_PENDING);
            return null;
        }).when(identityVerificationService).checkIdentityDocumentsForVerification(eq(ownerId), eq(idVerification));

        Message<EnrollmentEvent> message =
                stateMachineService.createMessage(ownerId, idVerification.getProcessId(), EnrollmentEvent.EVENT_NEXT_STATE);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .sendEvent(message)
                        .expectState(EnrollmentState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                        .and()
                        .build();
        expected.test();
    }

}
