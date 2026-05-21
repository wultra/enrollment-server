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

import com.wultra.app.enrollmentserver.model.enumeration.RejectOrigin;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.SelfieRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.database.entity.ScaResultEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.response.ApproveClientResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    private SelfieRepository selfieRepository;

    @MockitoBean
    private AuditService auditService;

    @Autowired
    private OnboardingApprovalService tested;

    @Test
    void testApprove() throws Exception {
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
                .thenReturn(ApproveClientResponse.builder()
                        .result(ApproveClientResponse.ApprovalResult.OK)
                        .build());

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setPresenceCheckResult(ScaResultEntity.Result.SUCCESS);
        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(any()))
                .thenReturn(Optional.of(scaResult));

        final OnboardingApprovalService.ApprovalResult result = tested.approve(identityVerification);

        assertEquals(OnboardingApprovalService.ApprovalResult.OK, result);

        verify(onboardingProvider).approveClient(any());
        verify(selfieRepository).findTopByIdentityVerificationOrderByTimestampCreatedDesc(identityVerification);
    }

    @Test
    void testReject() throws Exception {
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
                .thenReturn(ApproveClientResponse.builder()
                        .result(ApproveClientResponse.ApprovalResult.NOK)
                        .resultReason("Some reason")
                        .build());

        final ScaResultEntity scaResult = new ScaResultEntity();
        scaResult.setPresenceCheckResult(ScaResultEntity.Result.SUCCESS);
        when(scaResultRepository.findTopByIdentityVerificationOrderByTimestampCreatedDesc(any()))
                .thenReturn(Optional.of(scaResult));

        final OnboardingApprovalService.ApprovalResult result = tested.approve(identityVerification);

        assertEquals(OnboardingApprovalService.ApprovalResult.NOK, result);

        final ArgumentCaptor<IdentityVerificationEntity> argumentCaptor = ArgumentCaptor.forClass(IdentityVerificationEntity.class);

        verify(onboardingProvider).approveClient(any());
        verify(selfieRepository).findTopByIdentityVerificationOrderByTimestampCreatedDesc(identityVerification);
        verify(auditService).audit(argumentCaptor.capture(), any(), any(Object[].class));

        assertEquals("Some reason", argumentCaptor.getValue().getRejectReason());
        assertEquals(RejectOrigin.CLIENT_APPROVAL, argumentCaptor.getValue().getRejectOrigin());
    }
}
