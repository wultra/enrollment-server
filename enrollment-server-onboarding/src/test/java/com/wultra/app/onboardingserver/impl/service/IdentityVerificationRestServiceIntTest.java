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

import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitV2Request;
import com.wultra.app.enrollmentserver.api.model.onboarding.request.IdentityVerificationCleanupRequest;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.api.provider.PresenceCheckProvider;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.security.powerauth.rest.api.spring.authentication.impl.PowerAuthActivationImpl;
import com.wultra.security.powerauth.rest.api.spring.authentication.impl.PowerAuthApiAuthenticationImpl;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link IdentityVerificationRestService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestPropertySource(properties = {
        "enrollment-server-onboarding.document-verification.cleanupEnabled=true",
        "enrollment-server-onboarding.presence-check.cleanupEnabled=true"
})
class IdentityVerificationRestServiceIntTest {

    @Autowired
    private IdentityVerificationRestService tested;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DocumentVerificationProvider documentVerificationProvider;

    @MockitoBean
    private PresenceCheckProvider presenceCheckProvider;

    @Test
    void testSubmitDocumentsV2_newDocumentsAreSubmitted_responseOk() throws OnboardingProcessException, RemoteCommunicationException, IdentityVerificationLimitException, DocumentSubmitException, PowerAuthEncryptionException, IdentityVerificationException, OnboardingProcessLimitException, PowerAuthAuthenticationException {
        // given
        final var request = DocumentSubmitV2Request.builder()
                .processId("b9d4cf32-3e3c-4bb1-8f66-5a3c91fe2f8b")
                .documents(List.of(
                        DocumentSubmitV2Request.Document.builder()
                                .type(DocumentType.ID_CARD)
                                .side(CardSide.FRONT)
                                .filename("id_card_front.jpg")
                                .data(Base64.getEncoder().encodeToString(new byte[] { 1 }))
                                .build(),
                        DocumentSubmitV2Request.Document.builder()
                                .type(DocumentType.ID_CARD)
                                .side(CardSide.BACK)
                                .filename("id_card_back.jpg")
                                .data(Base64.getEncoder().encodeToString(new byte[] { 2 }))
                                .build()
                ))
                .build();

        final var requestObject = new ObjectRequest<>(request);
        final var encryptionContext = new EncryptionContext(null, "b7717831-4ed3-4597-88c1-b4646b91a76f", null, null, null);
        final var apiAuthentication = new PowerAuthApiAuthenticationImpl();

        // when
        final var response = tested.submitDocumentsV2(requestObject, encryptionContext, apiAuthentication);

        // then
        assertEquals("OK", response.getStatus());
    }

    @Test
    void testSubmitDocumentsV2_documentsAreResubmitted_responseOk() throws OnboardingProcessException, RemoteCommunicationException, IdentityVerificationLimitException, DocumentSubmitException, PowerAuthEncryptionException, IdentityVerificationException, OnboardingProcessLimitException, PowerAuthAuthenticationException {
        // given
        final var request = DocumentSubmitV2Request.builder()
                .processId("c3c2c4c1-2a84-4c6e-a8bd-4e3b824d5c91")
                .resubmit(true)
                .documents(List.of(
                        DocumentSubmitV2Request.Document.builder()
                                .originalDocumentId("bc8b0f4a-8a6f-4dc7-ae2b-05d6ca059e33")
                                .type(DocumentType.ID_CARD)
                                .side(CardSide.FRONT)
                                .filename("id_card_front.jpg")
                                .data(Base64.getEncoder().encodeToString(new byte[] { 1 }))
                                .build(),
                        DocumentSubmitV2Request.Document.builder()
                                .originalDocumentId("8f3a3887-6f7a-4d84-b907-3bdf257cbb46")
                                .type(DocumentType.ID_CARD)
                                .side(CardSide.BACK)
                                .filename("id_card_back.jpg")
                                .data(Base64.getEncoder().encodeToString(new byte[] { 2 }))
                                .build()
                ))
                .build();

        final var requestObject = new ObjectRequest<>(request);
        final var encryptionContext = new EncryptionContext(null, "8e4a0b0a-84f3-4c71-9cc0-c9ac7b3f6593", null, null, null);
        final var apiAuthentication = new PowerAuthApiAuthenticationImpl();

        // when
        final var response = tested.submitDocumentsV2(requestObject, encryptionContext, apiAuthentication);

        // then
        assertEquals("OK", response.getStatus());
    }

    /**
     * Tests complete cleanup.
     * For identity verification the following configuration is applied:
     * - enrollment-server-onboarding.document-verification.cleanupEnabled=true
     * For presence check the following configuration is applied:
     * - enrollment-server-onboarding.presence-check.enabled=true
     * - enrollment-server-onboarding.presence-check.cleanupEnabled=true
     * During cleanup, none of these limits are reached:
     * - enrollment-server-onboarding.onboarding-process.max-error-score
     * - enrollment-server-onboarding.identity-verification.max-failed-attempts
     *
     * @throws Exception Any exception that occurs during the test
     */
    @Test
    void testCleanup_fullDocumentVerificationAndPresenceCheckCleanup_allDataAreCleaned() throws Exception {
        // given
        final var request = new IdentityVerificationCleanupRequest();
        request.setProcessId("0c47c3cf-6f77-4f52-93f2-934efc6322dd");

        final var activationContext = new PowerAuthActivationImpl();
        activationContext.setUserId("mockuser_264962080414477774");
        activationContext.setActivationId("5d1fbb02-94b9-4a49-a0fd-7cda061ca655");

        final var apiAuthentication = new PowerAuthApiAuthenticationImpl();
        apiAuthentication.setActivationContext(activationContext);

        // when
        tested.cleanup(new ObjectRequest<>(request), apiAuthentication);

        // then
        assertDocumentVerificationAndPresenceCheckCleanup();
    }

    private void assertDocumentVerificationAndPresenceCheckCleanup() throws Exception {
        final var documentDataCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM es_document_data", Integer.class);
        assertEquals(0, documentDataCount);

        final var processedDocumentDataCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM es_processed_document_data", Integer.class);
        assertEquals(0, processedDocumentDataCount);

        final var documentResultCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) 
                FROM es_document_result 
                WHERE extracted_data IS null AND verification_result IS null""",
                Integer.class);
        assertEquals(3, documentResultCount);

        final var documentVerificationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) 
                FROM es_document_verification 
                WHERE status = 'FAILED' 
                  AND used_for_verification = false 
                  AND timestamp_last_updated > DATEADD('MINUTE', -1, CURRENT_TIMESTAMP)""",
                Integer.class);
        assertEquals(3, documentVerificationCount);

        final var identityVerificationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) 
                FROM es_identity_verification 
                WHERE id = '08923a0a-5f4c-41dc-acda-75bb921c75a4' 
                  AND phase = 'COMPLETED' 
                  AND status = 'FAILED' 
                  AND error_origin = 'CLEANUP' 
                  AND error_detail = 'reset due to cleanup' 
                  AND timestamp_last_updated > DATEADD('MINUTE', -1, CURRENT_TIMESTAMP) 
                  AND timestamp_failed > DATEADD('MINUTE', -1, CURRENT_TIMESTAMP)""",
                Integer.class);
        assertEquals(1, identityVerificationCount);

        final var onboardingProcessCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) 
                FROM es_onboarding_process 
                WHERE id = '0c47c3cf-6f77-4f52-93f2-934efc6322dd' 
                  AND error_score = 3""",
                Integer.class);
        assertEquals(1, onboardingProcessCount);

        verify(documentVerificationProvider).cleanupDocuments(
                any(OwnerId.class),
                argThat(it -> it.containsAll(List.of("c8bf612b-6718-4614-b150-ce17bc39221c", "03a2785a-a563-4feb-bde4-3ce0367e4e9d", "62338ec9-ff2f-4751-9762-e09d34c62796"))));

        final var sessionInfo = new SessionInfo();
        sessionInfo.setSessionAttributes(Map.of(
                SessionInfo.ATTRIBUTE_IMAGE_UPLOADED, true,
                "mockVerificationToken", "d988b368-ff76-4ec1-9e2d-3fbbe61cb854",
                SessionInfo.ATTRIBUTE_TIMESTAMP_LAST_USED, 1772449728009L));

        verify(presenceCheckProvider).cleanupIdentityData(
                any(OwnerId.class),
                eq(sessionInfo));
    }
}
