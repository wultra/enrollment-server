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

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.security.powerauth.client.model.request.RemoveActivationFlagsRequest;
import com.wultra.security.powerauth.client.v4.PowerAuthClient;
import com.wultra.security.powerauth.rest.api.spring.service.HttpCustomizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ActivationFlagService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ActivationFlagServiceTest {

    private static final String ACTIVATION_ID = "activation-id";
    private static final String ACTIVATION_FLAG = "RE_KYC_IN_PROGRESS";

    @Mock
    private PowerAuthClient powerAuthClient;

    @Mock
    private HttpCustomizationService httpCustomizationService;

    @InjectMocks
    private ActivationFlagService tested;

    @Captor
    private ArgumentCaptor<RemoveActivationFlagsRequest> removeRequestCaptor;

    @Test
    void testRemoveActivationFlagWithoutFetchingFlags() throws Exception {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);

        tested.removeActivationFlag(ownerId, ACTIVATION_FLAG);

        verify(powerAuthClient).removeActivationFlags(removeRequestCaptor.capture(), any(), any());
        verify(powerAuthClient, never()).listActivationFlags(any(), any(), any());
        assertEquals(ACTIVATION_ID, removeRequestCaptor.getValue().getActivationId());
        assertEquals(1, removeRequestCaptor.getValue().getActivationFlags().size());
        assertEquals(ACTIVATION_FLAG, removeRequestCaptor.getValue().getActivationFlags().get(0));
    }

    @Test
    void testUpdateActivationFlagsForSucceededIdentityVerification_existingActivation() throws Exception {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        final var configuration = OnboardingProcessConfigurationValue.builder()
                .existingActivation(true)
                .existingActivationFlag(ACTIVATION_FLAG)
                .build();

        tested.updateActivationFlagsForSucceededIdentityVerification(ownerId, configuration);

        verify(powerAuthClient).removeActivationFlags(removeRequestCaptor.capture(), any(), any());
        verify(powerAuthClient, never()).listActivationFlags(any(), any(), any());
        assertEquals(ACTIVATION_FLAG, removeRequestCaptor.getValue().getActivationFlags().get(0));
    }
}
