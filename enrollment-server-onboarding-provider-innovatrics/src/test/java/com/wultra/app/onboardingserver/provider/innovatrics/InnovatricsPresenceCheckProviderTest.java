/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.innovatrics;

import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CustomerInspectResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.EvaluateCustomerLivenessResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.SelfieInspection;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.SelfieSimilarityWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Test for {@link InnovatricsPresenceCheckProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class InnovatricsPresenceCheckProviderTest {

    private static final String CUSTOMER_ID = "customer-1";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InnovatricsConfigProps innovatricsConfigProps;

    @Mock
    private InnovatricsApiService innovatricsApiService;

    @InjectMocks
    private InnovatricsPresenceCheckProvider tested;

    @Test
    void testGetResult_success() throws Exception {
        final OwnerId id = new OwnerId();
        final SessionInfo sessionInfo = createSessionInfo();

        when(innovatricsApiService.evaluateLiveness(CUSTOMER_ID, id))
                .thenReturn(new EvaluateCustomerLivenessResponse(0.95, null));
        when(innovatricsConfigProps.getPresenceCheckConfiguration().getScore())
                .thenReturn(0.80);
        when(innovatricsApiService.inspectCustomer(CUSTOMER_ID, id))
                .thenReturn(createCustomerInspectResponse(true, true));

        final PresenceCheckResult result = tested.getResult(id, sessionInfo);

        assertEquals(PresenceCheckStatus.ACCEPTED, result.getStatus());
        assertNull(result.getErrorDetail());
        assertNull(result.getRejectReason());
    }

    @Test
    void testGetResult_livenessFailed() throws Exception {
        final OwnerId id = new OwnerId();
        final SessionInfo sessionInfo = createSessionInfo();

        when(innovatricsApiService.evaluateLiveness(CUSTOMER_ID, id))
                .thenReturn(new EvaluateCustomerLivenessResponse(null, EvaluateCustomerLivenessResponse.ErrorCodeEnum.NOT_ENOUGH_DATA));

        final PresenceCheckResult result = tested.getResult(id, sessionInfo);

        assertEquals(PresenceCheckStatus.FAILED, result.getStatus());
        assertEquals("NOT_ENOUGH_DATA", result.getErrorDetail());
        assertNull(result.getRejectReason());
    }

    @Test
    void testGetResult_livenessRejected() throws Exception {
        final OwnerId id = new OwnerId();
        final SessionInfo sessionInfo = createSessionInfo();

        when(innovatricsApiService.evaluateLiveness(CUSTOMER_ID, id))
                .thenReturn(new EvaluateCustomerLivenessResponse(0.70, null));
        when(innovatricsConfigProps.getPresenceCheckConfiguration().getScore())
                .thenReturn(0.875);

        final PresenceCheckResult result = tested.getResult(id, sessionInfo);

        assertEquals(PresenceCheckStatus.REJECTED, result.getStatus());
        assertNull(result.getErrorDetail());
        assertEquals("Score 0.700 is bellow the threshold 0.875", result.getRejectReason());
    }

    @Test
    void testGetResult_customerInspectionFailed() throws Exception {
        final OwnerId id = new OwnerId();
        final SessionInfo sessionInfo = createSessionInfo();

        when(innovatricsApiService.evaluateLiveness(CUSTOMER_ID, id))
                .thenReturn(new EvaluateCustomerLivenessResponse(0.95, null));
        when(innovatricsConfigProps.getPresenceCheckConfiguration().getScore())
                .thenReturn(0.80);
        when(innovatricsApiService.inspectCustomer(CUSTOMER_ID, id))
                .thenReturn(new CustomerInspectResponse()); // selfieInspection == null

        final PresenceCheckResult result = tested.getResult(id, sessionInfo);

        assertEquals(PresenceCheckStatus.FAILED, result.getStatus());
        assertEquals("Missing selfie inspection payload", result.getErrorDetail());
        assertNull(result.getRejectReason());
    }

    @Test
    void testGetResult_customerInspectionRejected() throws Exception {
        final OwnerId id = new OwnerId();
        final SessionInfo sessionInfo = createSessionInfo();

        when(innovatricsApiService.evaluateLiveness(CUSTOMER_ID, id))
                .thenReturn(new EvaluateCustomerLivenessResponse(0.95, null));
        when(innovatricsConfigProps.getPresenceCheckConfiguration().getScore())
                .thenReturn(0.80);
        when(innovatricsApiService.inspectCustomer(CUSTOMER_ID, id))
                .thenReturn(createCustomerInspectResponse(false, true));

        final PresenceCheckResult result = tested.getResult(id, sessionInfo);

        assertEquals(PresenceCheckStatus.REJECTED, result.getStatus());
        assertNull(result.getErrorDetail());
        assertEquals("The person in the selfie does not match a person in the document portrait", result.getRejectReason());
    }

    private static SessionInfo createSessionInfo() {
        final SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionAttributes(Map.of("InnovatricsCustomerId", CUSTOMER_ID));
        return sessionInfo;
    }

    private static CustomerInspectResponse createCustomerInspectResponse(final Boolean documentPortrait, final Boolean livenessSelfies) {
        return new CustomerInspectResponse()
                .selfieInspection(new SelfieInspection()
                        .similarityWith(new SelfieSimilarityWith(documentPortrait, livenessSelfies)));
    }
}