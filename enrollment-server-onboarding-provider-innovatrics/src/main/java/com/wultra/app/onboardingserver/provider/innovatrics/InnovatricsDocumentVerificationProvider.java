/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.innovatrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.innovatrics.com/">Innovatrics</a>.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
        """)
@Component
@AllArgsConstructor
@Slf4j
public class InnovatricsDocumentVerificationProvider implements DocumentVerificationProvider {

    private final InnovatricsApiService innovatricsApiService;
    private final ObjectMapper objectMapper;
    private static final String REJECTION_REASON_DELIMITER = "#";

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException("Method checkDocumentUpload is not supported by Innovatrics provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws RemoteCommunicationException, DocumentVerificationException {
        if (CollectionUtils.isEmpty(documents)) {
            logger.info("Empty documents list passed to document provider.");
            return new DocumentsSubmitResult();
        }

        final DocumentType documentType = documents.get(0).getType();
        if (DocumentType.SELFIE_PHOTO.equals(documentType)) {
            throw new DocumentVerificationException("Selfie photo cannot be submitted as a document");
        }

        if (DocumentType.SELFIE_VIDEO.equals(documentType)) {
            throw new DocumentVerificationException("Selfie video cannot be submitted as a document");
        }

        final String customerId = createCustomer();
        createDocument(customerId, documentType);
        logger.debug("Created new customer, customerId={}", customerId);

        final DocumentsSubmitResult results = new DocumentsSubmitResult();
        for (SubmittedDocument page : documents) {
            final CreateDocumentPageResponse createDocumentPageResponse = provideDocumentPage(customerId, page);
            if (containsError(createDocumentPageResponse)) {
                logger.debug("Document page (documentId={}) was not read successfully", page.getDocumentId());
                results.getResults().add(createErrorSubmitResult(customerId, createDocumentPageResponse));
            } else {
                logger.debug("Document page (documentId={}) was read successfully", page.getDocumentId());
                results.getResults().add(createSubmitResult(customerId));
                if (!StringUtils.hasText(results.getExtractedPhotoId()) && hasDocumentPortrait(customerId)) {
                    results.setExtractedPhotoId(customerId);
                }
            }
        }

        return results;
    }

    @Override
    public boolean shouldStoreSelfie() {
        return false;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        final DocumentsVerificationResult results = new DocumentsVerificationResult();
        // TODO - verificationId, status

        for (String customerId : uploadIds) {
            final DocumentVerificationResult result = verifyDocument(customerId);
            results.getResults().add(result);
        }

        return results;
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException("Method getVerificationResult is not supported by Innovatrics provider.");
    }

    @Override
    public Image getPhoto(String photoId) throws RemoteCommunicationException, DocumentVerificationException {
        return Image.builder()
                .data(innovatricsApiService.getDocumentPortrait(photoId)
                        .map(ImageCrop::getData)
                        .orElseThrow(() -> new RemoteCommunicationException("Document portrait not available for customer %s".formatted(photoId))))
                .filename("document_portrait_%s.jpg".formatted(photoId))
                .build();
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        for (String customerId : uploadIds) {
            innovatricsApiService.deleteCustomer(customerId);
        }
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        final String reasons = docResult.getRejectReason();
        if (!StringUtils.hasText(reasons)) {
            return new ArrayList<>();
        }

        return Arrays.asList(docResult.getRejectReason().split(REJECTION_REASON_DELIMITER));
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) throws RemoteCommunicationException, DocumentVerificationException {
        return new VerificationSdkInfo();
    }

    /**
     * Create a new customer resource.
     * @return ID of the new customer.
     * @throws RemoteCommunicationException if the resource was not created properly.
     */
    private String createCustomer() throws RemoteCommunicationException {
        return innovatricsApiService.createCustomer()
                .map(CreateCustomerResponse::getId)
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new RemoteCommunicationException("A customer resource could not be created."));
    }

    /**
     * Create a new document resource to an existing customer.
     * @param customerId id of the customer to assign the resource to.
     * @param documentType type of the document that will be uploaded later.
     * @throws RemoteCommunicationException if the resource was not created properly.
     */
    private void createDocument(String customerId, DocumentType documentType) throws RemoteCommunicationException {
            innovatricsApiService.createDocument(customerId, documentType)
                    .map(CreateDocumentResponse::getLinks)
                    .map(Links::getSelf)
                    .filter(StringUtils::hasText)
                    .orElseThrow(() -> new RemoteCommunicationException("A document resource could not be created for customer %s".formatted(customerId)));
    }

    /**
     * Upload a page of a document to a customer.
     * @param customerId id of the customer to whom upload the document page.
     * @param page SubmittedDocument object representing the page.
     * @return CreateDocumentPageResponse containing info about the document type. An unsuccessful response will contain an error code.
     * @throws RemoteCommunicationException if the document page was not uploaded properly.
     */
    private CreateDocumentPageResponse provideDocumentPage(String customerId, SubmittedDocument page) throws RemoteCommunicationException {
            return innovatricsApiService.provideDocumentPage(customerId, page.getSide(), page.getPhoto().getData())
                    .orElseThrow(() -> new RemoteCommunicationException("Document page was not uploaded for customer %s.".formatted(customerId)));
    }

    /**
     * Checks if CreateDocumentPageResponse contains error or warnings.
     * @param pageResponse response to a page upload.
     * @return true if there is an error or warnings, false otherwise.
     */
    private static boolean containsError(CreateDocumentPageResponse pageResponse) {
        return pageResponse.getErrorCode() != null || !CollectionUtils.isEmpty(pageResponse.getWarnings());
    }

    /**
     * Creates DocumentSubmitResult with error or reject reason.
     * @param uploadId external id of the document.
     * @param response returned from provider.
     * @return DocumentSubmitResult with error or reject reason.
     */
    private static DocumentSubmitResult createErrorSubmitResult(String uploadId, CreateDocumentPageResponse response) {
        final DocumentSubmitResult result = new DocumentSubmitResult();
        result.setUploadId(uploadId);
        result.setExtractedData(DocumentSubmitResult.NO_DATA_EXTRACTED);

        if (response.getErrorCode() != null) {
            switch (response.getErrorCode()) {
                case NO_CARD_CORNERS_DETECTED -> result.setRejectReason("Document page was not detected in the photo.");
                case PAGE_DOESNT_MATCH_DOCUMENT_TYPE_OF_PREVIOUS_PAGE -> result.setRejectReason("Mismatched document pages types.");
                default -> result.setRejectReason("Unknown error: %s".formatted(response.getErrorCode().getValue()));
            }
        }

        if (!CollectionUtils.isEmpty(response.getWarnings())) {
            for (CreateDocumentPageResponse.WarningsEnum w : response.getWarnings()) {
                switch (w) {
                    case DOCUMENT_TYPE_NOT_RECOGNIZED -> result.setRejectReason(concat(result.getRejectReason(), "Document type not recognized."));
                    default -> result.setRejectReason(concat(result.getRejectReason(), "Unknown warning: %s".formatted(w.getValue())));
                }
            }
        }

        return result;
    }

    /**
     * Gets all customer data extracted from uploaded documents in a JSON form.
     * @param customerId id of the customer.
     * @return JSON serialized data.
     * @throws RemoteCommunicationException in case of the remote service error.
     * @throws DocumentVerificationException if the returned data could not be provided.
     */
    private String getExtractedData(String customerId) throws RemoteCommunicationException, DocumentVerificationException {
        final GetCustomerResponse response = innovatricsApiService.getCustomer(customerId)
                    .orElseThrow(() -> new RemoteCommunicationException("Customer data could not be obtained for customer %s".formatted(customerId)));
        try {
            return toJson(response);
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Unexpected error when serializing extracted data of customer %s".formatted(customerId), e);
        }
    }

    /**
     * Checks if a document portrait of the customer is available.
     * @param customerId id of the customer.
     * @return true if document portrait is available, false otherwise.
     */
    private boolean hasDocumentPortrait(String customerId) throws RemoteCommunicationException {
        return innovatricsApiService.getDocumentPortrait(customerId).isPresent();
    }

    /**
     * Creates DocumentSubmitResult containing extracted data.
     * @param customerId id of the customer to get data from.
     * @return DocumentSubmitResult containing extracted data.
     * @throws RemoteCommunicationException in case of the remote service error.
     * @throws DocumentVerificationException if the extracted data could not be provided.
     */
    private DocumentSubmitResult createSubmitResult(String customerId) throws RemoteCommunicationException, DocumentVerificationException {
        final DocumentSubmitResult result = new DocumentSubmitResult();
        result.setUploadId(customerId);
        result.setExtractedData(getExtractedData(customerId));
        return result;
    }

    /**
     * Verify document uploaded to the customer.
     * @param customerId id of the customer.
     * @return DocumentVerificationResult with rejection or error reason if any.
     * @throws RemoteCommunicationException in case the verification data could not be obtained.
     */
    private DocumentVerificationResult verifyDocument(String customerId) throws RemoteCommunicationException {
        return innovatricsApiService.inspectDocument(customerId)
                .map(r -> createVerificationResult(customerId, r))
                .orElseThrow(() -> new RemoteCommunicationException("Document inspection result could not be obtained for customer %s".formatted(customerId)));
    }

    /**
     * Parses DocumentInspectResponse into error or rejection details.
     * @param customerId id of the customer the document belongs to.
     * @param response inspection details.
     * @return DocumentVerificationResult
     */
    private DocumentVerificationResult createVerificationResult(String customerId, DocumentInspectResponse response) {
        final DocumentVerificationResult result = new DocumentVerificationResult();

        if (Boolean.TRUE.equals(response.getExpired())) {
            result.setRejectReason(concat(result.getRejectReason(), "Document expired."));
        }

        if (response.getMrzInspection() != null && Boolean.FALSE.equals(response.getMrzInspection().getValid())) {
            result.setRejectReason(concat(result.getRejectReason(), "MRZ does not conform the ICAO specification."));
        }

        // Portrait inspection checks if the ESTIMATED age and gender from a document portrait is consistent
        // with extracted data (MRZ or text).

        final VisualZoneInspection viz = response.getVisualZoneInspection();
        if (viz != null) {
            if (!CollectionUtils.isEmpty(viz.getOcrConfidence().getLowOcrConfidenceTexts())) {
                result.setErrorDetail(concat(result.getErrorDetail(), "Low OCR confidence of text."));
            }
            if (viz.getTextConsistency() != null && Boolean.FALSE.equals(viz.getTextConsistency().getConsistent())) {
                result.setRejectReason(concat(result.getRejectReason(), "Inconsistent text field."));
            }
        }

        if (response.getPageTampering() != null) {
            response.getPageTampering().forEach((k, v) -> {
                if (Boolean.TRUE.equals(v.getColorProfileChangeDetected())) {
                    result.setRejectReason(concat(result.getRejectReason(), "Colors on the document %s does not corresponds to the expected color profile.".formatted(k)));
                }
                if (Boolean.TRUE.equals(v.getLooksLikeScreenshot())) {
                    result.setRejectReason(concat(result.getRejectReason(), "Provided image of the document %s was taken from a screen of another device.".formatted(k)));
                }
                if (Boolean.TRUE.equals(v.getTamperedTexts())) {
                    result.setRejectReason(concat(result.getRejectReason(), "Text of the document %s is tampered.".formatted(k)));
                }
            });
        }

        result.setUploadId(customerId);
        // extractedData or verificationResult are not used
        return result;
    }

    /**
     * Get details of previously uploaded document of customer.
     * @param customerId id of the customer.
     * @return CustomerDocument with details
     * @throws RemoteCommunicationException if the document could not be obtained.
     */
    private CustomerDocument getDocument(String customerId) throws RemoteCommunicationException {
        return innovatricsApiService.getCustomer(customerId)
                .map(GetCustomerResponse::getCustomer)
                .map(Customer::getDocument)
                .orElseThrow(() -> new RemoteCommunicationException("A document could not be obtained for customer %s".formatted(customerId)));
    }

    private static String concat(String... values) {
        return Stream.of(values).filter(Objects::nonNull).collect(Collectors.joining(REJECTION_REASON_DELIMITER));
    }

    private <T> String toJson(T src) throws JsonProcessingException {
        return objectMapper.writeValueAsString(src);
    }

}
