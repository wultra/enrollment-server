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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.service.ActivationFlagService;
import com.wultra.app.onboardingserver.common.service.IdentityVerificationLimitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_UPLOAD;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IdentityVerificationCreateService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationCreateServiceTest {

    private static final String PROCESS_ID = "process-id";
    private static final String ACTIVATION_FLAG = "RE_KYC_IN_PROGRESS";

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private ActivationFlagService activationFlagService;

    @Mock
    private IdentityVerificationLimitService identityVerificationLimitService;

    @Mock
    private OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    @InjectMocks
    private IdentityVerificationCreateService tested;

    @Test
    void testCreateIdentityVerification_existingActivationSetsConfiguredFlag() throws Exception {
        final OwnerId ownerId = new OwnerId();
        when(onboardingProcessConfigurationService.findConfigByProcessId(PROCESS_ID)).thenReturn(
                OnboardingProcessConfigurationValue.builder()
                        .existingActivation(true)
                        .existingActivationFlag(ACTIVATION_FLAG)
                        .build());

        tested.createIdentityVerification(ownerId, PROCESS_ID);

        verify(identityVerificationLimitService).checkIdentityVerificationLimit(ownerId);
        verify(identityVerificationService).moveToPhaseAndStatus(any(), eq(DOCUMENT_UPLOAD), eq(IN_PROGRESS), eq(ownerId));
        verify(activationFlagService).addActivationFlag(ownerId, ACTIVATION_FLAG);
        verify(activationFlagService, never()).initActivationFlagsForIdentityVerification(ownerId);
    }

    @Test
    void testCreateIdentityVerification_newActivationInitializesLegacyFlags() throws Exception {
        final OwnerId ownerId = new OwnerId();
        when(onboardingProcessConfigurationService.findConfigByProcessId(PROCESS_ID)).thenReturn(
                OnboardingProcessConfigurationValue.builder().build());

        tested.createIdentityVerification(ownerId, PROCESS_ID);

        verify(identityVerificationLimitService).checkIdentityVerificationLimit(ownerId);
        verify(identityVerificationService).moveToPhaseAndStatus(any(), eq(DOCUMENT_UPLOAD), eq(IN_PROGRESS), eq(ownerId));
        verify(activationFlagService).initActivationFlagsForIdentityVerification(ownerId);
        verify(activationFlagService, never()).addActivationFlag(ownerId, ACTIVATION_FLAG);
    }
}
