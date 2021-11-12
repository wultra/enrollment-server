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
import com.wultra.app.enrollmentserver.EnrollmentServerTestApplication;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
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

    private final String DOC_ID_CARD_BACK = "idCardBack";

    private final String DOC_ID_CARD_FRONT = "idCardFront";

    private ZenidDocumentVerificationProvider provider;

    private OwnerId ownerId;

    @Autowired
    public void setProvider(ZenidDocumentVerificationProvider provider) {
        this.provider = provider;
    }

    @BeforeEach
    public void init() {
        ownerId = createOwnerId();
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
    public void submitDocumentsTest() throws Exception {
        List<SubmittedDocument> documents = createSubmittedDocuments();

        DocumentsSubmitResult result = provider.submitDocuments(ownerId, documents);

        assertSubmitDocumentsTest(ownerId, documents, result);
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

        DocumentsVerificationResult verificationResult1 = provider.verifyDocuments(ownerId, uploadIds);

        DocumentsVerificationResult verificationResult2 = provider.getVerificationResult(ownerId, verificationResult1.getVerificationId());

        assertEquals(verificationResult1.getVerificationId(), verificationResult2.getVerificationId());
        assertEquals(uploadIds.size(), verificationResult2.getResults().size());
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

        provider.submitDocuments(ownerId, documents);

        cleanupDocuments(ownerId);
    }

    private void cleanupDocuments(OwnerId ownerId) throws Exception {
        List<String> uploadIds = ImmutableList.of(DOC_ID_CARD_FRONT, DOC_ID_CARD_BACK);

        provider.cleanupDocuments(ownerId, uploadIds);
    }

    private List<SubmittedDocument> createSubmittedDocuments() throws Exception {
        SubmittedDocument idCardFront = new SubmittedDocument();
        idCardFront.setDocumentId(DOC_ID_CARD_FRONT);
        Image idCardFrontPhoto = TestUtil.loadPhoto("/images/specimen_id_front.jpg");
        idCardFront.setPhoto(idCardFrontPhoto);
        idCardFront.setSide(CardSide.FRONT);
        idCardFront.setType(DocumentType.ID_CARD);

        SubmittedDocument idCardBack = new SubmittedDocument();
        idCardBack.setDocumentId(DOC_ID_CARD_BACK);
        Image idCardBackPhoto = TestUtil.loadPhoto("/images/specimen_id_back.jpg");
        idCardBack.setPhoto(idCardBackPhoto);
        idCardBack.setSide(CardSide.BACK);
        idCardBack.setType(DocumentType.ID_CARD);

        return ImmutableList.of(idCardFront, idCardBack);
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("integration-test-" + UUID.randomUUID());
        ownerId.setUserId("integration-test-user-id");
        return ownerId;
    }

}