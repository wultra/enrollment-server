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

import com.google.common.base.Ascii;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wultra.app.enrollmentserver.database.entity.DocumentResultEntity;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.provider.DocumentVerificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mock implementation of the {@link DocumentVerificationProvider}
 */
@ConditionalOnProperty(value = "enrollment-server.document-verification.provider", havingValue = "mock")
@Component
public class WultraMockDocumentVerificationProvider implements DocumentVerificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(WultraMockDocumentVerificationProvider.class);

    private static final List<DocumentType> DOCUMENT_TYPES_WITH_EXTRACTED_PHOTO =
            List.of(DocumentType.DRIVING_LICENSE, DocumentType.ID_CARD, DocumentType.PASSPORT);

    private final Cache<String, List<String>> verificationUploadIds;

    public WultraMockDocumentVerificationProvider() {
        logger.warn("Using mocked version of {}", DocumentVerificationProvider.class.getName());

        verificationUploadIds = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) throws DocumentVerificationException {
        DocumentsSubmitResult result = new DocumentsSubmitResult();
        if (DOCUMENT_TYPES_WITH_EXTRACTED_PHOTO.contains(document.getType())) {
            // set extracted photo id only on a relevant documents submit
            result.setExtractedPhotoId("extracted-photo-id");
        }
        result.setResults(List.of(toDocumentSubmitResult(document.getUploadId())));

        logger.info("Mock - check document upload, {}", id);
        return result;
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws DocumentVerificationException {
        List<DocumentSubmitResult> submitResults = documents.stream()
                .map(doc -> toDocumentSubmitResult(doc.getDocumentId()))
                .collect(Collectors.toList());;

        DocumentsSubmitResult result = new DocumentsSubmitResult();
        if (documents.stream().anyMatch(doc -> DOCUMENT_TYPES_WITH_EXTRACTED_PHOTO.contains(doc.getType()))) {
            // set extracted photo id only on a relevant documents submit
            result.setExtractedPhotoId("extracted-photo-id");
        }
        result.setResults(submitResults);

        logger.info("Mock - submitted documents, {}", id);
        return result;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws DocumentVerificationException {
        String verificationId = UUID.randomUUID().toString();

        DocumentsVerificationResult result = new DocumentsVerificationResult();
        result.setStatus(DocumentVerificationStatus.IN_PROGRESS);
        result.setVerificationId(verificationId);

        verificationUploadIds.put(verificationId, uploadIds);

        logger.info("Mock - verifying documents uploadIds={}, {}", uploadIds, id);
        return result;
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws DocumentVerificationException {
        DocumentsVerificationResult result = new DocumentsVerificationResult();
        List<String> uploadIds = verificationUploadIds.getIfPresent(verificationId);
        if (uploadIds == null) {
            result.setStatus(DocumentVerificationStatus.FAILED);
            result.setErrorDetail("not existing verificationId: " + verificationId);
        } else {
            List<DocumentVerificationResult> verificationResults = uploadIds.stream()
                    .map(uploadId -> {
                        DocumentVerificationResult verificationResult = new DocumentVerificationResult();
                        verificationResult.setExtractedData("{\"extracted\": \"data-" + uploadId + "\"}");
                        verificationResult.setUploadId(uploadId);
                        verificationResult.setVerificationResult("{\"verificationResult\": \"data-" + uploadId + "\"}");
                        return verificationResult;
                    })
                    .collect(Collectors.toList());

            result.setResults(verificationResults);
            result.setStatus(DocumentVerificationStatus.ACCEPTED);
            result.setVerificationId(verificationId);
        }

        logger.info("Mock - getting verification result verificationId={}, {}", verificationId, id);
        return result;
    }

    @Override
    public Image getPhoto(String photoId) throws DocumentVerificationException {
        Image photo = new Image();
        photo.setData(new byte[]{});
        photo.setFilename("id_photo.jpg");

        logger.info("Mock - getting photoId={} from document verification", photoId);
        return photo;
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) throws DocumentVerificationException {
        logger.info("Mock - cleaned up documents uploadIds={}, {}", uploadIds, id);
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        if (docResult.getVerificationResult().contains("rejected")) {
            return List.of("Rejection reason");
        } else {
            return Collections.emptyList();
        }
    }

    private DocumentSubmitResult toDocumentSubmitResult(String docId) {
        if (docId == null) {
            // document from the submit request has no documentId, generate one
            docId = UUID.randomUUID().toString();
        }
        DocumentSubmitResult submitResult = new DocumentSubmitResult();
        submitResult.setDocumentId(docId);
        submitResult.setExtractedData("{\"extracted\": { \"data\": \"" + docId + "\" } }");
        String uploadedDocId;
        if (docId.startsWith("upload")) {
            uploadedDocId = docId;
        } else {
            uploadedDocId = Ascii.truncate("uploaded-" + docId, 36, "...");
        }
        submitResult.setUploadId(uploadedDocId);
        submitResult.setValidationResult("{\"validationResult\": { \"data\": \"" + docId + "\" } }");
        return submitResult;
    }

}
