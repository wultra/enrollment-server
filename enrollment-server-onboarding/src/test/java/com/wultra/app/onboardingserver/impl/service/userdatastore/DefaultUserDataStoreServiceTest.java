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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.security.userdatastore.client.UserDataStoreClient;
import com.wultra.security.userdatastore.client.model.error.UserDataStoreClientException;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.response.DocumentCreateResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link DefaultUserDataStoreService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class DefaultUserDataStoreServiceTest {

    @SpringBootTest(
            classes = {DefaultUserDataStoreService.class, JacksonAutoConfiguration.class},
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

        @MockitoBean
        private DocumentVerificationRepository documentVerificationRepository;

        @Test
        void testStoreData() throws Exception {
            final DocumentCreateRequest request = DocumentCreateRequest.builder().build();

            when(userDataStoreClient.createDocument(any()))
                    .thenThrow(new UserDataStoreClientException("error 1"))
                    .thenThrow(new UserDataStoreClientException("error 2"))
                    .thenReturn(DocumentCreateResponse.builder().id("doc-id").build());

            tested.storeDocumentData(List.of(request));

            verify(userDataStoreClient, times(3)).createDocument(any());
        }

        @Test
        void testStoreData_failed() throws Exception {
            final DocumentCreateRequest request = DocumentCreateRequest.builder().build();

            when(userDataStoreClient.createDocument(any()))
                    .thenThrow(new UserDataStoreClientException("error 1"))
                    .thenThrow(new UserDataStoreClientException("error 2"))
                    .thenThrow(new UserDataStoreClientException("error 3"));

            final var result = assertThrows(UserDataStoreClientException.class, () -> tested.storeDocumentData(List.of(request)));

            assertEquals("error 3", result.getMessage());
            verify(userDataStoreClient, times(3)).createDocument(any());
        }

        @Test
        void testCollectDocumentData() {
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

            final var documentResult = new DocumentResultEntity();
            documentResult.setExtractedData("{\"givenNames\":\"John\",\"surname\":\"Doe\"}");

            final var documentVerification = new DocumentVerificationEntity();
            documentVerification.setId("v1");
            documentVerification.setType(DocumentType.ID_CARD);
            documentVerification.setStatus(DocumentStatus.ACCEPTED);
            documentVerification.setCountry("CZE");
            documentVerification.setUsedForVerification(true);
            documentVerification.setResults(Set.of(documentResult));

            when(documentVerificationRepository.findAcceptedWithPhoto(identity))
                    .thenReturn(List.of(documentVerification));

            final var processedData = new ProcessedDocumentDataEntity();
            processedData.setId("pd1");
            processedData.setDocumentVerificationId("v1");
            processedData.setDataType(ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE);
            processedData.setData(new byte[]{1, 2, 3});
            processedData.setTimestampCreated(new Date());
            when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any()))
                    .thenReturn(List.of(processedData));

            final var results = tested.collectDocumentData(processId);

            assertEquals(1, results.size());
            final DocumentCreateRequest result = results.get(0);
            assertEquals(userId, result.userId());
            assertEquals("personal_id", result.documentType());
            assertEquals(processId, result.externalId());
            assertTrue(result.documentData().contains("\"givenNames\":\"John\""), "documentData: " + result.documentData());
            assertTrue(result.documentData().contains("\"surname\":\"Doe\""), "documentData: " + result.documentData());
            assertEquals(Boolean.TRUE, result.attributes().get("trustedImage"));
            assertEquals(1, result.photos().size());
            assertEquals("AQID", result.photos().get(0).photoData());
            assertEquals("pd1", result.photos().get(0).externalId());
        }

        @Test
        void testCollectDocumentData_returnsAllPhotos() {
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

            final var documentVerification = new DocumentVerificationEntity();
            documentVerification.setId("v1");
            documentVerification.setStatus(DocumentStatus.ACCEPTED);
            documentVerification.setType(DocumentType.ID_CARD);
            documentVerification.setUsedForVerification(true);
            documentVerification.setResults(Set.of(new DocumentResultEntity()));

            when(documentVerificationRepository.findAcceptedWithPhoto(identity))
                    .thenReturn(List.of(documentVerification));

            final var photoType = ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE;

            final var photoOld = new ProcessedDocumentDataEntity();
            photoOld.setId("old");
            photoOld.setDocumentVerificationId("v1");
            photoOld.setDataType(photoType);
            photoOld.setData(new byte[]{0});
            photoOld.setTimestampCreated(new Date(1000));

            final var photoNew = new ProcessedDocumentDataEntity();
            photoNew.setId("new");
            photoNew.setDocumentVerificationId("v1");
            photoNew.setDataType(photoType);
            photoNew.setData(new byte[]{1});
            photoNew.setTimestampCreated(new Date(2000));

            when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any()))
                    .thenReturn(List.of(photoOld, photoNew));

            final var results = tested.collectDocumentData(processId);

            assertEquals(1, results.size());
            assertEquals(2, results.get(0).photos().size());
        }

        @Test
        void testCollectDocumentData_multipleDocumentTypes() {
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

            final var idCardResult = new DocumentResultEntity();
            idCardResult.setExtractedData("{\"givenNames\":\"John\"}");
            final var idCard = new DocumentVerificationEntity();
            idCard.setId("v1");
            idCard.setType(DocumentType.ID_CARD);
            idCard.setStatus(DocumentStatus.ACCEPTED);
            idCard.setUsedForVerification(true);
            idCard.setResults(Set.of(idCardResult));

            final var drivingLicenseResult = new DocumentResultEntity();
            drivingLicenseResult.setExtractedData("{\"documentNumber\":\"DL-42\"}");
            final var drivingLicense = new DocumentVerificationEntity();
            drivingLicense.setId("v2");
            drivingLicense.setType(DocumentType.DRIVING_LICENSE);
            drivingLicense.setStatus(DocumentStatus.ACCEPTED);
            drivingLicense.setUsedForVerification(true);
            drivingLicense.setResults(Set.of(drivingLicenseResult));

            when(documentVerificationRepository.findAcceptedWithPhoto(identity))
                    .thenReturn(List.of(idCard, drivingLicense));

            when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any()))
                    .thenReturn(List.of());

            final var results = tested.collectDocumentData(processId);

            assertEquals(2, results.size());

            final var primary = results.stream()
                    .filter(it -> "personal_id".equals(it.documentType()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(Boolean.TRUE, primary.attributes().get("trustedImage"));
            assertTrue(primary.documentData().contains("\"givenNames\":\"John\""), "primary documentData: " + primary.documentData());

            final var other = results.stream()
                    .filter(it -> "drivers_license".equals(it.documentType()))
                    .findFirst()
                    .orElseThrow();
            assertNull(other.attributes(), "other.attributes: " + other.attributes());
            assertTrue(other.documentData().contains("\"documentNumber\":\"DL-42\""), "other documentData: " + other.documentData());
        }

        @Test
        void testCollectDocumentData_mergesExtractedDataFromFrontAndBackSides() {
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

            // ID card with front side data: given names + surname
            final var frontResult = new DocumentResultEntity();
            frontResult.setExtractedData("{\"givenNames\":\"John\",\"surname\":\"Doe\"}");
            final var frontSide = new DocumentVerificationEntity();
            frontSide.setId("v-front");
            frontSide.setType(DocumentType.ID_CARD);
            frontSide.setStatus(DocumentStatus.ACCEPTED);
            frontSide.setUsedForVerification(true);
            frontSide.setResults(Set.of(frontResult));

            // ID card with back side data: personal number + authority
            final var backResult = new DocumentResultEntity();
            backResult.setExtractedData("{\"personalNumber\":\"900101/1234\",\"authority\":\"MV CR\"}");
            final var backSide = new DocumentVerificationEntity();
            backSide.setId("v-back");
            backSide.setType(DocumentType.ID_CARD);
            backSide.setStatus(DocumentStatus.ACCEPTED);
            backSide.setUsedForVerification(true);
            backSide.setResults(Set.of(backResult));

            when(documentVerificationRepository.findAcceptedWithPhoto(identity))
                    .thenReturn(List.of(frontSide, backSide));

            when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any()))
                    .thenReturn(List.of());

            final var results = tested.collectDocumentData(processId);

            assertEquals(1, results.size());
            final var documentData = results.get(0).documentData();
            assertTrue(documentData.contains("\"givenNames\":\"John\""), "documentData: " + documentData);
            assertTrue(documentData.contains("\"surname\":\"Doe\""), "documentData: " + documentData);
            assertTrue(documentData.contains("\"personalNumber\":\"900101/1234\""), "documentData: " + documentData);
            assertTrue(documentData.contains("\"authority\":\"MV CR\""), "documentData: " + documentData);
        }

        @Test
        void testCollectDocumentData_processNotFound() {
            when(onboardingProcessRepository.findById("missing"))
                    .thenReturn(Optional.empty());

            final var results = tested.collectDocumentData("missing");

            assertEquals(0, results.size());
            verifyNoInteractions(documentVerificationRepository);
        }

        @Test
        void testCollectDocumentData_noDocumentVerification() {
            final var processId = "testProcessId";
            final var activationId = "testActivationId";

            final var process = new OnboardingProcessEntity();
            process.setId(processId);
            process.setActivationId(activationId);
            process.setUserId("testUserId");
            when(onboardingProcessRepository.findById(processId))
                    .thenReturn(Optional.of(process));

            final var identity = new IdentityVerificationEntity();
            identity.setActivationId(activationId);
            when(identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(activationId))
                    .thenReturn(Optional.of(identity));

            when(documentVerificationRepository.findAcceptedWithPhoto(identity))
                    .thenReturn(List.of());

            final var results = tested.collectDocumentData(processId);

            assertEquals(0, results.size());
            verifyNoInteractions(processedDocumentDataRepository);
        }
    }

    @SpringBootTest(
            classes = {DefaultUserDataStoreService.class, JacksonAutoConfiguration.class},
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

        @MockitoBean
        private DocumentVerificationRepository documentVerificationRepository;

        @Test
        void testCollectDocumentData_withTrustedImageOnly() {
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

            // The repository query already filters by usedForVerification = true,
            // so only the trusted document is returned.
            final var documentResult = new DocumentResultEntity();
            documentResult.setExtractedData("{}");

            final var documentVerification = new DocumentVerificationEntity();
            documentVerification.setId("v2");
            documentVerification.setStatus(DocumentStatus.ACCEPTED);
            documentVerification.setType(DocumentType.PASSPORT);
            documentVerification.setUsedForVerification(true);
            documentVerification.setCountry("CZE");
            documentVerification.setResults(Set.of(documentResult));

            when(documentVerificationRepository.findAcceptedWithPhoto(identity))
                    .thenReturn(List.of(documentVerification));

            final var result = tested.collectDocumentData(processId);

            assertEquals(1, result.size());
            assertEquals("passport", result.get(0).documentType());
            assertEquals(Boolean.TRUE, result.get(0).attributes().get("trustedImage"));
        }
    }
}
