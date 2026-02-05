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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.AcknowledgeApproveClientRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.AcknowledgeApproveClientResponse;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Test for {@link AcknowledgeService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class AcknowledgeServiceTest {

    private static final String PROCESS_ID = "process123";
    private static final String USER_ID = "user123";
    private static final String IDENTITY_VERIFICATION_ID = "verification123";

    @Mock
    private OnboardingProcessRepository onboardingProcessRepository;

    @Mock
    private IdentityVerificationRepository identityVerificationRepository;

    @Mock
    private StateMachineService stateMachineService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AcknowledgeService tested;

    @Test
    void testAcknowledgeApproveClient_success() throws Exception{
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.of(identityVerification));

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.OK, response.result());
        verify(stateMachineService).processStateMachineEvent(any(), eq(PROCESS_ID), eq(OnboardingEvent.ONBOARDING_APPROVAL_ACKNOWLEDGED_APPROVE));
        verify(auditService).audit(eq(identityVerification), anyString(), eq(AcknowledgeApproveClientRequest.ApprovalResult.OK));
    }

    @Test
    void testAcknowledgeApproveClient_processNotFoundOrInvalidState() {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.empty());

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement failed. Process not found or in invalid state.", response.resultReason());
        verifyNoInteractions(identityVerificationRepository);
        verifyNoInteractions(stateMachineService);
    }

    @Test
    void testAcknowledgeApproveClient_validationFails_notFound() {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.empty());

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement validation failed. Identity verification not found.", response.resultReason());
        verifyNoInteractions(stateMachineService);
    }

    @Test
    void testAcknowledgeApproveClient_validationFails_processIdMismatch() {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        identityVerification.setProcessId("wrong-process-id");

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.of(identityVerification));

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement validation failed. Identity verification does not belong to the process.", response.resultReason());
        verifyNoInteractions(stateMachineService);
    }

    @Test
    void testAcknowledgeApproveClient_validationFails_phaseMismatch() {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        identityVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.of(identityVerification));

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement validation failed. Identity verification is not in ONBOARDING_APPROVAL phase.", response.resultReason());
        verifyNoInteractions(stateMachineService);
    }

    @Test
    void testAcknowledgeApproveClient_validationFails_statusMismatch() {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        identityVerification.setStatus(IdentityVerificationStatus.FAILED);

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.of(identityVerification));

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement validation failed. Identity verification is not in IN_PROGRESS state.", response.resultReason());
        verifyNoInteractions(stateMachineService);
    }

    @Test
    void testAcknowledgeApproveClient_validationFails_userMismatch() {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        identityVerification.setUserId("wrong-user-id");

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.of(identityVerification));

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement validation failed. Identity verification does not belong to the user.", response.resultReason());
        verifyNoInteractions(stateMachineService);
    }

    @Test
    void testAcknowledgeApproveClient_stateMachineEventFails() throws Exception {
        final AcknowledgeApproveClientRequest request = createAcknowledgeApproveClientRequest();
        final OnboardingProcessEntity process = createOnboardingProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        when(onboardingProcessRepository.findByIdWithLock(PROCESS_ID)).thenReturn(Optional.of(process));
        when(identityVerificationRepository.findById(IDENTITY_VERIFICATION_ID)).thenReturn(Optional.of(identityVerification));
        doThrow(new IdentityVerificationException("Verification failed")).when(stateMachineService).processStateMachineEvent(any(), eq(PROCESS_ID), any());

        final AcknowledgeApproveClientResponse response = tested.acknowledgeApproveClient(request);

        assertEquals(AcknowledgeApproveClientResponse.Result.NOK, response.result());
        assertEquals("Acknowledgement failed. Verification not found or in invalid state.", response.resultReason());
    }

    private static OnboardingProcessEntity createOnboardingProcess() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);
        process.setStatus(OnboardingStatus.VERIFICATION_IN_PROGRESS);
        return process;
    }

    private static IdentityVerificationEntity createIdentityVerification() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId(IDENTITY_VERIFICATION_ID);
        identityVerification.setProcessId(PROCESS_ID);
        identityVerification.setPhase(IdentityVerificationPhase.ONBOARDING_APPROVAL);
        identityVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        identityVerification.setUserId(USER_ID);
        return identityVerification;
    }

    private static AcknowledgeApproveClientRequest createAcknowledgeApproveClientRequest() {
        return new AcknowledgeApproveClientRequest(
                PROCESS_ID, USER_ID, IDENTITY_VERIFICATION_ID, AcknowledgeApproveClientRequest.ApprovalResult.OK
        );
    }
}
