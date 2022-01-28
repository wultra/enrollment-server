/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.docverify.mock.provider;

import com.google.common.collect.ImmutableList;
import com.wultra.app.docverify.AbstractDocumentVerificationProviderTest;
import com.wultra.app.enrollmentserver.EnrollmentServerTestApplication;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("mock")
@ComponentScan(basePackages = {"com.wultra.app.docverify.mock"})
@EnableConfigurationProperties
public class WultraMockDocumentVerificationProviderTest extends AbstractDocumentVerificationProviderTest {

    private WultraMockDocumentVerificationProvider provider;

    private OwnerId ownerId;

    @BeforeEach
    public void init() {
        ownerId = createOwnerId();
    }

    @Autowired
    public void setProvider(WultraMockDocumentVerificationProvider provider) {
        this.provider = provider;
    }

    @Test
    public void checkDocumentUploadTest() throws Exception {
        SubmittedDocument document = createSubmittedDocument();

        DocumentsSubmitResult result = provider.checkDocumentUpload(ownerId, document);

        assertSubmittedDocuments(ownerId, List.of(document), result);
    }

    @Test
    public void submitDocumentsTest() throws Exception {
        SubmittedDocument document = createSubmittedDocument();
        List<SubmittedDocument> documents = ImmutableList.of(document);

        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        assertSubmittedDocuments(ownerId, documents, result);
    }

    @Test
    public void verifyDocumentsTest() throws Exception {
        List<String> uploadIds = ImmutableList.of("doc_1", "doc_2");

        DocumentsVerificationResult result = provider.verifyDocuments(ownerId, uploadIds);
        assertEquals(DocumentVerificationStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getVerificationId());
    }

    @Test
    public void getVerificationResultTest() throws Exception {
        List<String> uploadIds = ImmutableList.of("doc_1", "doc_2");

        DocumentsVerificationResult result = provider.verifyDocuments(ownerId, uploadIds);

        // Check status of an existing verification
        DocumentsVerificationResult verificationResults = provider.getVerificationResult(ownerId, result.getVerificationId());
        assertTrue(verificationResults.isAccepted());
        assertEquals(DocumentVerificationStatus.ACCEPTED, verificationResults.getStatus());

        List<DocumentVerificationResult> documentResults = verificationResults.getResults();
        assertEquals(uploadIds.size(), documentResults.size());
        documentResults.forEach(documentResult -> {
            assertTrue(uploadIds.contains(documentResult.getUploadId()));
            assertNotNull(documentResult.getExtractedData());
            assertNotNull(documentResult.getVerificationResult());
        });

        // Check status of a not existing verification
        DocumentsVerificationResult verificationResultNotExisting = provider.getVerificationResult(ownerId, "notExisting");
        assertEquals(DocumentVerificationStatus.FAILED, verificationResultNotExisting.getStatus());
        assertNotNull(verificationResultNotExisting.getErrorDetail());
    }

    @Test
    public void getPhotoTest() throws Exception {
        Image photo = provider.getPhoto("photoId");

        assertNotNull(photo.getData());
        assertNotNull(photo.getFilename());
    }

    @Test
    public void cleanupDocumentsTest() throws Exception {
        List<String> uploadIds = ImmutableList.of("doc_1", "doc_2");

        provider.cleanupDocuments(ownerId, uploadIds);
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("activation-id");
        ownerId.setUserId("user-id");
        return ownerId;
    }

    private SubmittedDocument createSubmittedDocument() {
        SubmittedDocument document = new SubmittedDocument();
        document.setDocumentId("documentId");
        document.setType(DocumentType.ID_CARD);
        document.setSide(CardSide.FRONT);

        return document;
    }

}
