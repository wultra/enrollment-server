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
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationOtpService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.action.verification.DocumentsVerificationPendingGuard;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@MockBean(IdentityVerificationOtpService.class)
@MockBean(VerificationProcessResultAction.class)
@Transactional
class DocumentVerificationTransitionsTest extends AbstractStateMachineTest {

    @MockBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockBean
    private IdentityVerificationService identityVerificationService;

    @MockBean
    private DocumentsVerificationPendingGuard documentsVerificationPendingGuard;

    @MockBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @Test
    void testDocumentVerificationAccepted() throws Exception {
        createIdentityVerificationLocal(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        testDocumentVerificationStatus(IdentityVerificationStatus.ACCEPTED, OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED);
    }

    @Test
    void testDocumentVerificationInProgress() throws Exception {
        createIdentityVerificationLocal(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        testDocumentVerificationStatus(IN_PROGRESS, OnboardingState.DOCUMENT_VERIFICATION_IN_PROGRESS);
    }

    @Test
    void testDocumentVerificationRejected() throws Exception {
        createIdentityVerificationLocal(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        testDocumentVerificationStatus(IdentityVerificationStatus.REJECTED, OnboardingState.DOCUMENT_VERIFICATION_REJECTED);
    }

    @Test
    void testDocumentVerificationFailed() throws Exception {
        createIdentityVerificationLocal(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.VERIFICATION_PENDING);
        testDocumentVerificationStatus(IdentityVerificationStatus.FAILED, OnboardingState.DOCUMENT_VERIFICATION_FAILED);
    }

    @Test
    void testDocumentVerificationTransitionToClientEvaluation() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerificationLocal(IdentityVerificationPhase.DOCUMENT_VERIFICATION, IdentityVerificationStatus.ACCEPTED);
        when(onboardingProcessRepository.findByActivationIdAndStatusWithLock(idVerification.getActivationId(), OnboardingStatus.VERIFICATION_IN_PROGRESS))
                .thenReturn(Optional.of(createOnboardingProcessEntity()));
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(identityVerificationConfig.isPresenceCheckEnabled()).thenReturn(true);
        when(identityVerificationService.moveToPhaseAndStatus(idVerification, CLIENT_EVALUATION, IN_PROGRESS, OWNER_ID))
                .thenReturn(idVerification);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS)
                .and()
                .build()
                .test();
    }

    private void testDocumentVerificationStatus(IdentityVerificationStatus identityStatus, OnboardingState expectedState) throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerificationLocal(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.VERIFICATION_PENDING);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        doAnswer(args -> {
            idVerification.setStatus(identityStatus);
            return null;
        }).when(identityVerificationService).startVerification(OWNER_ID, idVerification);
        when(documentsVerificationPendingGuard.evaluate(any()))
                .thenReturn(true);

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(expectedState)
                .and()
                .build()
                .test();
    }

    private IdentityVerificationEntity createIdentityVerificationLocal(IdentityVerificationPhase phase, IdentityVerificationStatus status) {
        final IdentityVerificationEntity idVerification = super.createIdentityVerification(phase, status);

        when(onboardingProcessRepository.findByActivationIdAndStatusWithLock(idVerification.getActivationId(), OnboardingStatus.VERIFICATION_IN_PROGRESS))
                .thenReturn(Optional.of(createOnboardingProcessEntity()));
        return idVerification;
    }

}
