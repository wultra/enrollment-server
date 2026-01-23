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

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.database.entity.ScaResultEntity;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.response.ApproveClientResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link OnboardingApprovalService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class OnboardingApprovalServiceTest {

    @MockitoBean
    private OnboardingServiceImpl onboardingService;

    @MockitoBean
    private OnboardingProvider onboardingProvider;

    @MockitoBean
    private ScaResultRepository scaResultRepository;

    @Autowired
    private OnboardingApprovalService tested;

    @Test
    void testApproveRetry() throws Exception {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("verification-1");
        identityVerification.setProcessId("process-1");
        identityVerification.setUserId("user-1");

        final OnboardingProcessConfigurationEntity processConfiguration = new OnboardingProcessConfigurationEntity();
        processConfiguration.setProcessType("onboarding");

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setProcessConfiguration(processConfiguration);

        when(onboardingService.findProcess(identityVerification.getProcessId()))
                .thenReturn(process);

        when(onboardingProvider.approveClient(any()))
                .thenThrow(new OnboardingProviderException("approval failed"))
                .thenReturn(ApproveClientResponse.builder()
                        .result(ApproveClientResponse.EvaluationResult.OK)
                        .build());

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setPresenceCheckResult(ScaResultEntity.Result.SUCCESS);
        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(any()))
                .thenReturn(Optional.of(scaResult));

        final ApproveClientResponse.EvaluationResult result = tested.approve(identityVerification);

        assertEquals(ApproveClientResponse.EvaluationResult.OK, result);

        verify(onboardingProvider, times(2)).approveClient(any());
    }
}
