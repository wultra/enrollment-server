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
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.DataExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class DocumentProcessingServiceTest {

    @MockBean
    DataExtractionService dataExtractionService;

    @Autowired
    DocumentProcessingService tested;

    @Autowired
    DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    IdentityVerificationRepository identityVerificationRepository;

    @Test
    @Sql
    void testPairTwoSidedDocuments() {
        tested.pairTwoSidedDocuments(documentVerificationRepository.findAll());
        assertEquals("2", documentVerificationRepository.findById("1").map(DocumentVerificationEntity::getOtherSideId).get());
        assertEquals("1", documentVerificationRepository.findById("2").map(DocumentVerificationEntity::getOtherSideId).get());
    }

    @Test
    @Sql
    void testSubmitDocuments() throws Exception {
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findById("v1").get();
        assertNotNull(identityVerification);

        final DocumentSubmitRequest.DocumentMetadata page1 = new DocumentSubmitRequest.DocumentMetadata();
        page1.setFilename("id_card_front.png");
        page1.setType(DocumentType.ID_CARD);
        page1.setSide(CardSide.FRONT);

        final DocumentSubmitRequest.DocumentMetadata page2 = new DocumentSubmitRequest.DocumentMetadata();
        page2.setFilename("id_card_back.png");
        page2.setType(DocumentType.ID_CARD);
        page2.setSide(CardSide.BACK);

        final DocumentSubmitRequest request = new DocumentSubmitRequest();
        request.setProcessId("p1");
        request.setResubmit(false);
        request.setData("files".getBytes());
        request.setDocuments(List.of(page1, page2));

        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("a1");
        ownerId.setUserId("u1");

        final Document documentPage1 = new Document();
        documentPage1.setData("img1".getBytes());
        documentPage1.setFilename("id_card_front.png");

        final Document documentPage2 = new Document();
        documentPage2.setData("img2".getBytes());
        documentPage2.setFilename("id_card_back.png");


        when(dataExtractionService.extractDocuments(request.getData())).thenReturn(List.of(documentPage1, documentPage2));

        final SubmittedDocument submittedPage1 = new SubmittedDocument();
        submittedPage1.setDocumentId(page1.getUploadId());
        submittedPage1.setSide(page1.getSide());
        submittedPage1.setType(page1.getType());
        submittedPage1.setPhoto(Image.builder().filename("id_card_front.png").data("img1".getBytes()).build());

        final SubmittedDocument submittedPage2 = new SubmittedDocument();
        submittedPage2.setDocumentId(page2.getUploadId());
        submittedPage2.setSide(page2.getSide());
        submittedPage2.setType(page2.getType());
        submittedPage2.setPhoto(Image.builder().filename("id_card_back.png").data("back".getBytes()).build());

        tested.submitDocuments(identityVerification, request, ownerId);

        List<DocumentVerificationEntity> documents = documentVerificationRepository.findAll();
        assertEquals(2, documents.size());
        assertEquals(2, documents.stream().map(DocumentVerificationEntity::getSide).distinct().toList().size());

    }

}
