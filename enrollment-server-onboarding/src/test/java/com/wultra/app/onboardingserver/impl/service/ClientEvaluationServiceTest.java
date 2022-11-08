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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ClientEvaluationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ClientEvaluationServiceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private OnboardingProvider onboardingProvider;

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private IdentityVerificationConfig identityVerificationConfig;

    @InjectMocks
    private ClientEvaluationService tested;

    @Test
    void testProcessClientEvaluation_successful() throws Exception {
        when(identityVerificationConfig.getClientEvaluationMaxFailedAttempts())
                .thenReturn(1);

        final EvaluateClientRequest evaluateClientRequest = EvaluateClientRequest.builder()
                .processId("p1")
                .userId("u1")
                .identityVerificationId("i1")
                .verificationId("v1")
                .build();
        final EvaluateClientResponse evaluateClientResponse = EvaluateClientResponse.builder()
                .accepted(true)
                .build();
        when(onboardingProvider.evaluateClient(evaluateClientRequest))
                .thenReturn(evaluateClientResponse);

        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("i1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setPhase(CLIENT_EVALUATION);
        identityVerification.setDocumentVerifications(Set.of(
                createDocumentVerification("d1", DocumentStatus.ACCEPTED, "v1"),
                createDocumentVerification("d2", DocumentStatus.ACCEPTED, "v1"),
                createDocumentVerification("d3", DocumentStatus.DISPOSED, "v2")));

        final OwnerId ownerId = new OwnerId();

        tested.processClientEvaluation(identityVerification, ownerId);

        verify(identityVerificationService).moveToPhaseAndStatus(identityVerification, CLIENT_EVALUATION, ACCEPTED, ownerId);
    }

    @Test
    void testProcessClientEvaluation_invalidVerificationId() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("i1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setPhase(CLIENT_EVALUATION);
        identityVerification.setDocumentVerifications(Set.of(
                createDocumentVerification("d1", DocumentStatus.ACCEPTED, "v1"),
                createDocumentVerification("d2", DocumentStatus.ACCEPTED, "v2")));

        final OwnerId ownerId = new OwnerId();

        tested.processClientEvaluation(identityVerification, ownerId);

        verify(identityVerificationService).moveToPhaseAndStatus(identityVerification, CLIENT_EVALUATION, FAILED, ownerId);

        assertEquals("unableToGetDocumentVerificationId", identityVerification.getErrorDetail());
        assertEquals(ErrorOrigin.CLIENT_EVALUATION, identityVerification.getErrorOrigin());
    }

    @Test
    void testProcessClientEvaluation_tooManyAttempts() throws Exception {
        when(identityVerificationConfig.getClientEvaluationMaxFailedAttempts())
                .thenReturn(1);

        final EvaluateClientRequest evaluateClientRequest = EvaluateClientRequest.builder()
                .processId("p1")
                .userId("u1")
                .identityVerificationId("i1")
                .verificationId("v1")
                .build();
        final EvaluateClientResponse evaluateClientResponse = EvaluateClientResponse.builder()
                .accepted(true)
                .build();
        when(onboardingProvider.evaluateClient(evaluateClientRequest))
                .thenThrow(new OnboardingProviderException());

        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("i1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setPhase(CLIENT_EVALUATION);
        identityVerification.setDocumentVerifications(Set.of(
                createDocumentVerification("d1", DocumentStatus.ACCEPTED, "v1")));

        final OwnerId ownerId = new OwnerId();

        tested.processClientEvaluation(identityVerification, ownerId);

        verify(identityVerificationService).moveToPhaseAndStatus(identityVerification, CLIENT_EVALUATION, FAILED, ownerId);

        assertEquals("maxFailedAttemptsClientEvaluation", identityVerification.getErrorDetail());
        assertEquals(ErrorOrigin.PROCESS_LIMIT_CHECK, identityVerification.getErrorOrigin());
    }

    private static DocumentVerificationEntity createDocumentVerification(final String id, final DocumentStatus status, final String verificationId) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setId(id);
        documentVerification.setFilename(UUID.randomUUID().toString());
        documentVerification.setStatus(status);
        documentVerification.setVerificationId(verificationId);
        documentVerification.setUsedForVerification(true);
        return documentVerification;
    }
}
