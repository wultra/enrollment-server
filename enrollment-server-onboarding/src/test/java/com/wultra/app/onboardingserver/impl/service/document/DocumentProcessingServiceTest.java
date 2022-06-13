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

import com.wultra.app.onboardingserver.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
class DocumentProcessingServiceTest {

    @InjectMocks
    DocumentProcessingService service;

    @Mock
    DocumentVerificationRepository documentVerificationRepository;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void pairTwoSidedDocumentsTest() {
        DocumentVerificationEntity docIdCardFront = new DocumentVerificationEntity();
        docIdCardFront.setId("1");
        docIdCardFront.setType(DocumentType.ID_CARD);
        docIdCardFront.setSide(CardSide.FRONT);

        DocumentVerificationEntity docIdCardBack = new DocumentVerificationEntity();
        docIdCardBack.setId("2");
        docIdCardBack.setType(DocumentType.ID_CARD);
        docIdCardBack.setSide(CardSide.BACK);

        List<DocumentVerificationEntity> documents = List.of(docIdCardFront, docIdCardBack);

        service.pairTwoSidedDocuments(documents);
        when(documentVerificationRepository.setOtherDocumentSide("1", "2")).thenReturn(1);
        verify(documentVerificationRepository, times(1)).setOtherDocumentSide("1", "2");
        when(documentVerificationRepository.setOtherDocumentSide("2", "1")).thenReturn(1);
        verify(documentVerificationRepository, times(1)).setOtherDocumentSide("2", "1");
        assertEquals(docIdCardBack.getId(), docIdCardFront.getOtherSideId());
        assertEquals(docIdCardFront.getId(), docIdCardBack.getOtherSideId());
    }

}
