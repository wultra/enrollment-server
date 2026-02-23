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
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.integration.DocumentSubmitResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
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
    private IdentityVerificationConfig identityVerificationConfig;

    @Mock
    private CommonOnboardingService onboardingService;

    @Mock
    private ClientEvaluationDocumentCheckResultBuilder clientEvaluationDocumentCheckResultBuilder;

    private ClientEvaluationService tested;

    @BeforeEach
    void setUp() {
        when(identityVerificationConfig.getClientEvaluationMaxFailedAttempts()).thenReturn(1);

        tested = new ClientEvaluationService(
                onboardingProvider,
                identityVerificationConfig,
                auditService,
                onboardingService,
                clientEvaluationDocumentCheckResultBuilder
        );
    }

    @Test
    void testProcessClientEvaluation_successfulWithExtractedData() throws Exception {
        when(identityVerificationConfig.getClientEvaluationMaxFailedAttempts())
                .thenReturn(1);
        when(identityVerificationConfig.isSendingExtractedDataEnabled())
                .thenReturn(true);
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("testProvider");

        final OnboardingProcessConfigurationEntity processConfiguration = new OnboardingProcessConfigurationEntity();
        processConfiguration.setProcessType("onboarding");
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId("p1");
        process.setProcessConfiguration(processConfiguration);

        when(onboardingService.findProcess("p1")).thenReturn(process);

        final EvaluateClientResponse evaluateClientResponse = EvaluateClientResponse.builder()
                .evaluationResult(EvaluateClientResponse.EvaluationResult.OK)
                .build();
        when(onboardingProvider.evaluateClient(any(EvaluateClientRequest.class)))
                .thenReturn(evaluateClientResponse);

        when(clientEvaluationDocumentCheckResultBuilder.build(anySet(), eq(true)))
                .thenReturn(EvaluateClientRequest.DocumentCheckResult.builder().build());

        final var documentVerifications = Set.of(
                createDocumentVerificationWithResults("d1", """
                {"dateOfBirth": "24.12.1999"}""", DocumentType.ID_CARD),
                createDocumentVerificationWithResults("d2", DocumentSubmitResult.NO_DATA_EXTRACTED, DocumentType.DRIVING_LICENSE),
                createDocumentVerification("d3", DocumentStatus.DISPOSED, "v2"));

        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("i1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setPhase(CLIENT_EVALUATION);
        identityVerification.setDocumentVerifications(documentVerifications);

        final OwnerId ownerId = new OwnerId();

        final var result = tested.processClientEvaluation(identityVerification, ownerId);

        assertEquals(EvaluateClientResponse.EvaluationResult.OK, result);
    }

    @Test
    void testProcessClientEvaluation_invalidVerificationId() throws OnboardingProcessException {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("i1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setPhase(CLIENT_EVALUATION);
        identityVerification.setDocumentVerifications(Set.of(
                createDocumentVerification("d1", DocumentStatus.ACCEPTED, "v1"),
                createDocumentVerification("d2", DocumentStatus.ACCEPTED, "v2")));

        final OwnerId ownerId = new OwnerId();

        when(onboardingService.findProcess("p1")).thenThrow(new OnboardingProcessException("Test exception verification not found"));

        final var response = tested.processClientEvaluation(identityVerification, ownerId);

        assertNull(response);
        assertEquals("unableToGetDocumentVerificationId", identityVerification.getErrorDetail());
        assertEquals(ErrorOrigin.CLIENT_EVALUATION, identityVerification.getErrorOrigin());
    }

    @Test
    void testProcessClientEvaluation_tooManyAttempts() throws Exception {
        when(identityVerificationConfig.getClientEvaluationMaxFailedAttempts())
                .thenReturn(1);

        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("testProvider");

        final EvaluateClientRequest evaluateClientRequest = buildRequestWithoutExtractedData();

        when(onboardingProvider.evaluateClient(evaluateClientRequest))
                .thenThrow(new OnboardingProviderException());

        final OnboardingProcessConfigurationEntity processConfiguration = new OnboardingProcessConfigurationEntity();
        processConfiguration.setProcessType("onboarding");
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId("p1");
        process.setProcessConfiguration(processConfiguration);
        when(onboardingService.findProcess("p1"))
                .thenReturn(process);

        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("i1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setPhase(CLIENT_EVALUATION);
        identityVerification.setDocumentVerifications(Set.of(
                createDocumentVerification("d1", DocumentStatus.ACCEPTED, "v1")));

        final OwnerId ownerId = new OwnerId();

        final var result = tested.processClientEvaluation(identityVerification, ownerId);

        assertNull(result);
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

    private static DocumentVerificationEntity createDocumentVerificationWithResults(final String id, final String extractedData, final DocumentType documentType) {
        final DocumentResultEntity documentResult = new DocumentResultEntity();
        documentResult.setExtractedData(extractedData);

        final DocumentVerificationEntity documentVerification = createDocumentVerification(id, DocumentStatus.ACCEPTED, "v1");
        documentVerification.setResults(Set.of(documentResult));
        documentVerification.setPhotoId("photo1");
        documentVerification.setType(documentType);
        return documentVerification;
    }

    private static EvaluateClientRequest buildRequestWithoutExtractedData() {
        final var checkResult = new EvaluateClientRequest.DocumentCheckResult(List.of(), null);

        return EvaluateClientRequest.builder()
                .processId("p1")
                .processType("onboarding")
                .userId("u1")
                .identityVerificationId("i1")
                .verificationId("v1")
                .provider("testProvider")
                .status(EvaluateClientRequest.Status.SUCCESS)
                .documentCheckResult(checkResult)
                .build();
    }
}
