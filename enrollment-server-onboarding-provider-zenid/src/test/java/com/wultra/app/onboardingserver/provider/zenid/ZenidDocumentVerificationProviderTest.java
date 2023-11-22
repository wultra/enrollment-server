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
package com.wultra.app.onboardingserver.provider.zenid;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("external-service")
@ComponentScan(basePackages = {"com.wultra.app.onboardingserver.docverify.zenid"})
@EnableConfigurationProperties
@Tag("external-service")
class ZenidDocumentVerificationProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(ZenidDocumentVerificationProviderTest.class);

    private static final String DOC_ID_CARD_BACK = "idCardBack";

    private static final String DOC_ID_CARD_FRONT = "idCardFront";

    private List<String> uploadIds;

    private OwnerId ownerId;

    @MockBean
    private DocumentVerificationRepository documentVerificationRepository;

    @Autowired
    private ZenidDocumentVerificationProvider provider;

    @BeforeEach
    public void init() {
        ownerId = createOwnerId();
        uploadIds = new ArrayList<>();
    }

    @AfterEach
    public void teardown() {
        try {
            cleanupDocuments(ownerId);
        } catch (Exception e) {
            logger.warn("Unable to cleanup documents during teardown", e);
        }
    }

    @Test
    void checkDocumentUploadTest() throws Exception {
        SubmittedDocument document = createIdCardFrontDocument();
        List<SubmittedDocument> documents = List.of(document);

        DocumentsSubmitResult docsSubmitResult = submitDocuments(ownerId, documents);
        DocumentSubmitResult docSubmitResult = docsSubmitResult.getResults().get(0);
        DocumentVerificationEntity docVerification = new DocumentVerificationEntity();
        docVerification.setType(document.getType());
        docVerification.setUploadId(docSubmitResult.getUploadId());
        DocumentsSubmitResult result = provider.checkDocumentUpload(ownerId, docVerification);

        assertEquals(1, result.getResults().size());
        assertEquals(docVerification.getUploadId(), result.getResults().get(0).getUploadId());
    }

    @Test
    void submitDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult result = submitDocuments(ownerId, documents);

        assertSubmittedDocuments(ownerId, documents, result);
    }

    @Test
    void verifyDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult submitResult = provider.submitDocuments(ownerId, documents);

        final List<String> uploadIds = submitResult.getResults().stream()
                .map(DocumentSubmitResult::getUploadId)
                .toList();

        DocumentsVerificationResult verificationResult = provider.verifyDocuments(ownerId, uploadIds);

        assertNotNull(verificationResult.getVerificationId());
        assertEquals(uploadIds.size(), verificationResult.getResults().size());
    }

    @Test
    void getVerificationResultTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult submitResult = provider.submitDocuments(ownerId, documents);

        final List<String> uploadIds = submitResult.getResults().stream()
                .map(DocumentSubmitResult::getUploadId)
                .toList();

        DocumentsVerificationResult verifyDocumentsResult = provider.verifyDocuments(ownerId, uploadIds);
        Mockito.when(documentVerificationRepository.findAllUploadIds(verifyDocumentsResult.getVerificationId()))
                .thenReturn(uploadIds);

        DocumentsVerificationResult verificationResult = provider.getVerificationResult(ownerId, verifyDocumentsResult.getVerificationId());

        assertEquals(verifyDocumentsResult.getVerificationId(), verificationResult.getVerificationId());
        assertEquals(uploadIds.size(), verificationResult.getResults().size());
    }

    @Test
    void getPhotoTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        Image photo = provider.getPhoto(result.getExtractedPhotoId());

        assertNotNull(photo.getData());
        assertNotNull(photo.getFilename());
    }

    @Test
    void cleanupDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        submitDocuments(ownerId, documents);

        cleanupDocuments(ownerId);
    }

    @Test
    void parseRejectionReasonsTest() throws Exception {
        DocumentResultEntity docResult = new DocumentResultEntity();
        docResult.setVerificationResult("[{\"Ok\": false, \"Issues\":[{\"IssueDescription\": \"Rejection reason\"}]}]");
        List<String> rejectionReasons = provider.parseRejectionReasons(docResult);
        assertEquals(List.of("Rejection reason"), rejectionReasons);
    }

    @Test
    void initVerificationSdkTest() throws Exception {
        Map<String, String> attributes = Map.of("sdk-init-token", UUID.randomUUID().toString());
        VerificationSdkInfo verificationSdkInfo = provider.initVerificationSdk(ownerId, attributes);
        assertNotNull(verificationSdkInfo.getAttributes().get("zenid-sdk-init-response"), "Missing SDK init response");
    }

    private void cleanupDocuments(OwnerId ownerId) throws Exception {
        if (!uploadIds.isEmpty()) {
            provider.cleanupDocuments(ownerId, uploadIds);
        }
    }

    private DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> documents) throws Exception {
        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        final List<String> uploadIdsFromSubmit = result.getResults().stream()
                .map(DocumentSubmitResult::getUploadId)
                .toList();
        uploadIds.addAll(uploadIdsFromSubmit);

        return result;
    }

    private List<SubmittedDocument> createSubmittedDocuments() throws Exception {
        return List.of(
                createIdCardFrontDocument(),
                createIdCardBackDocument()
        );
    }

    private SubmittedDocument createIdCardFrontDocument() throws IOException {
        SubmittedDocument idCardFront = new SubmittedDocument();
        idCardFront.setDocumentId(DOC_ID_CARD_FRONT);
        Image idCardFrontPhoto = loadPhoto("/images/specimen_id_front.jpg");
        idCardFront.setPhoto(idCardFrontPhoto);
        idCardFront.setSide(CardSide.FRONT);
        idCardFront.setType(DocumentType.ID_CARD);

        return idCardFront;
    }

    private SubmittedDocument createIdCardBackDocument() throws IOException {
        SubmittedDocument idCardBack = new SubmittedDocument();
        idCardBack.setDocumentId(DOC_ID_CARD_BACK);
        Image idCardBackPhoto = loadPhoto("/images/specimen_id_back.jpg");
        idCardBack.setPhoto(idCardBackPhoto);
        idCardBack.setSide(CardSide.BACK);
        idCardBack.setType(DocumentType.ID_CARD);

        return idCardBack;
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("integration-test-" + UUID.randomUUID());
        ownerId.setUserId("integration-test-user-id");
        return ownerId;
    }

    private static Image loadPhoto(final String path) throws IOException {
        final File file = new File(path);

        return Image.builder()
                .data(readImageData(path))
                .filename(file.getName())
                .build();
    }

    private static byte[] readImageData(final String path) throws IOException {
        try (InputStream stream = ZenidDocumentVerificationProviderTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Unable to get a stream for: " + path);
            }
            return stream.readAllBytes();
        }
    }

    private static void assertSubmittedDocuments(OwnerId ownerId, List<SubmittedDocument> documents, DocumentsSubmitResult result) {
        assertEquals(documents.size(), result.getResults().size(), "Different size of submitted documents than expected");
        assertNotNull(result.getExtractedPhotoId(), "Missing extracted photoId");

        final List<String> submittedDocsIds = result.getResults().stream()
                .map(DocumentSubmitResult::getDocumentId)
                .toList();
        assertEquals(documents.size(), submittedDocsIds.size(), "Different size of unique submitted documents than expected");
        documents.forEach(document ->
                assertTrue(submittedDocsIds.contains(document.getDocumentId())));

        result.getResults().forEach(submitResult -> {
            assertNull(submitResult.getErrorDetail());
            assertNull(submitResult.getRejectReason());

            assertNotNull(submitResult.getUploadId());
        });
    }

}
