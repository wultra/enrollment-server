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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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
    private final InnovatricsConfigProps configuration;

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) throws RemoteCommunicationException, DocumentVerificationException {
        logger.warn("Unexpected state of document {}, {}", document, id);
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Innovatrics provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws RemoteCommunicationException, DocumentVerificationException {
        if (CollectionUtils.isEmpty(documents)) {
            logger.info("Empty documents list passed to document provider, {}", id);
            return new DocumentsSubmitResult();
        }

        final DocumentType documentType = documents.get(0).getType();
        if (DocumentType.SELFIE_PHOTO == documentType) {
            logger.debug("Selfie photo passed as a document, {}", id);
            throw new DocumentVerificationException("Selfie photo cannot be submitted as a document");
        }

        if (DocumentType.SELFIE_VIDEO == documentType) {
            logger.debug("Selfie video passed as a document, {}", id);
            throw new DocumentVerificationException("Selfie video cannot be submitted as a document");
        }

        final String customerId = createCustomer(id);
        createDocument(customerId, documentType, id);
        logger.debug("Created new customer {}, {}", customerId, id);

        final DocumentsSubmitResult results = new DocumentsSubmitResult();
        for (SubmittedDocument page : documents) {
            final CreateDocumentPageResponse createDocumentPageResponse = provideDocumentPage(customerId, page, id);
            if (containsError(createDocumentPageResponse)) {
                logger.debug("Page upload was not successful, {}", id);
                results.getResults().add(createErrorSubmitResult(customerId, createDocumentPageResponse, page));
            } else {
                logger.debug("Document page was read successfully by provider, {}", id);
                results.getResults().add(createSubmitResult(customerId, page));
            }
        }

        final Optional<DocumentSubmitResult> primaryPage = results.getResults().stream()
                .filter(result -> Strings.isNullOrEmpty(result.getRejectReason()) && Strings.isNullOrEmpty(result.getErrorDetail()))
                .findFirst();

        if (primaryPage.isPresent()) {
            // Only first found successfully submitted page has extracted data, others has empty JSON
            primaryPage.get().setExtractedData(getExtractedData(customerId, id));
            if (hasDocumentPortrait(customerId, id)) {
                results.setExtractedPhotoId(customerId);
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
        results.setResults(new ArrayList<>());

        // Pages of the same document have same uploadId (= customerId), no reason to generate verification for each one.
        final List<String> distinctUploadIds = uploadIds.stream().distinct().toList();
        for (String customerId : distinctUploadIds) {
            final DocumentVerificationResult result = createVerificationResult(customerId, id);
            results.getResults().add(result);
        }

        final String rejectReasons = results.getResults().stream()
                .map(DocumentVerificationResult::getRejectReason)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(";"));
        if (StringUtils.hasText(rejectReasons)) {
            logger.debug("Some documents were rejected: rejectReasons={}, {}", rejectReasons, id);
            results.setStatus(DocumentVerificationStatus.REJECTED);
            results.setRejectReason(rejectReasons);
        } else {
            logger.debug("All documents accepted, {}", id);
            results.setStatus(DocumentVerificationStatus.ACCEPTED);
        }
        results.setVerificationId(UUID.randomUUID().toString());
        return results;
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws RemoteCommunicationException, DocumentVerificationException {
        logger.warn("Unexpected state of documents with verificationId={}, {}", verificationId, id);
        throw new UnsupportedOperationException("Method getVerificationResult is not supported by Innovatrics provider.");
    }

    @Override
    public Image getPhoto(String photoId) throws RemoteCommunicationException, DocumentVerificationException {
        logger.warn("Unexpected document portrait query for customerId={}", photoId);
        throw new UnsupportedOperationException("Method getPhoto is not implemented by Innovatrics provider.");
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        // Pages of the same document have same uploadId (= customerId), no reason to call delete for each one.
        final List<String> distinctUploadIds = uploadIds.stream().distinct().toList();
        logger.info("Invoked cleanupDocuments, {}", id);
        for (String customerId : distinctUploadIds) {
            innovatricsApiService.deleteCustomer(customerId, id);
        }
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        logger.debug("Parsing rejection reasons of {}", docResult);
        final String rejectionReasons = docResult.getRejectReason();
        if (!StringUtils.hasText(rejectionReasons)) {
            return Collections.emptyList();
        }

        return deserializeFromString(rejectionReasons);
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) throws RemoteCommunicationException, DocumentVerificationException {
        logger.debug("#initVerificationSdk does nothing for Innovatrics, {}", id);
        return new VerificationSdkInfo();
    }

    /**
     * Create a new customer resource.
     * @param ownerId owner identification.
     * @return ID of the new customer.
     * @throws RemoteCommunicationException if the resource was not created properly.
     */
    private String createCustomer(final OwnerId ownerId) throws RemoteCommunicationException {
        return innovatricsApiService.createCustomer(ownerId).getId();
    }

    /**
     * Create a new document resource to an existing customer.
     * @param customerId id of the customer to assign the resource to.
     * @param documentType type of the document that will be uploaded later.
     * @param ownerId owner identification.
     * @throws RemoteCommunicationException if the resource was not created properly.
     */
    private void createDocument(final String customerId, final DocumentType documentType, final OwnerId ownerId) throws RemoteCommunicationException, DocumentVerificationException {
            innovatricsApiService.createDocument(customerId, documentType, ownerId);
    }

    /**
     * Upload a page of a document to a customer.
     * @param customerId id of the customer to whom upload the document page.
     * @param page SubmittedDocument object representing the page.
     * @param ownerId owner identification.
     * @return CreateDocumentPageResponse containing info about the document type. An unsuccessful response will contain an error code.
     * @throws RemoteCommunicationException if the document page was not uploaded properly.
     */
    private CreateDocumentPageResponse provideDocumentPage(final String customerId, final SubmittedDocument page, final OwnerId ownerId) throws RemoteCommunicationException {
            return innovatricsApiService.provideDocumentPage(customerId, page.getSide(), page.getPhoto().getData(), ownerId);
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
     * @throws DocumentVerificationException in case of rejection reason serialization error.
     */
    private DocumentSubmitResult createErrorSubmitResult(String uploadId, CreateDocumentPageResponse response, SubmittedDocument submitted) throws DocumentVerificationException {
        final DocumentSubmitResult result = new DocumentSubmitResult();
        result.setUploadId(uploadId);
        result.setDocumentId(submitted.getDocumentId());

        final List<String> rejectionReasons = new ArrayList<>();
        if (response.getErrorCode() != null) {
            switch (response.getErrorCode()) {
                case NO_CARD_CORNERS_DETECTED -> rejectionReasons.add("Document page was not detected in the photo.");
                case PAGE_DOESNT_MATCH_DOCUMENT_TYPE_OF_PREVIOUS_PAGE -> rejectionReasons.add("Mismatched document pages types.");
                default -> rejectionReasons.add("Unknown error: %s".formatted(response.getErrorCode().getValue()));
            }
        }

        if (!CollectionUtils.isEmpty(response.getWarnings())) {
            for (CreateDocumentPageResponse.WarningsEnum warning : response.getWarnings()) {
                switch (warning) {
                    case DOCUMENT_TYPE_NOT_RECOGNIZED -> rejectionReasons.add("Document type not recognized.");
                    default -> rejectionReasons.add("Unknown warning: %s".formatted(warning.getValue()));
                }
            }
        }

        if (!rejectionReasons.isEmpty()) {
            result.setRejectReason(serializeToString(rejectionReasons));
        }

        return result;
    }

    /**
     * Gets all customer data extracted from uploaded documents in a JSON form.
     * @param customerId id of the customer.
     * @param ownerId owner identification.
     * @return JSON serialized data.
     * @throws RemoteCommunicationException in case of the remote service error.
     * @throws DocumentVerificationException if the returned data could not be provided.
     */
    private String getExtractedData(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException, DocumentVerificationException {
        return serializeToString(innovatricsApiService.getCustomer(customerId, ownerId));
    }

    /**
     * Checks if a document portrait of the customer is available.
     * @param customerId id of the customer.
     * @param ownerId owner identification.
     * @return true if document portrait is available, false otherwise.
     */
    private boolean hasDocumentPortrait(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        return innovatricsApiService.getDocumentPortrait(customerId, ownerId).isPresent();
    }

    /**
     * Creates DocumentSubmitResult containing extracted data.
     * @param customerId id of the customer to get data from.
     * @return DocumentSubmitResult containing extracted data.
     */
    private static DocumentSubmitResult createSubmitResult(final String customerId, final SubmittedDocument submitted) {
        final DocumentSubmitResult result = new DocumentSubmitResult();
        result.setUploadId(customerId);
        result.setDocumentId(submitted.getDocumentId());
        result.setExtractedData(DocumentSubmitResult.NO_DATA_EXTRACTED);
        return result;
    }

    /**
     * Gets document inspection from Innovatrics and parses it to the verification result.
     * @param customerId id of the customer the document belongs to.
     * @param ownerId owner identification.
     * @return DocumentVerificationResult
     */
    private DocumentVerificationResult createVerificationResult(String customerId, OwnerId ownerId) throws DocumentVerificationException, RemoteCommunicationException {
        final DocumentInspectResponse response = innovatricsApiService.inspectDocument(customerId, ownerId);

        final List<String> rejectionReasons = new ArrayList<>();
        if (Boolean.TRUE.equals(response.getExpired())) {
            rejectionReasons.add("Document expired.");
        }

        if (response.getMrzInspection() != null && !Boolean.TRUE.equals(response.getMrzInspection().getValid())) {
            rejectionReasons.add("MRZ does not conform the ICAO specification.");
        }

        final VisualZoneInspection viz = response.getVisualZoneInspection();
        rejectionReasons.addAll(parseVisualZoneInspection(viz));

        if (response.getPageTampering() != null) {
            response.getPageTampering().forEach((side, inspection) -> {
                if (Boolean.TRUE.equals(inspection.getColorProfileChangeDetected())) {
                    rejectionReasons.add("Colors on the document %s does not corresponds to the expected color profile.".formatted(side));
                }
                if (Boolean.TRUE.equals(inspection.getLooksLikeScreenshot())) {
                    rejectionReasons.add("Provided image of the document %s was taken from a screen of another device.".formatted(side));
                }
                if (Boolean.TRUE.equals(inspection.getTamperedTexts())) {
                    rejectionReasons.add("Text of the document %s is tampered.".formatted(side));
                }
            });
        }

        final DocumentVerificationResult result = new DocumentVerificationResult();
        result.setUploadId(customerId);
        result.setVerificationResult(serializeToString(response));
        if (!rejectionReasons.isEmpty()) {
            result.setRejectReason(serializeToString(rejectionReasons));
        }
        return result;
    }

    /**
     * Parse VisualZoneInspection of a document provided by Innovatrics.
     * @param visualZoneInspection inspection of a document by Innovatrics.
     * @return List of reasons to reject the document.
     */
    private List<String> parseVisualZoneInspection(final VisualZoneInspection visualZoneInspection) {
        final List<String> rejectionReasons = new ArrayList<>();
        if (visualZoneInspection == null) {
            return rejectionReasons;
        }

        // Contains fields with a ocr confidence lower than ocr-text-field-threshold settings.
        final List<String> lowOcrConfidenceAttributes = visualZoneInspection.getOcrConfidence().getLowOcrConfidenceTexts();
        if (!CollectionUtils.isEmpty(lowOcrConfidenceAttributes)) {
            rejectionReasons.add("Low OCR confidence of attributes: %s".formatted(lowOcrConfidenceAttributes));
        }

        final TextConsistency textConsistency = visualZoneInspection.getTextConsistency();
        if (textConsistency == null) {
            return rejectionReasons;
        }

        final TextConsistentWith textConsistentWith = textConsistency.getConsistencyWith();
        if (textConsistentWith == null) {
            return rejectionReasons;
        }

        final MrzConsistency mrzConsistency = textConsistentWith.getMrz();
        if (mrzConsistency != null) {
            final List<String> inconsistentAttributes = mrzConsistency.getInconsistentTexts();
            if (!inconsistentAttributes.isEmpty()) {
                rejectionReasons.add("Inconsistent attributes with MRZ: %s".formatted(inconsistentAttributes));
            }
        }

        final BarcodesConsistency barcodesConsistency = textConsistentWith.getBarcodes();
        if (barcodesConsistency != null) {
            final List<String> inconsistentAttributes = barcodesConsistency.getInconsistentTexts();
            if (!inconsistentAttributes.isEmpty()) {
                rejectionReasons.add("Inconsistent attributes with barcode: %s".formatted(inconsistentAttributes));
            }
        }

        return rejectionReasons;
    }

    private <T> String serializeToString(T src) throws DocumentVerificationException {
        try {
            return objectMapper.writeValueAsString(src);
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Unexpected error when serializing data", e);
        }
    }

    private <T> T deserializeFromString(String src) throws DocumentVerificationException {
        try {
            return objectMapper.readValue(src, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Unexpected error when deserializing data", e);
        }
    }

}
