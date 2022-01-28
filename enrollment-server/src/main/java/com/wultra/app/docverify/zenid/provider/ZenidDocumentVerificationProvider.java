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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.docverify.zenid.model.api.*;
import com.wultra.app.docverify.zenid.service.ZenidRestApiService;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.provider.DocumentVerificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.*;

/**
 * Implementation of the {@link DocumentVerificationProvider} with ZenID (https://zenid.trask.cz/)
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.document-verification.provider", havingValue = "zenid")
@Component
public class ZenidDocumentVerificationProvider implements DocumentVerificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZenidDocumentVerificationProvider.class);

    private final ObjectMapper objectMapper;

    private final ZenidRestApiService zenidApiService;

    /**
     * Service constructor.
     * @param objectMapper Object mapper.
     * @param zenidApiService ZenID API service.
     */
     @Autowired
    public ZenidDocumentVerificationProvider(
            @Qualifier("objectMapperZenid")
            ObjectMapper objectMapper,
            ZenidRestApiService zenidApiService) {
        this.objectMapper = objectMapper;
        this.zenidApiService = zenidApiService;
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, SubmittedDocument document) throws DocumentVerificationException {
        DocumentsSubmitResult result = new DocumentsSubmitResult();
        ResponseEntity<ZenidWebUploadSampleResponse> responseEntity;

        try {
            responseEntity = zenidApiService.syncSample(id, document.getDocumentId());
        } catch (RestClientException e) {
            logger.warn("Failed REST call to check " + document + " upload in ZenID, " + id, e);
            throw new DocumentVerificationException("Unable to check document upload due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when checking " + document + " upload in ZenID, " + id, e);
            throw new DocumentVerificationException("Unexpected error when checking document upload");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when checking " + document + " upload in ZenID, " + id);
            throw new DocumentVerificationException("Unexpected error when checking document upload");
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            logger.error("Failed to check " + document + " upload in ZenID, statusCode={}, responseBody='{}', {}",
                    responseEntity.getStatusCode(), responseEntity.getBody(), id);
            throw new DocumentVerificationException("Unable to check document upload due to a service error");
        }

        ZenidWebUploadSampleResponse response = responseEntity.getBody();
        DocumentSubmitResult documentSubmitResult = createDocumentSubmitResult(id, document, response);
        if (response.getMinedData() != null) {
            checkForMinedPhoto(id, document, result, response.getMinedData());
        }
        result.setResults(List.of(documentSubmitResult));

        return result;
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws DocumentVerificationException {
        DocumentsSubmitResult result = new DocumentsSubmitResult();

        String sessionId = UUID.randomUUID().toString();
        for (SubmittedDocument document : documents) {
            ResponseEntity<ZenidWebUploadSampleResponse> responseEntity;

            try {
                responseEntity = zenidApiService.uploadSample(id, sessionId, document);
            } catch (RestClientException e) {
                logger.warn("Failed REST call to submit documents to ZenID, " + id, e);
                throw new DocumentVerificationException("Unable to submit documents due to a REST call failure");
            } catch (Exception e) {
                logger.error("Unexpected error when submitting documents to ZenID, " + id, e);
                throw new DocumentVerificationException("Unexpected error when submitting documents");
            }

            if (responseEntity.getBody() == null) {
                logger.error("Missing response body when submitting documents to ZenID, " + id);
                throw new DocumentVerificationException("Unexpected error when submitting documents");
            }

            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                logger.error("Failed to submit documents to ZenID, statusCode={}, responseBody='{}', {}",
                        responseEntity.getStatusCode(), responseEntity.getBody(), id);
                throw new DocumentVerificationException("Unable to submit documents due to a service error");
            }

            ZenidWebUploadSampleResponse response = responseEntity.getBody();
            DocumentSubmitResult documentSubmitResult = createDocumentSubmitResult(id, document, response);
            if (response.getMinedData() != null) {
                checkForMinedPhoto(id, document, result, response.getMinedData());
            }
            result.getResults().add(documentSubmitResult);
        }
        return result;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws DocumentVerificationException {
        ResponseEntity<ZenidWebInvestigateResponse> responseEntity;
        try {
            responseEntity = zenidApiService.investigateSamples(uploadIds);
        } catch (RestClientException e) {
            logger.warn("Failed REST call to verify documents " + uploadIds + " in ZenID", e);
            throw new DocumentVerificationException("Unable to verify documents due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when verifying documents " + uploadIds + " in ZenID", e);
            throw new DocumentVerificationException("Unexpected error when verifying documents");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when verifying documents " + uploadIds + " in ZenID, " + id);
            throw new DocumentVerificationException("Unexpected error when verifying documents");
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            logger.error("Failed to verify documents {} in ZenID, statusCode={}, responseBody='{}', {}",
                    uploadIds, responseEntity.getStatusCode(), responseEntity.getBody(), id);
            throw new DocumentVerificationException("Unable to verify documents due to a service error");
        }

        return toResult(id, responseEntity.getBody());
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws DocumentVerificationException {
        ResponseEntity<ZenidWebInvestigateResponse> responseEntity;
        try {
            responseEntity = zenidApiService.getInvestigation(verificationId);
        } catch (RestClientException e) {
            logger.warn("Failed REST call to get a verification result for verificationId=" + verificationId + " from ZenID", e);
            throw new DocumentVerificationException("Unable to get a verification result due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when getting a verification result for verificationId=" + verificationId + " from ZenID", e);
            throw new DocumentVerificationException("Unexpected error when getting a verification result");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when getting a verification result for verificationId=" + verificationId + " from ZenID, " + id);
            throw new DocumentVerificationException("Unexpected error when getting a verification result for verificationId=" + verificationId + " from ZenID, " + id);
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            logger.error("Failed to get a verification result for verificationId={} from ZenID, statusCode={}, responseBody='{}', {}",
                    verificationId, responseEntity.getStatusCode(), responseEntity.getBody(), id);
            throw new DocumentVerificationException("Unable to get a verification result");
        }

        return toResult(id, responseEntity.getBody());
    }

    @Override
    public Image getPhoto(String photoId) throws DocumentVerificationException {
        ResponseEntity<byte[]> responseEntity;
        try {
            responseEntity = zenidApiService.getImage(photoId);
        } catch (RestClientException e) {
            logger.warn("Failed REST call to get a photoId=" + photoId + " from ZenID", e);
            throw new DocumentVerificationException("Unable to get a photo due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when getting a photo=" + photoId + " from ZenID", e);
            throw new DocumentVerificationException("Unexpected error when getting a photo");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when getting a photoId={} from ZenID", photoId);
            throw new DocumentVerificationException("Unexpected error when getting a photo");
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            logger.error("Failed to get a photo photoId={} from ZenID, statusCode={}, responseBody='{}'",
                    photoId, responseEntity.getStatusCode(), responseEntity.getBody());
            throw new DocumentVerificationException("Unable to get a photo due to a service error");
        }

        String filename = getContentDispositionFilename(responseEntity.getHeaders());

        Image image = new Image();
        image.setData(responseEntity.getBody());
        image.setFilename(filename);
        return image;
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) throws DocumentVerificationException {
        for (String uploadId : uploadIds) {
            ResponseEntity<ZenidWebDeleteSampleResponse> responseEntity;
            try {
                responseEntity = zenidApiService.deleteSample(uploadId);
            } catch (RestClientException e) {
                logger.warn("Failed REST call to cleanup documents from ZenID, " + id, e);
                throw new DocumentVerificationException("Unable to cleanup documents due to a REST call failure");
            } catch (Exception e) {
                logger.error("Unexpected error when cleaning up documents from ZenID, " + id, e);
                throw new DocumentVerificationException("Unexpected error when cleaning up documents");
            }

            if (responseEntity.getBody() == null) {
                logger.error("Missing response body when cleaning up documents from ZenID, " + id);
                throw new DocumentVerificationException("Unexpected error when cleaning up documents");
            }

            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                logger.error("Failed to cleanup a document uploadId={} from ZenID, statusCode={}, responseBody='{}', {}",
                        uploadId, responseEntity.getStatusCode(), responseEntity.getBody(), id);
                throw new DocumentVerificationException("Unable to cleanup documents due to a service error");
            }

            ZenidWebDeleteSampleResponse response = responseEntity.getBody();
            if (ZenidWebDeleteSampleResponse.ErrorCodeEnum.UNKNOWNSAMPLEID.equals(response.getErrorCode())) {
                logger.info("Cleanup of an unknown document with uploadId={}", uploadId);
            } else if (response.getErrorCode() != null) {
                logger.error("Failed to cleanup uploadId={} from ZenID, errorCode={}, errorText={}",
                        uploadId, response.getErrorCode(), response.getErrorText());
                throw new DocumentVerificationException("Failed to cleanup a document with uploadId=" + uploadId);
            }
        }
        logger.info("{} Cleaned up uploaded documents {} from ZenID.", id, uploadIds);
    }

    private DocumentSubmitResult createDocumentSubmitResult(OwnerId id,
                                                            SubmittedDocument document,
                                                            ZenidWebUploadSampleResponse response)
            throws DocumentVerificationException {
        DocumentSubmitResult documentSubmitResult = new DocumentSubmitResult();
        documentSubmitResult.setDocumentId(document.getDocumentId());
        documentSubmitResult.setUploadId(response.getSampleID());

        if (response.getMinedData() != null) {
            String extractedData = toExtractedData(id, response.getMinedData());
            documentSubmitResult.setExtractedData(extractedData);
        }

        if (ZenidWebUploadSampleResponse.StateEnum.DONE.equals(response.getState())) {
            if (documentSubmitResult.getExtractedData() == null) {
                logger.info("No data extracted from {} in ZenID, defaulting to empty json data, {}", document, id);
                documentSubmitResult.setExtractedData(DocumentSubmitResult.NO_DATA_EXTRACTED);
            }
        } else if (ZenidWebUploadSampleResponse.StateEnum.NOTDONE.equals(response.getState())) {
            logger.debug("Document upload of {} is still in progress in ZenID, {}", document, id);
        } else if (ZenidWebUploadSampleResponse.StateEnum.REJECTED.equals(response.getState())) {
            documentSubmitResult.setRejectReason(response.getErrorText());
        } else {
            logger.warn("Unexpected upload response from ZenID: {} for documentId= {}, {}",
                    response.getState(), document.getDocumentId(), id);
            throw new DocumentVerificationException("Unable to upload a document");
        }

        if (response.getErrorCode() != null) {
            documentSubmitResult.setErrorDetail("ZenID error: " + response.getErrorCode() +
                    (response.getErrorText() != null ? ", " + response.getErrorText() : ""));
        }
        return documentSubmitResult;
    }

    private void checkForMinedPhoto(
            OwnerId id,
            SubmittedDocument document,
            DocumentsSubmitResult result,
            ZenidSharedMineAllResult minedData) {
        // Photo hash of the person is optionally present at /MinedData/Photo/ImageData/ImageHash
        ZenidSharedMinedPhoto photo = minedData.getPhoto();
        if (photo != null && photo.getImageData() != null && photo.getImageData().getImageHash() != null) {
            logger.info("Extracted a photoId from submitted {} to ZenID, " + id, document);
            result.setExtractedPhotoId(photo.getImageData().getImageHash().getAsText());
        }
    }

    private String getContentDispositionFilename(HttpHeaders headers) {
        String filename = headers.getContentDisposition().getFilename();
        if (filename == null) {
            MediaType contentType = headers.getContentType();
            if (contentType != null) {
                filename = "unknown." + contentType.getSubtype().toLowerCase();
            } else {
                throw new IllegalStateException("Unable to resolve filename");
            }
        }
        return filename;
    }

    private DocumentsVerificationResult toResult(OwnerId id, ZenidWebInvestigateResponse response)
            throws DocumentVerificationException {
        DocumentsVerificationResult result = new DocumentsVerificationResult();
        result.setVerificationId(String.valueOf(response.getInvestigationID()));

        if (response.getErrorCode() != null) {
            result.setErrorDetail("ZenID error: " + response.getErrorCode() +
                    (response.getErrorText() != null ? ", " + response.getErrorText() : ""));
        } else {
            Map<String, List<ZenidWebInvestigationValidatorResponse>> sampleIdsValidations = new HashMap<>();
            List<ZenidWebInvestigationValidatorResponse> globalValidations = new ArrayList<>();
            for (ZenidWebInvestigationValidatorResponse validatorResult : response.getValidatorResults()) {
                if (validatorResult.getIssues().isEmpty()) {
                    // no issues - some kind of global validation
                    ZenidWebInvestigationValidatorResponse validationData = copyOf(validatorResult);
                    globalValidations.add(validationData);
                } else {
                    for (ZenidWebInvestigationIssueResponse issueItem : validatorResult.getIssues()) {
                        if (issueItem.getSampleID() == null) {
                            // missing sampleId - some kind of global validation
                            ZenidWebInvestigationValidatorResponse validationData = copyOf(validatorResult);
                            validationData.addIssuesItem(issueItem);
                            globalValidations.add(validationData);
                        } else {
                            // with sampleId - validation on a specific document
                            ZenidWebInvestigationValidatorResponse validationData = copyOf(validatorResult);
                            validationData.addIssuesItem(issueItem);
                            sampleIdsValidations.computeIfAbsent(issueItem.getSampleID(), (sampleId) -> new ArrayList<>())
                                    .add(validationData);
                        }
                    }
                }
            }

            List<DocumentVerificationResult> verificationResults = new ArrayList<>();

            String extractedData = toExtractedData(id, response.getMinedData());
            for (String sampleId : sampleIdsValidations.keySet()) {
                List<ZenidWebInvestigationValidatorResponse> validations = new ArrayList<>(sampleIdsValidations.get(sampleId));

                DocumentVerificationResult verificationResult = new DocumentVerificationResult();
                verificationResult.setExtractedData(extractedData);
                verificationResult.setUploadId(sampleId);

                // Find a first failed validation, use its description as the rejected reason for the document
                Optional<ZenidWebInvestigationValidatorResponse> failedValidation = validations.stream()
                        .filter(validation -> !validation.isOk())
                        // Sort the validations by difference between the actual score and the accepted score value
                        .max(Comparator.comparingInt((value -> value.getAcceptScore() - value.getScore())));
                if (failedValidation.isPresent()) {
                    String rejectReason = failedValidation.get().getIssues().get(0).getIssueDescription();
                    verificationResult.setRejectReason(rejectReason);
                }

                validations.addAll(globalValidations);
                String verificationResultData;
                try {
                    verificationResultData = objectMapper.writeValueAsString(validations);
                } catch (JsonProcessingException e) {
                    logger.error("Unexpected error when processing verification result data, " + id, e);
                    throw new DocumentVerificationException("Unexpected error when processing verification result data");
                }

                verificationResult.setVerificationResult(verificationResultData);
                verificationResults.add(verificationResult);
            }
            result.setResults(verificationResults);
        }

        // TODO compute verification score
        // result.setVerificationScore();

        DocumentVerificationStatus verificationStatus = toStatus(response.getState());
        if (result.getResults() != null) {
            // Check the results if there is no rejected validation
            Optional<DocumentVerificationResult> optionalFailedVerification = result.getResults()
                    .stream()
                    .filter(value -> value.getRejectReason() != null)
                    .findAny();
            if (optionalFailedVerification.isPresent()) {
                verificationStatus = DocumentVerificationStatus.REJECTED;
                result.setRejectReason(optionalFailedVerification.get().getRejectReason());
            }
        }
        result.setStatus(verificationStatus);
        return result;
    }

    private ZenidWebInvestigationValidatorResponse copyOf(ZenidWebInvestigationValidatorResponse value) {
        ZenidWebInvestigationValidatorResponse result = new ZenidWebInvestigationValidatorResponse();
        result.setAcceptScore(value.getAcceptScore());
        result.setCode(value.getCode());
        result.setName(value.getName());
        result.setScore(value.getScore());
        result.setOk(value.isOk());
        return result;
    }

    private String toExtractedData(OwnerId id, ZenidSharedMineAllResult minedData) throws DocumentVerificationException {
        String extractedData;
        try {
            extractedData = objectMapper.writeValueAsString(minedData);
        } catch (JsonProcessingException e) {
            logger.error("Unexpected error when processing extracted data, " + id, e);
            throw new DocumentVerificationException("Unexpected error when processing extracted data");
        }
        return extractedData;
    }

    private DocumentVerificationStatus toStatus(ZenidWebInvestigateResponse.StateEnum stateEnum) {
        switch (stateEnum) {
            case DONE:
                return DocumentVerificationStatus.ACCEPTED;
            case ERROR:
                return DocumentVerificationStatus.FAILED;
            case NOTDONE:
            case OPERATOR:
                return DocumentVerificationStatus.IN_PROGRESS;
            case REJECTED:
                return DocumentVerificationStatus.REJECTED;
            default:
                throw new IllegalStateException("Unknown investigation status in ZenID: " + stateEnum);
        }
    }

}
