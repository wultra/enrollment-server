/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.app.docverify.mock.provider;

import com.google.common.collect.ImmutableList;
import com.wultra.app.docverify.AbstractDocumentVerificationProviderTest;
import com.wultra.app.enrollmentserver.EnrollmentServerTestApplication;
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
    public void submitDocumentsTest() throws Exception {
        SubmittedDocument document = new SubmittedDocument();
        document.setDocumentId("documentId");

        List<SubmittedDocument> documents = ImmutableList.of(document);

        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        assertSubmitDocumentsTest(ownerId, documents, result);
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

}
