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
package com.wultra.app.onboardingserver.common.service;

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.configuration.CommonOnboardingConfig;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IdentityVerificationLimitService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationLimitServiceTest {

    private static final String ACTIVATION_ID = "activation-id";
    private static final String CURRENT_PROCESS_ID = "current-process";
    private static final String PREVIOUS_PROCESS_ID = "previous-process";
    private static final int ATTEMPT_LIMIT = 5;

    @Mock
    private IdentityVerificationRepository identityVerificationRepository;
    @Mock
    private OnboardingProcessRepository onboardingProcessRepository;
    @Mock
    private CommonOnboardingConfig config;
    @Mock
    private ActivationFlagService activationFlagService;
    @Mock
    private OnboardingProcessLimitService processLimitService;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private IdentityVerificationLimitService tested;

    @Test
    void testCheckLimit_newProcessDoesNotInheritAttemptsFromPreviousProcess() {
        when(config.getVerificationMaxFailedAttempts()).thenReturn(ATTEMPT_LIMIT);
        when(identityVerificationRepository.findByProcessIdOrderByTimestampCreatedDesc(PREVIOUS_PROCESS_ID))
                .thenReturn(IntStream.range(0, ATTEMPT_LIMIT).mapToObj(index -> failedAttempt()).toList());
        when(identityVerificationRepository.findByProcessIdOrderByTimestampCreatedDesc(CURRENT_PROCESS_ID))
                .thenReturn(List.of());
        final var previousProcess = new OnboardingProcessEntity();
        previousProcess.setId(PREVIOUS_PROCESS_ID);
        previousProcess.setActivationId(ACTIVATION_ID);
        previousProcess.setStatus(OnboardingStatus.VERIFICATION_IN_PROGRESS);
        previousProcess.setProcessConfiguration(new OnboardingProcessConfigurationEntity());
        when(onboardingProcessRepository.findById(PREVIOUS_PROCESS_ID)).thenReturn(Optional.of(previousProcess));

        final var ownerId = ownerId();
        assertThrows(OnboardingProcessLimitException.class,
                () -> tested.checkIdentityVerificationLimit(ownerId, PREVIOUS_PROCESS_ID));
        assertEquals(OnboardingStatus.FAILED, previousProcess.getStatus());
        assertDoesNotThrow(() -> tested.checkIdentityVerificationLimit(ownerId, CURRENT_PROCESS_ID));

        verify(onboardingProcessRepository).save(previousProcess);
    }

    @Test
    void testReset_currentProcessWithoutVerificationDoesNotFallBackToActivation() {
        final var process = new OnboardingProcessEntity();
        process.setId(CURRENT_PROCESS_ID);
        process.setActivationId(ACTIVATION_ID);
        when(onboardingProcessRepository.findByActivationIdAndStatus(ACTIVATION_ID, OnboardingStatus.VERIFICATION_IN_PROGRESS))
                .thenReturn(Optional.of(process));
        when(identityVerificationRepository.findFirstByProcessIdOrderByTimestampCreatedDesc(CURRENT_PROCESS_ID))
                .thenReturn(Optional.empty());

        assertThrows(IdentityVerificationException.class,
                () -> tested.resetIdentityVerification(ownerId(), ErrorOrigin.CLEANUP, "cleanup"));

        verify(identityVerificationRepository).findFirstByProcessIdOrderByTimestampCreatedDesc(CURRENT_PROCESS_ID);
        verifyNoMoreInteractions(identityVerificationRepository);
        verifyNoInteractions(processLimitService, activationFlagService);
    }

    private static OwnerId ownerId() {
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        ownerId.setUserId("user-id");
        return ownerId;
    }

    private static IdentityVerificationEntity failedAttempt() {
        final var verification = new IdentityVerificationEntity();
        verification.setActivationId(ACTIVATION_ID);
        verification.setProcessId(PREVIOUS_PROCESS_ID);
        verification.setStatus(IdentityVerificationStatus.FAILED);
        return verification;
    }
}
