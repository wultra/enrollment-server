/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.task.cleaning;

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.ActivationService;
import com.wultra.security.powerauth.client.model.response.ListActivationFlagsResponse;
import com.wultra.security.powerauth.client.model.enumeration.ActivationStatus;
import com.wultra.security.powerauth.client.v4.PowerAuthClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Test for {@link ActivationCleaningService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Transactional
@Sql
class ActivationCleaningServiceTest {

    @Autowired
    private ActivationCleaningService tested;

    @MockitoBean
    private ActivationService activationService;

    @MockitoBean
    private PowerAuthClient powerAuthClient;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() throws Exception {
        when(powerAuthClient.listActivationFlags(any(), any(), any())).thenReturn(new ListActivationFlagsResponse());
    }

    @Test
    void testSuccessful() throws Exception {
        when(activationService.fetchActivationStatus("a2"))
                .thenReturn(ActivationStatus.ACTIVE);

        tested.cleanupActivations();

        final OnboardingProcessEntity process = fetchOnboardingProcess("22222222-df91-4053-bb3d-3970979baf5d");
        assertTrue("activation should be marked as removed", process.isActivationRemoved());
        verify(activationService).removeActivation("a2");
    }

    @Test
    void testAlreadyDeleted() throws Exception {
        when(activationService.fetchActivationStatus("a2"))
                .thenReturn(ActivationStatus.REMOVED);

        tested.cleanupActivations();

        final OnboardingProcessEntity process = fetchOnboardingProcess("22222222-df91-4053-bb3d-3970979baf5d");
        assertTrue("activation should be marked as removed", process.isActivationRemoved());
        verify(activationService, never()).removeActivation("a2");
    }

    @Test
    void testCommunicationException() throws Exception {
        when(activationService.fetchActivationStatus("a2"))
                .thenThrow(new RemoteCommunicationException("test exception"));

        tested.cleanupActivations();

        final OnboardingProcessEntity process = fetchOnboardingProcess("22222222-df91-4053-bb3d-3970979baf5d");
        assertFalse("activation should not be marked as removed", process.isActivationRemoved());
        verify(activationService, never()).removeActivation("a2");
    }

    @Test
    void testKeepsExistingActivationWhenConfigured() throws Exception {
        when(activationService.fetchActivationStatus("a2"))
                .thenReturn(ActivationStatus.ACTIVE);
        final ListActivationFlagsResponse flagsResponse = new ListActivationFlagsResponse();
        flagsResponse.getActivationFlags().add("RE_KYC_IN_PROGRESS");
        when(powerAuthClient.listActivationFlags(any(), any(), any())).thenReturn(flagsResponse);

        tested.cleanupActivations();

        final OnboardingProcessEntity process = fetchOnboardingProcess("44444444-df91-4053-bb3d-3970979baf5d");
        assertTrue("existing activation should be marked as handled", process.isActivationRemoved());
        verify(activationService, never()).removeActivation("a4");
        verify(powerAuthClient).removeActivationFlags(any(), any(), any());
    }

    private OnboardingProcessEntity fetchOnboardingProcess(final String id) {
        return entityManager.find(OnboardingProcessEntity.class, id);
    }
}
