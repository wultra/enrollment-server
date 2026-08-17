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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.IdentityVerificationStatusRequest;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IdentityVerificationStatusService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationStatusServiceTest {

    private static final String PROCESS_ID = "0c4d8e71-2f6b-4a99-b3d5-7e1f2c8a6b55";
    private static final String ACTIVATION_ID = "activation-123";

    @InjectMocks
    private IdentityVerificationStatusService tested;

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private OnboardingServiceImpl onboardingService;

    @Mock
    private ActivationFlagService activationFlagService;

    @Test
    void testCheckIdentityVerificationStatus_identityVerificationExists_foundByCurrentProcessId() throws Exception {
        // given
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);

        final var idVerification = new IdentityVerificationEntity();
        idVerification.setActivationId(ACTIVATION_ID);
        idVerification.setProcessId(PROCESS_ID);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);

        final var onboardingProcess = createOnboardingProcess(ACTIVATION_ID);

        when(onboardingService.findProcessByActivationId(ACTIVATION_ID)).thenReturn(onboardingProcess);
        when(identityVerificationService.findByProcessIdOptional(PROCESS_ID)).thenReturn(Optional.of(idVerification));
        when(onboardingService.hasProcessExpired(onboardingProcess)).thenReturn(false);
        when(activationFlagService.containsActivationFlagVerificationPending(ACTIVATION_ID)).thenReturn(false);

        // when
        final var response = tested.checkIdentityVerificationStatus(new IdentityVerificationStatusRequest(), ownerId);

        // then
        assertEquals(PROCESS_ID, response.getProcessId());
        assertEquals(IdentityVerificationStatus.IN_PROGRESS, response.getIdentityVerificationStatus());
        assertEquals(IdentityVerificationPhase.DOCUMENT_UPLOAD, response.getIdentityVerificationPhase());
        verify(onboardingService).findProcessByActivationId(ACTIVATION_ID);
        verify(identityVerificationService).findByProcessIdOptional(PROCESS_ID);
    }

    @Test
    void testCheckIdentityVerificationStatus_activationFlagVerificationPending_notInitializedStatus() throws Exception {
        // given
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);

        final var idVerification = new IdentityVerificationEntity();
        idVerification.setActivationId(ACTIVATION_ID);
        idVerification.setProcessId(PROCESS_ID);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setPhase(IdentityVerificationPhase.DOCUMENT_UPLOAD);

        final var onboardingProcess = createOnboardingProcess(ACTIVATION_ID);

        when(onboardingService.findProcessByActivationId(ACTIVATION_ID)).thenReturn(onboardingProcess);
        when(identityVerificationService.findByProcessIdOptional(PROCESS_ID)).thenReturn(Optional.of(idVerification));
        when(onboardingService.hasProcessExpired(onboardingProcess)).thenReturn(false);
        when(activationFlagService.containsActivationFlagVerificationPending(ACTIVATION_ID)).thenReturn(true);

        // when
        final var response = tested.checkIdentityVerificationStatus(new IdentityVerificationStatusRequest(), ownerId);

        // then
        assertEquals(PROCESS_ID, response.getProcessId());
        assertEquals(IdentityVerificationStatus.NOT_INITIALIZED, response.getIdentityVerificationStatus());
        assertNull(response.getIdentityVerificationPhase());
    }

    @Test
    void testCheckIdentityVerificationStatus_identityVerificationNotInitialized_processFoundByActivationId() throws Exception {
        // given
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);

        final var onboardingProcess = createOnboardingProcess(ACTIVATION_ID);

        when(onboardingService.findProcessByActivationId(ACTIVATION_ID)).thenReturn(onboardingProcess);
        when(identityVerificationService.findByProcessIdOptional(PROCESS_ID)).thenReturn(Optional.empty());
        when(onboardingService.hasProcessExpired(onboardingProcess)).thenReturn(false);

        // when
        final var response = tested.checkIdentityVerificationStatus(new IdentityVerificationStatusRequest(), ownerId);

        // then
        assertEquals(PROCESS_ID, response.getProcessId());
        assertEquals(IdentityVerificationStatus.NOT_INITIALIZED, response.getIdentityVerificationStatus());
        assertNull(response.getIdentityVerificationPhase());
        verify(onboardingService).findProcessByActivationId(ACTIVATION_ID);
        verify(identityVerificationService).findByProcessIdOptional(PROCESS_ID);
    }

    @Test
    void testCheckIdentityVerificationStatus_identityVerificationNotInitializedAndProcessExpired_failedStatus() throws Exception {
        // given
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);

        final var onboardingProcess = createOnboardingProcess(ACTIVATION_ID);
        onboardingProcess.getProcessConfiguration().setProcessType("onboarding");
        onboardingProcess.getProcessConfiguration().setConfiguration(OnboardingProcessConfigurationValue.builder()
                .consentRequired(true)
                .build());
        onboardingProcess.setConsentAccepted(false);

        when(onboardingService.findProcessByActivationId(ACTIVATION_ID)).thenReturn(onboardingProcess);
        when(identityVerificationService.findByProcessIdOptional(PROCESS_ID)).thenReturn(Optional.empty());
        when(onboardingService.hasProcessExpired(onboardingProcess)).thenReturn(true);

        // when
        final var response = tested.checkIdentityVerificationStatus(new IdentityVerificationStatusRequest(), ownerId);

        // then
        assertEquals(PROCESS_ID, response.getProcessId());
        assertEquals("onboarding", response.getProcessType());
        assertTrue(response.isConsentRequired());
        assertEquals(IdentityVerificationStatus.FAILED, response.getIdentityVerificationStatus());
        assertEquals(IdentityVerificationPhase.COMPLETED, response.getIdentityVerificationPhase());
    }

    @Test
    void testCheckIdentityVerificationStatus_reKycAfterReactivation_identityVerificationNotInitializedForCurrentProcess() throws Exception {
        // given
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);

        final var onboardingProcess = createOnboardingProcess(ACTIVATION_ID);

        when(onboardingService.findProcessByActivationId(ACTIVATION_ID)).thenReturn(onboardingProcess);
        when(identityVerificationService.findByProcessIdOptional(PROCESS_ID)).thenReturn(Optional.empty());
        when(onboardingService.hasProcessExpired(onboardingProcess)).thenReturn(false);

        // when
        final var response = tested.checkIdentityVerificationStatus(new IdentityVerificationStatusRequest(), ownerId);

        // then
        assertEquals(PROCESS_ID, response.getProcessId());
        assertEquals(IdentityVerificationStatus.NOT_INITIALIZED, response.getIdentityVerificationStatus());
        assertNull(response.getIdentityVerificationPhase());
        verify(identityVerificationService, never()).findByOptional(ownerId);
    }

    @Test
    void testIsConsentPending_processConfigNotFound_exceptionIsThrown() {
        // given
        final var process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.isConsentPending(process));

        // then
        assertEquals("Configuration not found for process ID: " + PROCESS_ID, exception.getMessage());
    }

    @CsvSource({
            "true,,false",
            "true,true,false",
            "true,false,true",
            "false,,false",
            "false,true,false",
            "false,false,false"
    })
    @ParameterizedTest
    void testIsConsentPending(final boolean consentRequired, final Boolean consentAccepted, final boolean expectedResult) {
        // given
        final var processConfigValue = OnboardingProcessConfigurationValue.builder()
                .consentRequired(consentRequired)
                .build();

        final var processConfig = new OnboardingProcessConfigurationEntity();
        processConfig.setConfiguration(processConfigValue);

        final var process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);
        process.setProcessConfiguration(processConfig);

        Optional.ofNullable(consentAccepted)
                .ifPresent(process::setConsentAccepted);

        // when
        final var result = tested.isConsentPending(process);

        // then
        assertEquals(expectedResult, result);
    }

    private static OnboardingProcessEntity createOnboardingProcess(final String activationId) {
        final var processConfigValue = OnboardingProcessConfigurationValue.builder()
                .consentRequired(false)
                .build();
        final var processConfig = new OnboardingProcessConfigurationEntity();
        processConfig.setConfiguration(processConfigValue);

        final var process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);
        process.setActivationId(activationId);
        process.setProcessConfiguration(processConfig);
        return process;
    }
}
