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
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.action.verification.VerificationProcessResultAction;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DocumentVerificationTransitionsTest extends AbstractStateMachineTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private IdentityVerificationService identityVerificationService;

    @MockBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockBean
    private VerificationProcessResultAction verificationProcessResultAction;

    @Test
    void testDocumentVerificationAccepted() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.ACCEPTED, OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED);
    }

    @Test
    void testDocumentVerificationInProgress() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.IN_PROGRESS, OnboardingState.DOCUMENT_VERIFICATION_IN_PROGRESS);
    }

    @Test
    void testDocumentVerificationRejected() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.REJECTED, OnboardingState.DOCUMENT_VERIFICATION_REJECTED);
    }

    @Test
    void testDocumentVerificationFailed() throws Exception {
        testDocumentVerificationStatus(IdentityVerificationStatus.FAILED, OnboardingState.DOCUMENT_VERIFICATION_FAILED);
    }

    @Test
    void testDocumentVerificationTransitionToClientEvaluation() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.ACCEPTED);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.CLIENT_EVALUATION_IN_PROGRESS)
                .and()
                .build()
                .test();

        assertEquals(IdentityVerificationPhase.CLIENT_EVALUATION, idVerification.getPhase());
        assertEquals(IdentityVerificationStatus.IN_PROGRESS, idVerification.getStatus());
    }

    private void testDocumentVerificationStatus(IdentityVerificationStatus identityStatus, OnboardingState expectedState) throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.VERIFICATION_PENDING);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        doAnswer(args -> {
            idVerification.setStatus(identityStatus);
            return null;
        }).when(identityVerificationService).startVerification(eq(OWNER_ID), eq(idVerification));

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(expectedState)
                .and()
                .build()
                .test();
    }

}
