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
package com.wultra.app.onboardingserver.impl.service.document;

import com.wultra.app.enrollmentserver.api.model.onboarding.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.DocumentResultRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.DocumentSubmitException;
import com.wultra.app.onboardingserver.impl.service.DataExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@Sql
class DocumentProcessingServiceTest {

    @MockBean
    DataExtractionService dataExtractionService;

    @Autowired
    DocumentProcessingService tested;

    @Autowired
    DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    IdentityVerificationRepository identityVerificationRepository;

    @Autowired
    DocumentResultRepository documentResultRepository;

    @Test
    @Sql
    void testPairTwoSidedDocuments() {
        tested.pairTwoSidedDocuments(documentVerificationRepository.findAll());
        assertEquals("2", documentVerificationRepository.findById("1").map(DocumentVerificationEntity::getOtherSideId).get());
        assertEquals("1", documentVerificationRepository.findById("2").map(DocumentVerificationEntity::getOtherSideId).get());
    }

    @Test
    void testSubmitDocuments() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final List<DocumentSubmitRequest.DocumentMetadata> metadata = createIdCardMetadata();
        final List<Document> data = createIdCardData();
        final OwnerId ownerId = createOwnerId();

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(false);
        request.setData("files".getBytes());
        request.setDocuments(metadata);
        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(data);

        tested.submitDocuments(identityVerification, request, ownerId);

        final List<DocumentVerificationEntity> documents = documentVerificationRepository.findAll();
        assertEquals(2, documents.size());
        assertThat(documents)
                .extracting(DocumentVerificationEntity::getSide)
                .containsExactlyInAnyOrder(CardSide.FRONT, CardSide.BACK);
        assertThat(documents)
                .extracting(DocumentVerificationEntity::getStatus)
                .containsOnly(DocumentStatus.VERIFICATION_PENDING);

        final List<DocumentResultEntity> results = new ArrayList<>();
        documentResultRepository.findAll().forEach(results::add);
        assertEquals(2, results.size());
        assertThat(results)
                .extracting(DocumentResultEntity::getDocumentVerification)
                .extracting(DocumentVerificationEntity::getId)
                .containsExactlyInAnyOrder(documents.stream().map(DocumentVerificationEntity::getId).toArray(String[]::new));
        assertThat(results)
                .extracting(DocumentResultEntity::getPhase)
                .containsOnly(DocumentProcessingPhase.UPLOAD);
    }

    @Test
    void testSubmitDocuments_providerThrows() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final List<DocumentSubmitRequest.DocumentMetadata> metadata = createIdCardMetadata();
        metadata.get(1).setFilename("throw.exception");
        final List<Document> data = createIdCardData();
        data.get(1).setFilename("throw.exception");
        final OwnerId ownerId = createOwnerId();

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(false);
        request.setData("files".getBytes());
        request.setDocuments(metadata);
        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(data);

        tested.submitDocuments(identityVerification, request, ownerId);

        final List<DocumentVerificationEntity> documents = documentVerificationRepository.findAll();
        assertEquals(2, documents.size());
        assertThat(documents)
                .extracting(DocumentVerificationEntity::getSide)
                .containsExactlyInAnyOrder(CardSide.FRONT, CardSide.BACK);
        assertThat(documents)
                .extracting(DocumentVerificationEntity::getStatus)
                .containsOnly(DocumentStatus.FAILED);

        final List<DocumentResultEntity> results = new ArrayList<>();
        documentResultRepository.findAll().forEach(results::add);
        assertEquals(2, results.size());
        assertThat(results)
                .extracting(DocumentResultEntity::getErrorDetail)
                .containsOnly("documentVerificationFailed");
        assertThat(results)
                .extracting(DocumentResultEntity::getErrorOrigin)
                .containsOnly(ErrorOrigin.DOCUMENT_VERIFICATION);
    }

    @Test
    void testSubmitDocuments_missingData() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final List<DocumentSubmitRequest.DocumentMetadata> metadata = createIdCardMetadata();
        final List<Document> data = Collections.emptyList();
        final OwnerId ownerId = createOwnerId();

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(false);
        request.setData("files".getBytes());
        request.setDocuments(metadata);
        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(data);

        tested.submitDocuments(identityVerification, request, ownerId);

        List<DocumentVerificationEntity> documents = documentVerificationRepository.findAll();
        assertEquals(1, documents.size());
        assertThat(documents)
                .extracting(DocumentVerificationEntity::getStatus)
                .containsExactlyInAnyOrder(DocumentStatus.FAILED);
    }

    @Test
    @Sql
    void testResubmitDocuments() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final List<DocumentSubmitRequest.DocumentMetadata> metadata = createIdCardMetadata();
        metadata.get(0).setOriginalDocumentId("original1");
        metadata.get(1).setOriginalDocumentId("original2");
        final List<Document> data = createIdCardData();
        final OwnerId ownerId = createOwnerId();

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(true);
        request.setData("files".getBytes());
        request.setDocuments(metadata);
        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(data);

        tested.submitDocuments(identityVerification, request, ownerId);
        List<DocumentVerificationEntity> documents = documentVerificationRepository.findAll();
        assertEquals(4, documents.size());
        assertThat(documents)
                .extracting(DocumentVerificationEntity::getStatus)
                .containsOnly(DocumentStatus.VERIFICATION_PENDING, DocumentStatus.DISPOSED);
    }

    @Test
    void testResubmitDocuments_missingOriginalDocumentId() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final List<DocumentSubmitRequest.DocumentMetadata> metadata = createIdCardMetadata();
        final List<Document> data = createIdCardData();
        final OwnerId ownerId = createOwnerId();

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(true);
        request.setData("files".getBytes());
        request.setDocuments(metadata);
        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(data);

        final DocumentSubmitException exception = assertThrows(DocumentSubmitException.class,
                () -> tested.submitDocuments(identityVerification, request, ownerId));
        assertEquals("Detected a resubmit request without specified originalDocumentId, %s".formatted(ownerId), exception.getMessage());
    }

    @Test
    void testResubmitDocuments_missingResubmitFlag() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final List<DocumentSubmitRequest.DocumentMetadata> metadata = createIdCardMetadata();
        metadata.get(0).setOriginalDocumentId("original1");
        metadata.get(1).setOriginalDocumentId("original2");
        final List<Document> data = createIdCardData();
        final OwnerId ownerId = createOwnerId();

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(false);
        request.setData("files".getBytes());
        request.setDocuments(metadata);
        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(data);

        final DocumentSubmitException exception = assertThrows(DocumentSubmitException.class,
                () -> tested.submitDocuments(identityVerification, request, ownerId));
        assertEquals("Detected a submit request with specified originalDocumentId=original1, %s".formatted(ownerId), exception.getMessage());
    }

    private List<DocumentSubmitRequest.DocumentMetadata> createIdCardMetadata() {
        final DocumentSubmitRequest.DocumentMetadata page1 = new DocumentSubmitRequest.DocumentMetadata();
        page1.setFilename("id_card_front.png");
        page1.setType(DocumentType.ID_CARD);
        page1.setSide(CardSide.FRONT);

        final DocumentSubmitRequest.DocumentMetadata page2 = new DocumentSubmitRequest.DocumentMetadata();
        page2.setFilename("id_card_back.png");
        page2.setType(DocumentType.ID_CARD);
        page2.setSide(CardSide.BACK);

        return List.of(page1, page2);
    }

    private List<Document> createIdCardData() {
        final Document documentPage1 = new Document();
        documentPage1.setData("img1".getBytes());
        documentPage1.setFilename("id_card_front.png");

        final Document documentPage2 = new Document();
        documentPage2.setData("img2".getBytes());
        documentPage2.setFilename("id_card_back.png");

        return List.of(documentPage1, documentPage2);
    }

    private OwnerId createOwnerId() {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("a1");
        ownerId.setUserId("u1");
        return ownerId;
    }

}
