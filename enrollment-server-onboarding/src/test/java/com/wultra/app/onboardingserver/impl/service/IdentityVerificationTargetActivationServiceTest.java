/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.CreateTargetActivationRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.CreateTargetActivationResponse;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import com.wultra.security.powerauth.client.model.response.InitActivationResponse;
import com.wultra.security.powerauth.client.model.response.v3.GetActivationStatusResponse;
import com.wultra.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import com.wultra.security.powerauth.rest.api.spring.authentication.impl.PowerAuthActivationImpl;
import com.wultra.security.powerauth.rest.api.spring.authentication.impl.PowerAuthApiAuthenticationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link IdentityVerificationTargetActivationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationTargetActivationServiceTest {

    @Mock
    private OnboardingServiceImpl onboardingService;

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private LookupUserService lookupUserService;

    @Mock
    private ActivationService activationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private IdentityVerificationTargetActivationService tested;

    @Test
    void testCreateTargetActivation_notExisting() throws Exception {
        mockIdentityVerification();
        when(lookupUserService.lookupUser(any(), any()))
                .thenReturn(Optional.of("user1"));

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        when(onboardingService.verifyProcessIdAndLock(any(), any(), any()))
                .thenReturn(process);

        final InitActivationResponse initActivationResponse = new InitActivationResponse();
        initActivationResponse.setActivationId("activation-1");
        initActivationResponse.setActivationCode("KA4PD-RTIE2-KOP3U-H53EA");
        when(activationService.initTargetActivation(any()))
                .thenReturn(initActivationResponse);

        final CreateTargetActivationResponse result = tested.createTargetActivation(createRequest(), createPowerAuthApiAuthentication());

        assertEquals("KA4PD-RTIE2-KOP3U-H53EA", result.activationCode());

        verify(auditService).auditActivation(process, "Create target activation for user: {}", "user1");

        final var processCaptor = ArgumentCaptor.forClass(OnboardingProcessEntity.class);
        verify(onboardingService, times(2)).updateProcess(processCaptor.capture());

        final var capturedProcesses = processCaptor.getAllValues();
        assertEquals("activation-1", capturedProcesses.get(1).getTargetActivationId());
    }

    @Test
    void testCreateTargetActivation_created() throws Exception {
        mockIdentityVerification();
        when(lookupUserService.lookupUser(any(), any()))
                .thenReturn(Optional.of("user1"));

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setTargetActivationId("activation-1");
        when(onboardingService.verifyProcessIdAndLock(any(), any(), any()))
                .thenReturn(process);

        mockActivationStatus(ActivationStatus.CREATED, "KA4PD-RTIE2-KOP3U-H53EA");

        final CreateTargetActivationResponse result = tested.createTargetActivation(createRequest(), createPowerAuthApiAuthentication());

        assertEquals("KA4PD-RTIE2-KOP3U-H53EA", result.activationCode());

        verify(auditService, never()).auditActivation(any(), any(), any());

        verify(onboardingService).updateProcess(process);
    }

    @Test
    void testCreateTargetActivation_removed() throws Exception {
        mockIdentityVerification();
        when(lookupUserService.lookupUser(any(), any()))
                .thenReturn(Optional.of("user1"));

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setTargetActivationId("activation-1");
        when(onboardingService.verifyProcessIdAndLock(any(), any(), any()))
                .thenReturn(process);

        mockActivationStatus(ActivationStatus.REMOVED);

        final InitActivationResponse initActivationResponse = new InitActivationResponse();
        initActivationResponse.setActivationId("activation-1");
        initActivationResponse.setActivationCode("KA4PD-RTIE2-KOP3U-H53EA");
        when(activationService.initTargetActivation(any()))
                .thenReturn(initActivationResponse);

        final CreateTargetActivationResponse result = tested.createTargetActivation(createRequest(), createPowerAuthApiAuthentication());

        assertEquals("KA4PD-RTIE2-KOP3U-H53EA", result.activationCode());

        verify(auditService).auditActivation(process, "Create target activation for user: {}", "user1");

        final var processCaptor = ArgumentCaptor.forClass(OnboardingProcessEntity.class);
        verify(onboardingService, times(2)).updateProcess(processCaptor.capture());

        final var capturedProcesses = processCaptor.getAllValues();
        assertEquals("activation-1", capturedProcesses.get(1).getTargetActivationId());
    }

    @Test
    void testCreateTargetActivation_pendingCommit() throws Exception {
        mockIdentityVerification();
        when(lookupUserService.lookupUser(any(), any()))
                .thenReturn(Optional.of("user1"));

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setTargetActivationId("activation-1");
        when(onboardingService.verifyProcessIdAndLock(any(), any(), any()))
                .thenReturn(process);

        mockActivationStatus(ActivationStatus.PENDING_COMMIT);

        final InitActivationResponse initActivationResponse = new InitActivationResponse();
        initActivationResponse.setActivationId("activation-1");
        initActivationResponse.setActivationCode("KA4PD-RTIE2-KOP3U-H53EA");
        when(activationService.initTargetActivation(any()))
                .thenReturn(initActivationResponse);

        final CreateTargetActivationResponse result = tested.createTargetActivation(createRequest(), createPowerAuthApiAuthentication());

        assertEquals("KA4PD-RTIE2-KOP3U-H53EA", result.activationCode());

        verify(auditService).auditActivation(process, "Remove activation for user: {}", "user1");
        verify(auditService).auditActivation(process, "Create target activation for user: {}", "user1");

        final var processCaptor = ArgumentCaptor.forClass(OnboardingProcessEntity.class);
        verify(onboardingService, times(2)).updateProcess(processCaptor.capture());

        verify(activationService).removeActivation("activation-1");

        final var capturedProcesses = processCaptor.getAllValues();
        assertEquals("activation-1", capturedProcesses.get(1).getTargetActivationId());
    }

    @Test
    void testCreateTargetActivation_blocked() throws Exception {
        mockIdentityVerification();
        when(lookupUserService.lookupUser(any(), any()))
                .thenReturn(Optional.of("user1"));

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setTargetActivationId("activation-1");
        when(onboardingService.verifyProcessIdAndLock(any(), any(), any()))
                .thenReturn(process);

        mockActivationStatus(ActivationStatus.BLOCKED);

        var result = assertThrows(OnboardingProcessException.class, () ->
                tested.createTargetActivation(createRequest(), createPowerAuthApiAuthentication()));

        assertEquals("Unexpected activation status: BLOCKED", result.getMessage());

        verify(auditService, never()).auditActivation(any(), any(), any());

        verify(onboardingService, times(1)).updateProcess(any(OnboardingProcessEntity.class));
    }

    private static CreateTargetActivationRequest createRequest() {
        return CreateTargetActivationRequest.builder().build();
    }

    private void mockIdentityVerification() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setPhase(IdentityVerificationPhase.ACTIVATION_FINISH);

        when(identityVerificationService.findByOptional(any())).
                thenReturn(Optional.of(identityVerification));
    }

    private void mockActivationStatus(final ActivationStatus status) throws RemoteCommunicationException {
        mockActivationStatus(status, null);
    }

    private void mockActivationStatus(final ActivationStatus status, final String activationCode) throws RemoteCommunicationException {
        final GetActivationStatusResponse activationStatusResponse = new GetActivationStatusResponse();
        activationStatusResponse.setActivationStatus(status);
        activationStatusResponse.setActivationCode(activationCode);

        when(activationService.fetchActivationStatusResponse("activation-1"))
                .thenReturn(activationStatusResponse);
    }

    private static PowerAuthApiAuthentication createPowerAuthApiAuthentication() {
        final PowerAuthApiAuthentication apiAuthentication = new PowerAuthApiAuthenticationImpl();
        apiAuthentication.setActivationContext(new PowerAuthActivationImpl());
        return apiAuthentication;
    }
}
