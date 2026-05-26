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
package com.wultra.app.onboardingserver.statemachine.event;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationTargetActivationService;
import com.wultra.app.onboardingserver.impl.service.userdatastore.UserDataStoreService;
import com.wultra.app.onboardingserver.impl.service.verification.VerificationResultService;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for {@link OnboardingCompletedAcceptedListener}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class OnboardingCompletedAcceptedListenerIntTest {

    private static final String PROCESS_ID = "processId";
    private static final String USER_ID = "userId";
    private static final String ACTIVATION_ID = "activationId";

    private static final long VERIFY_TIMEOUT = 2_000L;

    @Autowired
    private StateMachineService stateMachineService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private OnboardingCompletedAcceptedListener onboardingCompletedAcceptedListener;

    @MockitoBean
    private IdentityVerificationService identityVerificationService;

    @MockitoBean
    private IdentityVerificationTargetActivationService identityVerificationTargetActivationService;

    @MockitoBean
    private VerificationResultService verificationResultService;

    @MockitoBean
    private UserDataStoreService userDataStoreService;

    @Test
    void testOnOnboardingCompletedAccepted_eventIsAccepted() throws Exception {
        // given
        final var ownerId = createOwnerId();
        final var identityVerification = createIdentityVerification();

        when(identityVerificationService.findBy(any())).thenReturn(identityVerification);
        doReturn(List.of()).when(identityVerificationService).findAllIdentityVerificationsToChangeState();
        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID)).thenReturn(true);
        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.OK);
        when(userDataStoreService.collectDocumentData(any())).thenReturn(List.of());

        final ArgumentCaptor<OnboardingCompletedAcceptedEvent> onboardingCompletedAcceptedEventCaptor =
                ArgumentCaptor.forClass(OnboardingCompletedAcceptedEvent.class);

        // when
        stateMachineService.processStateMachineEvent(ownerId, PROCESS_ID, OnboardingEvent.EVENT_NEXT_STATE);

        // then
        verify(onboardingCompletedAcceptedListener, timeout(VERIFY_TIMEOUT))
                .onOnboardingCompletedAccepted(onboardingCompletedAcceptedEventCaptor.capture());

        final var capturedEvent = onboardingCompletedAcceptedEventCaptor.getValue();
        assertEquals(PROCESS_ID, capturedEvent.getProcessId());
        assertEquals(ACTIVATION_ID, capturedEvent.getOwnerId().getActivationId());
        assertEquals(USER_ID, capturedEvent.getOwnerId().getUserId());
    }

    @Test
    void testOnOnboardingCompletedAccepted_transactionRollback_eventIsNotAccepted() throws Exception {
        // given
        final var ownerId = createOwnerId();
        final var identityVerification = createIdentityVerification();

        when(identityVerificationService.findBy(any())).thenReturn(identityVerification);
        doReturn(List.of()).when(identityVerificationService).findAllIdentityVerificationsToChangeState();
        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID)).thenReturn(true);
        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.OK);

        final var transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            processStateMachineEvent(ownerId);
            status.setRollbackOnly();
        });

        // then
        verify(onboardingCompletedAcceptedListener, after(VERIFY_TIMEOUT).never()).onOnboardingCompletedAccepted(any());
    }

    @Test
    void testOnOnboardingCompletedAccepted_userDataIsStored() throws Exception {
        // given
        final var ownerId = createOwnerId();
        final var identityVerification = createIdentityVerification();
        final var documentCreateRequests = createDocumentCreateRequests();

        when(identityVerificationService.findBy(any())).thenReturn(identityVerification);
        doReturn(List.of()).when(identityVerificationService).findAllIdentityVerificationsToChangeState();
        when(identityVerificationTargetActivationService.isTargetActivationFinished(PROCESS_ID)).thenReturn(true);
        when(verificationResultService.processVerificationResult(any(), any()))
                .thenReturn(IdentityVerificationService.FinalVerificationResult.OK);
        when(userDataStoreService.collectDocumentData(any())).thenReturn(documentCreateRequests);

        // when
        stateMachineService.processStateMachineEvent(ownerId, PROCESS_ID, OnboardingEvent.EVENT_NEXT_STATE);

        // then
        verify(userDataStoreService, timeout(VERIFY_TIMEOUT)).storeDocumentData(documentCreateRequests);
    }

    private void processStateMachineEvent(final OwnerId ownerId) {
        try {
            stateMachineService.processStateMachineEvent(ownerId, PROCESS_ID, OnboardingEvent.EVENT_NEXT_STATE);
        } catch (final Exception e) {
            throw new RuntimeException("Test exception", e);
        }
    }

    private static OwnerId createOwnerId() {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        ownerId.setUserId(USER_ID);
        return ownerId;
    }

    private static IdentityVerificationEntity createIdentityVerification() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setActivationId(ACTIVATION_ID);
        identityVerification.setUserId(USER_ID);
        identityVerification.setProcessId(PROCESS_ID);
        identityVerification.setPhase(IdentityVerificationPhase.ACTIVATION_FINISH);
        identityVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        return identityVerification;
    }

    private static List<DocumentCreateRequest> createDocumentCreateRequests() {
        return List.of(
                DocumentCreateRequest.builder().build(),
                DocumentCreateRequest.builder().build()
        );
    }
}
