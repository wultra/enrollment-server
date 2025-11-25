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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.OnboardingStartRequest;
import com.wultra.app.enrollmentserver.api.model.onboarding.response.OnboardingStartResponse;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.response.LookupUserResponse;
import com.wultra.core.http.common.request.RequestContext;
import com.wultra.security.powerauth.client.model.request.InitActivationRequest;
import com.wultra.security.powerauth.client.model.response.InitActivationResponse;
import com.wultra.security.powerauth.client.model.response.LookupApplicationByAppKeyResponse;
import com.wultra.security.powerauth.client.v3.PowerAuthClient;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link OnboardingServiceImpl}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Sql
@Transactional
class OnboardingServiceImplTest {

    @MockitoBean
    private OnboardingProvider onboardingProvider;

    @MockitoBean
    private PowerAuthClient powerAuthClient;

    @Autowired
    private OnboardingServiceImpl tested;

    @Test
    void testStartProcess() throws Exception {
        final OnboardingStartRequest request = OnboardingStartRequest.builder()
                .identification(Map.of("username", "john.doe"))
                .processType("reactivation")
                .build();
        final RequestContext context = RequestContext.builder().build();
        final EncryptionContext encryptionContext = new EncryptionContext("CIx/arZ6CUphVBv9xnddPA==", null, null, null, null);

        when(onboardingProvider.lookupUser(any())).thenReturn(LookupUserResponse.builder()
                .userId("mock_user")
                .build());

        final LookupApplicationByAppKeyResponse appKeyResponse = new LookupApplicationByAppKeyResponse();
        appKeyResponse.setApplicationId("mock_app_id");

        when(powerAuthClient.lookupApplicationByAppKey(any(), any(), any()))
                .thenReturn(appKeyResponse);

        final InitActivationResponse initResponse = new InitActivationResponse();
        initResponse.setActivationCode("mock_activation_code");

        when(powerAuthClient.initActivation(any(), any(), any()))
                .thenReturn(initResponse);

        final OnboardingStartResponse result = tested.startOnboarding(request, context, encryptionContext);

        assertNotNull(result);
        assertNotNull(result.processId());
        assertEquals(OnboardingStatus.ACTIVATION_IN_PROGRESS, result.onboardingStatus());
        assertNotNull(result.activationCode());
        assertEquals("mock_activation_code", result.activationCode());

        final ArgumentCaptor<InitActivationRequest> requestCaptor = ArgumentCaptor.forClass(InitActivationRequest.class);
        verify(powerAuthClient).initActivation(requestCaptor.capture(), any(), any());

        final InitActivationRequest captorValue = requestCaptor.getValue();
        assertEquals("mock_app_id", captorValue.getApplicationId());
        assertEquals(List.of("VERIFICATION_PENDING"), captorValue.getFlags());
    }

    @Test
    void testStartProcess_defaultProcessType() throws Exception {
        final OnboardingStartRequest request = OnboardingStartRequest.builder()
                .identification(Map.of("username", "john.doe"))
                .processType("") // blank on purpose
                .build();
        final RequestContext context = RequestContext.builder().build();
        final EncryptionContext encryptionContext = new EncryptionContext("CIx/arZ6CUphVBv9xnddPA==", null, null, null, null);

        when(onboardingProvider.lookupUser(any())).thenReturn(LookupUserResponse.builder()
                .userId("mock_user")
                .build());

        final LookupApplicationByAppKeyResponse appKeyResponse = new LookupApplicationByAppKeyResponse();
        appKeyResponse.setApplicationId("mock_app_id");

        when(powerAuthClient.lookupApplicationByAppKey(any(), any(), any()))
                .thenReturn(appKeyResponse);

        final InitActivationResponse initResponse = new InitActivationResponse();
        initResponse.setActivationCode("mock_activation_code");

        when(powerAuthClient.initActivation(any(), any(), any()))
                .thenReturn(initResponse);

        final OnboardingStartResponse result = tested.startOnboarding(request, context, encryptionContext);

        assertNotNull(result);
        assertNotNull(result.processId());
        assertEquals(OnboardingStatus.ACTIVATION_IN_PROGRESS, result.onboardingStatus());
        assertNotNull(result.activationCode());
    }

    @Test
    void testStartProcess_nullUser() throws Exception {
        final OnboardingStartRequest request = OnboardingStartRequest.builder()
                .identification(Map.of("username", "john.doe"))
                .processType("reactivation")
                .build();
        final RequestContext context = RequestContext.builder().build();
        final EncryptionContext encryptionContext = new EncryptionContext("CIx/arZ6CUphVBv9xnddPA==", null, null, null, null);

        when(onboardingProvider.lookupUser(any())).thenThrow(new OnboardingProviderException("User not found."));

        final OnboardingStartResponse result = tested.startOnboarding(request, context, encryptionContext);

        verify(powerAuthClient, never()).initActivation(any(), any(), any());

        assertNotNull(result);
        assertNotNull(result.processId());
        assertEquals(OnboardingStatus.ACTIVATION_IN_PROGRESS, result.onboardingStatus());
        assertNotNull(result.activationCode());
        assertTrue(result.activationCode().matches("([A-Z0-9]{5}-){3}[A-Z0-9]{5}"),
                "Activation code should match pattern: XXXXX-XXXXX-XXXXX-XXXXX");
    }

    @Test
    void testStartProcess_invalidProcessType() {
        final OnboardingStartRequest request = OnboardingStartRequest.builder()
                .identification(Map.of("username", "john.doe"))
                .processType("non-existing")
                .build();
        final RequestContext context = RequestContext.builder().build();
        final EncryptionContext encryptionContext = new EncryptionContext(null, null, null, null, null);

        final OnboardingProcessException result = assertThrows(OnboardingProcessException.class, () -> tested.startOnboarding(request, context, encryptionContext));

        assertEquals("No configuration found for process type: non-existing", result.getMessage());
    }
}
