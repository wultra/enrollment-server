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
package com.wultra.app.onboardingserver.providers.zenid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.providers.zenid.model.api.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.core.rest.client.base.RestClientException;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://zenid.trask.cz/">ZenID</a>.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "zenid")
@Component
@Slf4j
public class ZenidDocumentVerificationProvider implements DocumentVerificationProvider {

    private static final String SDK_INIT_RESPONSE = "zenid-sdk-init-response";
    private static final String SDK_INIT_TOKEN = "sdk-init-token";

    private static final String INTERNAL_SERVER_ERROR = "InternalServerError";
    private static final String LICENSE_INVALID = "License invalid";

    private final ZenidConfigProps zenidConfigProps;

    private final ObjectMapper objectMapper;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final ZenidRestApiService zenidApiService;

    /**
     * Service constructor.
     *
     * @param zenidConfigProps               ZenID configuration properties.
     * @param objectMapper                   Object mapper.
     * @param documentVerificationRepository Document verification repository.
     * @param zenidApiService                ZenID API service.
     */
    @Autowired
    public ZenidDocumentVerificationProvider(
            ZenidConfigProps zenidConfigProps,
            @Qualifier("objectMapperZenid")
            ObjectMapper objectMapper,
            DocumentVerificationRepository documentVerificationRepository,
            ZenidRestApiService zenidApiService) {
        this.zenidConfigProps = zenidConfigProps;
        this.objectMapper = objectMapper;
        this.documentVerificationRepository = documentVerificationRepository;
        this.zenidApiService = zenidApiService;
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) throws RemoteCommunicationException, DocumentVerificationException {
        DocumentsSubmitResult result = new DocumentsSubmitResult();
        ResponseEntity<ZenidWebUploadSampleResponse> responseEntity;

        try {
            responseEntity = zenidApiService.syncSample(document.getUploadId());
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Unable to check %s upload in ZenID due to a REST call failure, statusCode=%s, responseBody='%s', %s",
                            document, e.getStatusCode(), e.getResponse(), id),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException(
                    String.format("Unexpected error when checking %s upload in ZenID, %s", document, id), e);
        }

        final ZenidWebUploadSampleResponse response = responseEntity.getBody();
        if (response == null) {
            throw new RemoteCommunicationException(String.format("Missing response body when checking %s upload in ZenID, %s", document, id));
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DocumentVerificationException(
                    String.format("Failed to check %s upload in ZenID, statusCode=%s, responseBody='%s', %s",
                            document, responseEntity.getStatusCode(), response, id));
        }
        handleLicenceError(response.getErrorCode(), response.getErrorText());

        DocumentSubmitResult documentSubmitResult =
                createDocumentSubmitResult(id, document.getUploadId(), document.toString(), response);
        if (response.getMinedData() != null) {
            checkForMinedPhoto(id, document.getUploadId(), result, response.getMinedData());
        }

        if (zenidConfigProps.isAdditionalDocSubmitValidationsEnabled() && response.getMinedData() != null) {
            checkAdditionalValidations(id, document.getType(), document.getSide(), documentSubmitResult, response.getMinedData());
        }

        result.setResults(List.of(documentSubmitResult));

        return result;
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws RemoteCommunicationException, DocumentVerificationException {
        DocumentsSubmitResult result = new DocumentsSubmitResult();

        for (SubmittedDocument document : documents) {
            ResponseEntity<ZenidWebUploadSampleResponse> responseEntity;

            try {
                responseEntity = zenidApiService.uploadSample(id, document);
            } catch (RestClientException e) {
                throw new RemoteCommunicationException(
                        String.format("Failed REST call to submit documents to ZenID, statusCode=%s, responseBody='%s', %s",
                                e.getStatusCode(), e.getResponse(), id),
                        e);
            } catch (Exception e) {
                throw new RemoteCommunicationException(String.format("Unexpected error when submitting documents to ZenID, %s", id), e);
            }

            final ZenidWebUploadSampleResponse response = responseEntity.getBody();
            if (response == null) {
                throw new RemoteCommunicationException(String.format("Missing response body when submitting documents to ZenID, %s", id));
            }

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DocumentVerificationException(
                        String.format("Failed to submit documents to ZenID, statusCode=%s, responseBody='%s', %s",
                                responseEntity.getStatusCode(), response, id));
            }
            handleLicenceError(response.getErrorCode(), response.getErrorText());

            DocumentSubmitResult documentSubmitResult =
                    createDocumentSubmitResult(id, document.getDocumentId(), document.toString(), response);
            if (response.getMinedData() != null) {
                checkForMinedPhoto(id, document.getDocumentId(), result, response.getMinedData());
            }
            if (zenidConfigProps.isAdditionalDocSubmitValidationsEnabled() && response.getMinedData() != null) {
                checkAdditionalValidations(id, document.getType(), document.getSide(), documentSubmitResult, response.getMinedData());
            }
            result.getResults().add(documentSubmitResult);
        }
        return result;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        ResponseEntity<ZenidWebInvestigateResponse> responseEntity;
        try {
            responseEntity = zenidApiService.investigateSamples(uploadIds);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to verify documents %s in ZenID, statusCode=%s, responseBody='%s', %s",
                            uploadIds, e.getStatusCode(), e.getResponse(), id),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException(String.format("Unexpected error when verifying documents %s in ZenID", uploadIds), e);
        }

        final ZenidWebInvestigateResponse response = responseEntity.getBody();
        if (response == null) {
            throw new RemoteCommunicationException(String.format("Missing response body when verifying documents %s in ZenID, %s", uploadIds, id));
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DocumentVerificationException(
                    String.format("Failed to verify documents %s in ZenID, statusCode=%s, responseBody='%s', %s", uploadIds, responseEntity.getStatusCode(), response, id));
        }
        handleLicenceError(response.getErrorCode(), response.getErrorText());

        return toResult(id, response, uploadIds);
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws RemoteCommunicationException, DocumentVerificationException {
        ResponseEntity<ZenidWebInvestigateResponse> responseEntity;
        try {
            responseEntity = zenidApiService.getInvestigation(verificationId);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to get a verification result for verificationId=%s from ZenID, statusCode=%s, responseBody='%s', %s", verificationId, e.getStatusCode(), e.getResponse(), id), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when getting a verification result for verificationId=" + verificationId, e);
        }

        final ZenidWebInvestigateResponse response = responseEntity.getBody();
        if (response == null) {
            throw new RemoteCommunicationException(
                    String.format("Unexpected error when getting a verification result for verificationId=%s from ZenID, %s", verificationId, id));
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DocumentVerificationException(
                    String.format("Failed to get a verification result for verificationId=%s from ZenID, statusCode=%s, responseBody='%s', %s",
                            verificationId, responseEntity.getStatusCode(), response, id));
        }
        handleLicenceError(response.getErrorCode(), response.getErrorText());

        List<String> uploadIds = documentVerificationRepository.findAllUploadIds(verificationId);
        return toResult(id, response, uploadIds);
    }

    @Override
    public Image getPhoto(String photoId) throws RemoteCommunicationException, DocumentVerificationException {
        ResponseEntity<byte[]> responseEntity;
        try {
            responseEntity = zenidApiService.getImage(photoId);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to get a photoId=%s from ZenID, statusCode=%s, responseBody='%s'", photoId, e.getStatusCode(), e.getResponse()),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException(String.format("Unexpected error when getting a photo=%s from ZenID", photoId), e);
        }

        if (responseEntity.getBody() == null) {
            throw new RemoteCommunicationException(String.format("Missing response body when getting a photoId=%s from ZenID", photoId));
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DocumentVerificationException(String.format("Failed to get a photo photoId=%s from ZenID, statusCode=%s, hasBody=%s",
                    photoId, responseEntity.getStatusCode(), responseEntity.hasBody()));
        }

        String filename = getContentDispositionFilename(responseEntity.getHeaders());

        return Image.builder()
                .data(responseEntity.getBody())
                .filename(filename)
                .build();
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        for (String uploadId : uploadIds) {
            ResponseEntity<ZenidWebDeleteSampleResponse> responseEntity;
            try {
                responseEntity = zenidApiService.deleteSample(uploadId);
            } catch (RestClientException e) {
                throw new RemoteCommunicationException(
                        String.format("Failed REST call to cleanup documents from ZenID, statusCode=%s, responseBody='%s', %s",
                            e.getStatusCode(), e.getResponse(), id),
                        e);
            } catch (Exception e) {
                throw new RemoteCommunicationException("Unexpected error when cleaning up documents from ZenID, " + id, e);
            }

            final ZenidWebDeleteSampleResponse response = responseEntity.getBody();
            if (response == null) {
                throw new RemoteCommunicationException("Missing response body when cleaning up documents from ZenID, " + id);
            }

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new DocumentVerificationException(
                        String.format("Failed to cleanup a document uploadId=%s from ZenID, statusCode=%s, responseBody='%s', %s",
                                uploadId, responseEntity.getStatusCode(), response, id));
            }
            handleLicenceError(response.getErrorCode(), response.getErrorText());

            if (ZenidWebDeleteSampleResponse.ErrorCodeEnum.UNKNOWNSAMPLEID.equals(response.getErrorCode())) {
                logger.info("Cleanup of an unknown document with uploadId={}", uploadId);
            } else if (response.getErrorCode() != null) {
                throw new DocumentVerificationException(
                        String.format("Failed to cleanup uploadId=%s from ZenID, errorCode=%s, errorText=%s",
                                uploadId, response.getErrorCode(), response.getErrorText()));
            }
        }
        logger.info("{} Cleaned up uploaded documents {} from ZenID.", id, uploadIds);
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        if (docResult.getVerificationResult() == null) {
            logger.warn("Missing the verification result in {} to parse rejected errors from", docResult);
            return Collections.emptyList();
        }
        final List<ZenidWebInvestigationValidatorResponse> validations;
        try {
            validations = objectMapper.readValue(docResult.getVerificationResult(), new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Unexpected error when parsing verification result data from " + docResult, e);
        }

        final List<String> errors = new ArrayList<>();
        if (validations != null) {
            validations.forEach(validation -> {
                if (Boolean.TRUE.equals(validation.getOk())) {
                    return;
                }
                validation.getIssues().forEach(issue -> errors.add(issue.getIssueDescription()));
            });
        }

        return errors;
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) throws RemoteCommunicationException, DocumentVerificationException {
        Preconditions.checkArgument(initAttributes.containsKey(SDK_INIT_TOKEN), "Missing initialization token for ZenID SDK");
        String token = initAttributes.get(SDK_INIT_TOKEN);

        ResponseEntity<ZenidWebInitSdkResponse> responseEntity;
        try {
            responseEntity = zenidApiService.initSdk(token);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to init ZenID SDK, statusCode=%s, responseBody='%s', %s", e.getStatusCode(), e.getResponse(), id),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when initializing ZenID SDK, " + id, e);
        }

        final ZenidWebInitSdkResponse response = responseEntity.getBody();
        if (response == null) {
            throw new RemoteCommunicationException("Missing response body when initializing ZenID SDK, " + id);
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new DocumentVerificationException(
                    String.format("Failed to initialize ZenID SDK, statusCode=%s, responseBody='%s', %s",
                        responseEntity.getStatusCode(), response, id));
        }

        VerificationSdkInfo verificationSdkInfo = new VerificationSdkInfo();
        verificationSdkInfo.getAttributes().put(SDK_INIT_RESPONSE, response.getResponse());
        return verificationSdkInfo;
    }

    private DocumentSubmitResult createDocumentSubmitResult(OwnerId id,
                                                            String documentId,
                                                            String uploadContext,
                                                            ZenidWebUploadSampleResponse response)
            throws DocumentVerificationException {
        DocumentSubmitResult documentSubmitResult = new DocumentSubmitResult();
        documentSubmitResult.setDocumentId(documentId);
        documentSubmitResult.setUploadId(response.getSampleID());

        if (response.getMinedData() != null) {
            String extractedData = toExtractedData(id, response.getMinedData());
            documentSubmitResult.setExtractedData(extractedData);
        }

        final ZenidWebUploadSampleResponse.StateEnum state = response.getState();
        if (state == ZenidWebUploadSampleResponse.StateEnum.DONE) {
            logger.debug("Document upload of {} is done in ZenID, {}", uploadContext, id);
            if (documentSubmitResult.getExtractedData() == null) {
                logger.info("No data extracted from {} in ZenID, defaulting to empty json data, {}", uploadContext, id);
                documentSubmitResult.setExtractedData(DocumentSubmitResult.NO_DATA_EXTRACTED);
            }
        } else if (state == ZenidWebUploadSampleResponse.StateEnum.NOTDONE) {
            logger.debug("Document upload of {} is still in progress in ZenID, {}", uploadContext, id);
        } else if (state == ZenidWebUploadSampleResponse.StateEnum.REJECTED) {
            logger.debug("Document upload of {} is rejected in ZenID, {}, {}", uploadContext, response.getErrorText(), id);
            documentSubmitResult.setRejectReason(response.getErrorText());
        } else {
            throw new DocumentVerificationException(String.format("Document upload of %s failed in ZenID: %s, %s", uploadContext, state, id));
        }

        if (response.getErrorCode() != null) {
            documentSubmitResult.setErrorDetail("ZenID error: " + response.getErrorCode() +
                    (response.getErrorText() != null ? ", " + response.getErrorText() : ""));
        }
        return documentSubmitResult;
    }

    private void checkAdditionalValidations(OwnerId id,
                                            DocumentType documentType,
                                            CardSide cardSide,
                                            DocumentSubmitResult documentSubmitResult,
                                            ZenidSharedMineAllResult minedData) {
        if (DocumentType.SELFIE_PHOTO.equals(documentType)) {
            logger.debug("Not performing additional validations for selfie photo");
            return;
        }
        DocumentType zenIdDocType = minedData.getDocumentRole() == null ?
                null : toDocumentType(minedData.getDocumentRole());
        if (documentType != zenIdDocType) {
            logger.info("Received different document type {} ({}) than expected {} from ZenID, {}",
                    zenIdDocType, minedData.getDocumentRole(), documentType, id);
            documentSubmitResult.setRejectReason(
                    String.format("Different document type %s than expected %s", zenIdDocType, documentType)
            );
        }
        if (documentSubmitResult.getRejectReason() == null) {
            CardSide zenIdCardSide = minedData.getPageCode() == null ?
                    null : toCardSide(minedData.getPageCode());
            if (zenIdCardSide == null && cardSide != null) {
                documentSubmitResult.setRejectReason(String.format("Not recognized document side %s", cardSide));
            } else if (zenIdCardSide != null && cardSide != null && cardSide != zenIdCardSide) {
                documentSubmitResult.setRejectReason(
                        String.format("Different document side %s than expected %s", zenIdCardSide, cardSide)
                );
            }
        }        
    }
    
    private void checkForMinedPhoto(OwnerId id,
                                    String documentId,
                                    DocumentsSubmitResult result,
                                    ZenidSharedMineAllResult minedData) {
        // Photo hash of the person is optionally present at /MinedData/Photo/ImageData/ImageHash
        ZenidSharedMinedPhoto photo = minedData.getPhoto();
        if (photo != null && photo.getImageData() != null && photo.getImageData().getImageHash() != null) {
            logger.info("Extracted a photoId from submitted documentId={} to ZenID, {}", documentId, id);
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

    private DocumentsVerificationResult toResult(OwnerId id, ZenidWebInvestigateResponse response, List<String> knownSampleIds)
            throws DocumentVerificationException {
        DocumentsVerificationResult result = new DocumentsVerificationResult();
        result.setVerificationId(String.valueOf(response.getInvestigationID()));

        if (response.getErrorCode() != null) {
            result.setErrorDetail("ZenID error: " + response.getErrorCode() +
                    (response.getErrorText() != null ? ", " + response.getErrorText() : ""));
        } else {
            Map<String, List<ZenidWebInvestigationValidatorResponse>> sampleIdsValidations = new HashMap<>();
            // Only sampleIds with failed validations are in the response, prefill with all known sampleIds
            knownSampleIds.forEach(sampleId -> sampleIdsValidations.put(sampleId, new ArrayList<>()));
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
                            sampleIdsValidations.computeIfAbsent(issueItem.getSampleID(), sampleId -> new ArrayList<>())
                                    .add(validationData);
                        }
                    }
                }
            }

            List<DocumentVerificationResult> verificationResults = new ArrayList<>();

            String extractedData = toExtractedData(id, response.getMinedData());
            for (var entry : sampleIdsValidations.entrySet()) {
                // TODO Consider using an object instead of simple array (call for a standard json)
                List<ZenidWebInvestigationValidatorResponse> validations = new ArrayList<>(entry.getValue());

                final DocumentVerificationResult verificationResult = new DocumentVerificationResult();
                verificationResult.setExtractedData(extractedData);
                verificationResult.setUploadId(entry.getKey());

                // Find a first failed validation, use its description as the rejected reason for the document
                Optional<ZenidWebInvestigationValidatorResponse> failedValidation = validations.stream()
                        .filter(validation -> !Boolean.TRUE.equals(validation.getOk()))
                        // Sort the validations by difference between the actual score and the accepted score value
                        .max(Comparator.comparingInt((value -> value.getAcceptScore() - value.getScore())));
                if (failedValidation.isPresent()) {
                    final String rejectReason = failedValidation.get().getIssues().get(0).getIssueDescription();
                    logger.debug("Document rejected in ZenID, {}, {}", rejectReason, id);
                    verificationResult.setRejectReason(rejectReason);
                }

                validations.addAll(globalValidations);
                String verificationResultData;
                try {
                    verificationResultData = objectMapper.writeValueAsString(validations);
                } catch (JsonProcessingException e) {
                    throw new DocumentVerificationException("Unexpected error when processing verification result data, " + id, e);
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
                    .filter(value -> StringUtils.isNotBlank(value.getRejectReason()))
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
        result.setOk(value.getOk());
        return result;
    }

    private String toExtractedData(OwnerId id, ZenidSharedMineAllResult minedData) throws DocumentVerificationException {
        try {
            return objectMapper.writeValueAsString(minedData);
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Unexpected error when processing extracted data, " + id, e);
        }
    }

    private DocumentVerificationStatus toStatus(ZenidWebInvestigateResponse.StateEnum stateEnum) {
        return switch (stateEnum) {
            case DONE -> DocumentVerificationStatus.ACCEPTED;
            case ERROR -> DocumentVerificationStatus.FAILED;
            case NOTDONE, OPERATOR -> DocumentVerificationStatus.IN_PROGRESS;
            case REJECTED -> DocumentVerificationStatus.REJECTED;
        };
    }

    private DocumentType toDocumentType(ZenidSharedMineAllResult.DocumentRoleEnum documentRoleEnum) {
        return switch (documentRoleEnum) {
            case DRV -> DocumentType.DRIVING_LICENSE;
            case IDC -> DocumentType.ID_CARD;
            case PAS -> DocumentType.PASSPORT;
            default -> DocumentType.UNKNOWN;
        };
    }

    @Nullable
    private CardSide toCardSide(@Nullable ZenidSharedMineAllResult.PageCodeEnum pageCodeEnum) {
        if (pageCodeEnum == null) {
            return null;
        }
        return switch (pageCodeEnum) {
            case F -> CardSide.FRONT;
            case B -> CardSide.BACK;
        };
    }

    private static void handleLicenceError(final Enum<?> errorCode, final String errorText) throws RemoteCommunicationException {
        if (errorCode != null && INTERNAL_SERVER_ERROR.equals(errorCode.name()) && StringUtils.startsWithIgnoreCase(errorText, LICENSE_INVALID)) {
            throw new RemoteCommunicationException("Out of ZenID licence: " + errorText);
        }
    }
}
