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
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.errorhandling.*;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.core.rest.model.base.request.ObjectRequest;
import com.wultra.security.powerauth.rest.api.spring.authentication.impl.PowerAuthApiAuthenticationImpl;
import com.wultra.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthAuthenticationException;
import com.wultra.security.powerauth.rest.api.spring.exception.PowerAuthEncryptionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for {@link IdentityVerificationRestService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class IdentityVerificationRestServiceIntTest {

    @Autowired
    private IdentityVerificationRestService tested;

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
}
