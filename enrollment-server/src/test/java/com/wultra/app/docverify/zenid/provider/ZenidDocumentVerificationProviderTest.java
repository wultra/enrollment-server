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
package com.wultra.app.docverify.zenid.provider;

import com.google.common.collect.ImmutableList;
import com.wultra.app.docverify.AbstractDocumentVerificationProviderTest;
import com.wultra.app.docverify.zenid.ZenidConst;
import com.wultra.app.enrollmentserver.EnrollmentServerTestApplication;
import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentResultEntity;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.test.TestUtil;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("external-service")
@ComponentScan(basePackages = {"com.wultra.app.docverify.zenid"})
@EnableConfigurationProperties
@Tag("external-service")
public class ZenidDocumentVerificationProviderTest extends AbstractDocumentVerificationProviderTest {

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
    public void checkDocumentUploadTest() throws Exception {
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
    public void submitDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult result = submitDocuments(ownerId, documents);

        assertSubmittedDocuments(ownerId, documents, result);
    }

    @Test
    public void verifyDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult submitResult = provider.submitDocuments(ownerId, documents);

        List<String> uploadIds = submitResult.getResults().stream()
                .map(DocumentSubmitResult::getUploadId)
                .collect(Collectors.toList());

        DocumentsVerificationResult verificationResult = provider.verifyDocuments(ownerId, uploadIds);

        assertNotNull(verificationResult.getVerificationId());
        assertEquals(uploadIds.size(), verificationResult.getResults().size());
    }

    @Test
    public void getVerificationResultTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult submitResult = provider.submitDocuments(ownerId, documents);

        List<String> uploadIds = submitResult.getResults().stream()
                .map(DocumentSubmitResult::getUploadId)
                .collect(Collectors.toList());

        DocumentsVerificationResult verifyDocumentsResult = provider.verifyDocuments(ownerId, uploadIds);
        Mockito.when(documentVerificationRepository.findAllUploadIds(verifyDocumentsResult.getVerificationId()))
                .thenReturn(uploadIds);

        DocumentsVerificationResult verificationResult = provider.getVerificationResult(ownerId, verifyDocumentsResult.getVerificationId());

        assertEquals(verifyDocumentsResult.getVerificationId(), verificationResult.getVerificationId());
        assertEquals(uploadIds.size(), verificationResult.getResults().size());
    }

    @Test
    public void getPhotoTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        Image photo = provider.getPhoto(result.getExtractedPhotoId());

        assertNotNull(photo.getData());
        assertNotNull(photo.getFilename());
    }

    @Test
    public void cleanupDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        submitDocuments(ownerId, documents);

        cleanupDocuments(ownerId);
    }

    @Test
    public void parseRejectionReasonsTest() throws Exception {
        DocumentResultEntity docResult = new DocumentResultEntity();
        docResult.setVerificationResult("[{\"Ok\": false, \"Issues\":[{\"IssueDescription\": \"Rejection reason\"}]}]");
        List<String> rejectionReasons = provider.parseRejectionReasons(docResult);
        assertEquals(List.of("Rejection reason"), rejectionReasons);
    }

    @Test
    public void initVerificationSdkTest() throws Exception {
        Map<String, String> attributes = Map.of(ZenidConst.SDK_INIT_TOKEN, "sdk-init-token");
        VerificationSdkInfo verificationSdkInfo = provider.initVerificationSdk(ownerId, attributes);
        assertNotNull(verificationSdkInfo.getAttributes().get(ZenidConst.SDK_INIT_RESPONSE), "Missing SDK init response");
    }

    private void cleanupDocuments(OwnerId ownerId) throws Exception {
        if (uploadIds.size() > 0) {
            provider.cleanupDocuments(ownerId, uploadIds);
        }
    }

    private DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> documents) throws Exception {
        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        List<String> uploadIdsFromSubmit = result.getResults().stream()
                .map(DocumentSubmitResult::getUploadId)
                .collect(Collectors.toList());
        uploadIds.addAll(uploadIdsFromSubmit);

        return result;
    }

    private List<SubmittedDocument> createSubmittedDocuments() throws Exception {
        return ImmutableList.of(
                createIdCardFrontDocument(),
                createIdCardBackDocument()
        );
    }

    private SubmittedDocument createIdCardFrontDocument() throws IOException {
        SubmittedDocument idCardFront = new SubmittedDocument();
        idCardFront.setDocumentId(DOC_ID_CARD_FRONT);
        Image idCardFrontPhoto = TestUtil.loadPhoto("/images/specimen_id_front.jpg");
        idCardFront.setPhoto(idCardFrontPhoto);
        idCardFront.setSide(CardSide.FRONT);
        idCardFront.setType(DocumentType.ID_CARD);

        return idCardFront;
    }

    private SubmittedDocument createIdCardBackDocument() throws IOException {
        SubmittedDocument idCardBack = new SubmittedDocument();
        idCardBack.setDocumentId(DOC_ID_CARD_BACK);
        Image idCardBackPhoto = TestUtil.loadPhoto("/images/specimen_id_back.jpg");
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

}
