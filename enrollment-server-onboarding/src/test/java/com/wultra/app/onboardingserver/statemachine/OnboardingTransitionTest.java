/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.impl.service.*;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test transitions with multiple states.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Slf4j
class OnboardingTransitionTest extends AbstractStateMachineTest {

    @MockitoBean
    private IdentityVerificationService identityVerificationService;

    @MockitoBean
    private IdentityVerificationConfig identityVerificationConfig;

    @MockitoBean
    private OnboardingProcessRepository onboardingProcessRepository;

    @MockitoBean
    private DocumentVerificationRepository documentVerificationRepository;

    @MockitoBean
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    @MockitoBean
    private IdentityVerificationOtpService identityVerificationOtpService;

    @MockitoBean
    private PresenceCheckService presenceCheckService;

    @MockitoBean
    private ClientEvaluationService clientEvaluationService;

    @MockitoBean
    private OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    @Test
    void testDocumentVerificationAcceptedChain() throws Exception {
        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(identityVerificationService.startDocumentVerification(any(), any()))
                .thenReturn(IdentityVerificationService.DocumentEvaluationStatus.OK);

        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        when(documentVerificationRepository.findAllUsedForVerification(any()))
                .thenReturn(List.of(documentVerification));

        final OnboardingProcessConfigurationValue configurationValue = OnboardingProcessConfigurationValue.builder()
                .clientEvaluationEnabled(false)
                .otpForIdentityVerification(true)
                .build();

        final OnboardingProcessConfigurationEntity configuration = new OnboardingProcessConfigurationEntity();
        configuration.setConfiguration(configurationValue);
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);
        process.setProcessConfiguration(configuration);

        when(onboardingProcessRepository.findById(PROCESS_ID))
                .thenReturn(Optional.of(process));
        when(onboardingProcessRepository.findByActivationIdAndStatusWithLock(any(), any()))
                .thenReturn(Optional.of(process));

        when(identityVerificationConfig.isPresenceCheckEnabled())
                .thenReturn(false);

        final List<OnboardingState> visitedStates = new ArrayList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.DOCUMENT_UPLOADED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertTrue(visitedStates.contains(OnboardingState.DOCUMENT_VERIFICATION_ACCEPTED), "Should contain DOCUMENT_VERIFICATION_ACCEPTED. Visited: " + visitedStates);
        assertTrue(visitedStates.contains(OnboardingState.DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS), "Should contain DOCUMENT_VERIFICATION_FINAL_IN_PROGRESS. Visited: " + visitedStates);
        assertTrue(visitedStates.contains(OnboardingState.OTP_VERIFICATION_PENDING), "Should contain OTP_VERIFICATION_PENDING. Visited: " + visitedStates);
    }

    @Test
    void testDocumentVerificationFailedChain() throws Exception {
        final IdentityVerificationEntity identityVerification = createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        final StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(identityVerification);
        stateMachine.startReactively().block();

        when(identityVerificationService.startDocumentVerification(any(), any()))
                .thenReturn(IdentityVerificationService.DocumentEvaluationStatus.NOK);

        final OnboardingProcessEntity processEntity = new OnboardingProcessEntity();
        processEntity.setId(PROCESS_ID);
        when(onboardingProcessRepository.findByActivationIdAndStatusWithLock(any(), any()))
                .thenReturn(Optional.of(processEntity));

        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        when(documentVerificationRepository.findAllUsedForVerification(any()))
                .thenReturn(List.of(documentVerification));

        final List<OnboardingState> visitedStates = new ArrayList<>();
        stateMachine.addStateListener(createListener(visitedStates));

        final Message<OnboardingEvent> message = stateMachineService.createMessage(OWNER_ID, PROCESS_ID, OnboardingEvent.DOCUMENT_UPLOADED);
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        logger.info("Visited states: {}", visitedStates);

        assertTrue(visitedStates.contains(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS), "Should return to DOCUMENT_UPLOAD_IN_PROGRESS. Visited: " + visitedStates);
    }

    private static StateMachineListenerAdapter<OnboardingState, OnboardingEvent> createListener(final List<OnboardingState> visitedStates) {
        return new StateMachineListenerAdapter<>() {
            @Override
            public void stateEntered(final State<OnboardingState, OnboardingEvent> state) {
                logger.info("Entered state: {}", state.getId());
                visitedStates.add(state.getId());
            }
        };
    }
}
