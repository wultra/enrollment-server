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
package com.wultra.app.onboardingserver.impl.service.userdatastore;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.security.userdatastore.client.UserDataStoreClient;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.response.DocumentCreateResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultUserDataStoreService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class DefaultUserDataStoreServiceTest {

    @SpringBootTest(
            classes = {DefaultUserDataStoreService.class},
            properties = {
                    "enrollment-server-onboarding.user-data-store.enabled=true",
                    "enrollment-server-onboarding.user-data-store.restClientConfig.baseUrl=http://example.com/uds",
                    "enrollment-server-onboarding.user-data-store.document-type=ALL",
                    "enrollment-server-onboarding.user-data-store.store-extracted-data=true",
                    "enrollment-server-onboarding.user-data-store.store-document-image-scan=true"
            }
    )
    @EnableConfigurationProperties(UserDataStoreConfigProperties.class)
    @Nested
    class AllDocumentsTest {

        @Autowired
        private DefaultUserDataStoreService tested;

        @MockitoBean
        private UserDataStoreClient userDataStoreClient;

        @MockitoBean
        private OnboardingProcessRepository onboardingProcessRepository;

        @MockitoBean
        private IdentityVerificationRepository identityVerificationRepository;

        @MockitoBean
        private ProcessedDocumentDataRepository processedDocumentDataRepository;

        @Test
        void testCollectDocumentData() throws Exception {
            final var processId = "testProcessId";
            final var activationId = "testActivationId";
            final var userId = "testUserId";

            final var process = new OnboardingProcessEntity();
            process.setId(processId);
            process.setActivationId(activationId);
            process.setUserId(userId);
            when(onboardingProcessRepository.findById(processId))
                    .thenReturn(Optional.of(process));

            final var identity = new IdentityVerificationEntity();
            identity.setActivationId(activationId);
            when(identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(activationId))
                    .thenReturn(Optional.of(identity));

            final var verification = new DocumentVerificationEntity();
            verification.setId("v1");
            verification.setType(DocumentType.ID_CARD);
            verification.setCountry("CZE");
            verification.setUsedForVerification(true);

            final var documentResult = new DocumentResultEntity();
            documentResult.setExtractedData("{\"foo\":\"bar\"}");
            verification.setResults(Set.of(documentResult));

            identity.setDocumentVerifications(Set.of(verification));

            final var processedData = new ProcessedDocumentDataEntity();
            processedData.setId("pd1");
            processedData.setDocumentVerificationId("v1");
            processedData.setDataType(com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE);
            processedData.setData(new byte[]{1, 2, 3});
            when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any()))
                    .thenReturn(List.of(processedData));

            when(userDataStoreClient.createDocument(any()))
                    .thenReturn(DocumentCreateResponse.builder().build());

            final var results = tested.collectDocumentData(processId);

            assertEquals(1, results.size());
            final DocumentCreateRequest result = results.get(0);
            assertEquals(userId, result.userId());
            assertEquals("personal_id", result.documentType());
            assertEquals(processId, result.externalId());
            assertEquals("{\"foo\":\"bar\"}", result.documentData());
            assertEquals(1, result.photos().size());
            assertEquals("AQID", result.photos().get(0).photoData());
        }
    }

    @SpringBootTest(
            classes = {DefaultUserDataStoreService.class},
            properties = {
                    "enrollment-server-onboarding.user-data-store.enabled=true",
                    "enrollment-server-onboarding.user-data-store.restClientConfig.baseUrl=http://example.com/uds",
                    "enrollment-server-onboarding.user-data-store.document-type=WITH_TRUSTED_IMAGE",
                    "enrollment-server-onboarding.user-data-store.store-extracted-data=true"
            }
    )
    @EnableConfigurationProperties(UserDataStoreConfigProperties.class)
    @Nested
    class TrustedTest {

        @Autowired
        private DefaultUserDataStoreService tested;

        @MockitoBean
        private UserDataStoreClient userDataStoreClient;

        @MockitoBean
        private OnboardingProcessRepository onboardingProcessRepository;

        @MockitoBean
        private IdentityVerificationRepository identityVerificationRepository;

        @MockitoBean
        private ProcessedDocumentDataRepository processedDocumentDataRepository;

        @Test
        void testCollectDocumentData_withTrustedImageOnly() throws Exception {
            final var processId = "testProcessId";
            final var activationId = "testActivationId";
            final var userId = "testUserId";

            final var process = new OnboardingProcessEntity();
            process.setId(processId);
            process.setActivationId(activationId);
            process.setUserId(userId);
            when(onboardingProcessRepository.findById(processId))
                    .thenReturn(Optional.of(process));

            final var identity = new IdentityVerificationEntity();
            identity.setActivationId(activationId);
            when(identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(activationId))
                    .thenReturn(Optional.of(identity));

            final var verification1 = new DocumentVerificationEntity();
            verification1.setId("v1");
            verification1.setType(DocumentType.ID_CARD);
            verification1.setUsedForVerification(false);

            final var verification2 = new DocumentVerificationEntity();
            verification2.setId("v2");
            verification2.setType(DocumentType.PASSPORT);
            verification2.setUsedForVerification(true);
            verification2.setCountry("CZE");
            final var result2 = new DocumentResultEntity();
            result2.setExtractedData("{}");
            verification2.setResults(Set.of(result2));

            identity.setDocumentVerifications(Set.of(verification1, verification2));

            when(userDataStoreClient.createDocument(any()))
                    .thenReturn(DocumentCreateResponse.builder().build());

            final var result = tested.collectDocumentData(processId);

            assertEquals(1, result.size());
            assertEquals("passport", result.get(0).documentType());
        }
    }
}
