/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.microblink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentDataEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationParsedResponse;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponseParser;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.client.v3.PowerAuthClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.microblink.com/">Microblink</a>.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Slf4j
public class MicroblinkDocumentVerificationProvider implements DocumentVerificationProvider {

    private final RestClient microblinkRestClient;
    private final DocumentVerificationResponseParser responseParser;
    private final Map<String, String> mobileSdkLicenseKeyByPlatform;
    private final PowerAuthClient powerAuthClient;
    private final DocumentDataRepository documentDataRepository;

    public MicroblinkDocumentVerificationProvider(
            RestClient microblinkRestClient,
            DocumentVerificationResponseParser responseParser,
            Map<MicroblinkMobilePlatform, String> mobileSdkLicenseKeys,
            PowerAuthClient powerAuthClient,
            DocumentDataRepository documentDataRepository
    ) {
        this.microblinkRestClient = microblinkRestClient;
        this.responseParser = responseParser;

        mobileSdkLicenseKeyByPlatform = mobileSdkLicenseKeys.entrySet()
                .stream()
                .collect(Collectors.toMap(k -> k.getKey().toString().toLowerCase(), Map.Entry::getValue));

        this.powerAuthClient = powerAuthClient;
        this.documentDataRepository = documentDataRepository;
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId ownerId, DocumentVerificationEntity document) {
        logger.info("action: checkDocumentUpload, state: unsupported, provider: microblink, ownerId: {}", ownerId);
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Microblink provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> submittedDocuments) {
        final var documentIds = submittedDocuments.stream()
                .map(SubmittedDocument::getDocumentId)
                .toList();

        logger.info("action: submitDocuments, state: initiated, provider: microblink, ownerId: {}, documentIds: {}", ownerId, documentIds);

        final var documentsData = new ArrayList<DocumentDataEntity>();
        final var documentResults = new ArrayList<DocumentSubmitResult>();

        for (final var submittedDocument : submittedDocuments) {
            final var documentData = new DocumentDataEntity();
            documentData.setId(UUID.randomUUID().toString());
            documentData.setData(submittedDocument.getPhoto().getData());

            documentsData.add(documentData);

            final var documentResult = new DocumentSubmitResult();
            documentResult.setDocumentId(submittedDocument.getDocumentId());
            documentResult.setUploadId(documentData.getId());
            // Setting the extracted data to an empty JSON object is important here. Leaving it null will prevent
            // the document from being passed to the next step and verified. See DocumentProcessingService::processDocsSubmitResults.
            documentResult.setExtractedData(DocumentSubmitResult.NO_DATA_EXTRACTED);

            documentResults.add(documentResult);
        }

        documentDataRepository.saveAll(documentsData);

        final var result = new DocumentsSubmitResult();
        result.setResults(documentResults);
        result.setExtractedPhotoId(UUID.randomUUID().toString());

        logger.info("action: submitDocuments, state: succeeded, provider: microblink, result: {}", result);
        return result;
    }

    @Override
    public boolean shouldStoreSelfie() {
        logger.info("action: shouldStoreSelfie, state: succeeded, provider: microblink, result: false");
        return false;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId ownerId, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        try {
            final var verificationId = UUID.randomUUID().toString();

            logger.info(
                    "action: verifyDocuments, state: initiated, provider: microblink, ownerId: {}, uploadIds: {}, verificationId: {}",
                    ownerId,
                    uploadIds,
                    verificationId
            );

            final var result = verifyDocuments(uploadIds, verificationId);

            logger.info("action: verifyDocuments, state: succeeded, provider: microblink, result: {}", result);
            return result;
        } catch (final RemoteCommunicationException | DocumentVerificationException e) {
            logger.info("action: verifyDocuments, state: failed, provider: microblink, error: {}", e.getMessage());
            throw e;
        }
    }

    private DocumentsVerificationResult verifyDocuments(List<String> uploadIds, String verificationId) throws DocumentVerificationException, RemoteCommunicationException {
        final var documentsData = StreamSupport.stream(documentDataRepository.findAllById(uploadIds).spliterator(), false)
                .toList();

        if (CollectionUtils.isEmpty(documentsData)) {
            throw new DocumentVerificationException("Verification data without documents");
        }

        final var documentsByTypeAndSide = groupDocumentsByTypeAndSide(documentsData);

        final var facePhotoExtractionDocumentType = DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO.stream()
                .filter(documentsByTypeAndSide::containsKey)
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException("No document of preferred type for face photo extraction found"));

        logger.info("Document type for face photo extraction: {}", facePhotoExtractionDocumentType);

        final var documentCheckResults = new ArrayList<String>();
        final var documentsCrosscheckData = new HashMap<String, List<String>>();
        final var documentVerificationResults = new ArrayList<DocumentVerificationResult>();

        for (final var documentsOfSameType : documentsByTypeAndSide.entrySet()) {
            final var documentType = documentsOfSameType.getKey();
            final var documentFront = documentsOfSameType.getValue().getOrDefault(CardSide.FRONT, null);
            final var documentBack = documentsOfSameType.getValue().getOrDefault(CardSide.BACK, null);

            final var parsedResponse = sendApiRequest(documentFront, documentBack);

            documentCheckResults.add(parsedResponse.verification().result());

            final var extractedType = parsedResponse.extraction().classInfo().type();
            verifyDocumentType(documentType, extractedType);

            final var overallExtraction = parsedResponse.extraction().overall();
            putDocumentCrosscheckData(overallExtraction, documentsCrosscheckData);

// TODO: store face photo into DB
//            if (documentType == facePhotoExtractionDocumentType) {
//                final var faceImageBase64 = Optional.ofNullable(parsedResponse.images())
//                        .orElse(Collections.emptyList())
//                        .stream()
//                        .filter(image -> image.name().equals("FaceImage"))
//                        .findFirst()
//                        .map(DocumentVerificationParsedResponse.Image::base64)
//                        .orElseThrow(() -> new DocumentVerificationException("Face image not extracted from document of type %s".formatted(documentType)));
//
//                final var facePhotoId = Optional.ofNullable(documentFront)
//                        .map(i -> i.getDocumentVerification().getPhotoId())
//                        .orElse(documentBack.getDocumentVerification().getPhotoId());
//
//
//                photoCache.put(facePhotoId, faceImageBase64);
//                logger.info("Face photo stored in cache with id: {}", facePhotoId);
//            }

            final var documentsVerificationResult = buildDocumentVerificationResults(documentFront.getId(), documentBack.getId(), parsedResponse);
            documentVerificationResults.addAll(documentsVerificationResult);
        }

        verifyDocumentsCrosscheck(documentsCrosscheckData);

        final var allChecksPassed = documentCheckResults.stream()
                .allMatch("Pass"::equalsIgnoreCase);

        final var result = new DocumentsVerificationResult();
        result.setVerificationId(verificationId);
        result.setStatus(allChecksPassed ? DocumentVerificationStatus.ACCEPTED : DocumentVerificationStatus.REJECTED);
        result.setResults(documentVerificationResults);
        return result;
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId ownerId, String verificationId) {
        logger.info("action: getVerificationResult, state: unsupported, provider: microblink, ownerId: {}, verificationId: {}", ownerId, verificationId);
        throw new UnsupportedOperationException("Method getVerificationResult is not supported by Microblink provider.");
    }

    @Override
    public Image getPhoto(String photoId) throws DocumentVerificationException {
        try {
            logger.info("action: getPhoto, state: initiated, provider: microblink, photoId: {}", photoId);

            // TODO: Load image from DB
            throw new DocumentVerificationException("Not implemented yet");
//            final var photoBase64 = Optional.ofNullable(photoCache.get(photoId, String.class))
//                    .orElseThrow(() -> new DocumentVerificationException("Photo with id %s not found".formatted(photoId)));
//
//            final var image = Image.builder()
//                    .filename("FaceImage.jpg")
//                    .data(Base64.getDecoder().decode(photoBase64))
//                    .build();
//
//            logger.info("action: getPhoto, state: succeeded, provider: microblink");
//            return image;
        } catch (final DocumentVerificationException e) {
            logger.info("action: getPhoto, state: failed, provider: microblink, error: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void cleanupDocuments(OwnerId ownerId, List<String> uploadIds) {
        logger.info("action: cleanupDocuments, state: initiated, provider: microblink, ownerId: {}, uploadIds: {}", ownerId, String.join(",", uploadIds));

        // TODO: Remove documents
        throw new NotImplementedException("Not implemented yet");

//        final var activationId = ownerId.getActivationId();
//        final var verificationData = verificationDataCache.get(activationId, MicroblinkVerificationData.class);
//        logger.debug("Record in cache before cleanup: {}", verificationData);
//
//        if (verificationData != null) {
//            final var documents = verificationData.documents().stream()
//                    .filter(d -> !uploadIds.contains(d.uploadId()))
//                    .toList();
//
//            final var updatedVerificationData = verificationData.toBuilder()
//                    .documents(documents)
//                    .build();
//
//            verificationDataCache.put(activationId, updatedVerificationData);
//            logger.debug("Record in cache after cleanup: {}", updatedVerificationData);
//        }
//
//        logger.info("action: cleanupDocuments, state: succeeded, provider: microblink");
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) {
        logger.info("action: parseRejectionReasons, state: initiated, provider: microblink, documentResultId: {}", docResult.getId());

        final var result = List.of(docResult.getVerificationResult());

        logger.info("action: parseRejectionReasons, state: succeeded, provider: microblink, rejectionReasons: {}", String.join(",", result));
        return result;
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId ownerId, Map<String, String> initAttributes) throws RemoteCommunicationException {
        logger.info("action: initVerificationSdk, state: initiated, provider: microblink, ownerId: {}, initAttributes: {}", ownerId, initAttributes);

        try {
            final var activationId = ownerId.getActivationId();
            var mobilePlatform = initAttributes.getOrDefault("platform", null);

            if (mobilePlatform == null || !mobileSdkLicenseKeyByPlatform.containsKey(mobilePlatform)) {
                mobilePlatform = fetchMobilePlatform(activationId);
            }

            final var sdkInfo = Optional.ofNullable(mobilePlatform)
                    .map(platform -> mobileSdkLicenseKeyByPlatform.getOrDefault(platform, null))
                    .map(licenseKey -> new VerificationSdkInfo(Map.of("license-key", licenseKey)))
                    .orElseGet(VerificationSdkInfo::new);

            logger.info("action: initVerificationSdk, state: succeeded, provider: microblink, sdkInfo: {}", MicroblinkLogSanitizationUtils.sanitizeSdkInfo(sdkInfo));
            return sdkInfo;
        } catch (final RemoteCommunicationException e) {
            logger.info("action: initVerificationSdk, state: failed, provider: microblink, error: {}", e.getMessage());
            throw e;
        }
    }

    private static DocumentVerificationImageSource buildImageSource(final byte[] data) {
        final var imageBase64 = Base64.getEncoder().encodeToString(data);

        final var imageSource = new DocumentVerificationImageSource();
        imageSource.setBase64(imageBase64);
        return imageSource;
    }

    private static DocumentVerificationRequest buildRequest(final DocumentDataEntity frontDocument, final DocumentDataEntity backDocument) {
        final var frontImageSource = buildImageSource(frontDocument.getData());
        final var backImageSource = buildImageSource(backDocument.getData());

        final var options = new DocumentVerificationProcessingOptions();
        options.setReturnImageFormat(ImageFormat.JPG);
        options.setReturnFaceImage(true);

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setOptions(options);
        request.setUseCase(useCase);
        return request;
    }

    private DocumentVerificationParsedResponse sendApiRequest(
            final DocumentDataEntity frontDocument,
            final DocumentDataEntity backDocument
    ) throws DocumentVerificationException, RemoteCommunicationException {
        logger.info("Sending request to Microblink REST API for documents: {}, {}", frontDocument, backDocument);

        try {
            final var request = buildRequest(frontDocument, backDocument);
            logger.debug("Request body: {}", (Supplier<String>) () -> MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationRequest(request));

            final var response = microblinkRestClient.post("/api/v2/docver", request, new ParameterizedTypeReference<String>() {});
            final var body = Optional.ofNullable(response)
                    .map(HttpEntity::getBody)
                    .orElseThrow(() -> new DocumentVerificationException("Response body is empty"));

            final var parsedResponse =  responseParser.parseResponse(body);
            logger.info("Response traceId: {}, verificationResult: {}", parsedResponse.runtime().traceId(), parsedResponse.verificationJson());
            logger.debug("Response body: {}", (Supplier<String>) () -> MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationResponseJson(body));

            return parsedResponse;
        } catch (final RestClientException e) {
            throw new RemoteCommunicationException(
                    "Failed REST API call to Microblink, statusCode=%s, responseBody='%s'".formatted(
                            e.getStatusCode(),
                            e.getResponse()
                    ),
                    e
            );
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Failed to parse Microblink API response", e);
        }
    }

    private static Map<DocumentType, Map<CardSide, DocumentDataEntity>> groupDocumentsByTypeAndSide(
            final List<DocumentDataEntity> documentsData
    ) throws DocumentVerificationException {
        final var documentsByTypeAndSide = new EnumMap<DocumentType, Map<CardSide, DocumentDataEntity>>(DocumentType.class);
        for (final var documentData : documentsData) {
            final var documentVerification = Optional.ofNullable(documentData.getDocumentVerification())
                    .orElseThrow(() -> new DocumentVerificationException("Missing document verification for document data id: %s".formatted(documentData.getId())));

            final var documentsOfSameType = documentsByTypeAndSide.computeIfAbsent(documentVerification.getType(), k -> new EnumMap<>(CardSide.class));

            final var documentOfSameSide = documentsOfSameType.getOrDefault(documentVerification.getSide(), null);
            if (documentOfSameSide != null) {
                throw new DocumentVerificationException(
                        "Multiple documents of type %s and side %s found. Document data ids: %s".formatted(
                                documentVerification.getType(),
                                documentVerification.getSide(),
                                List.of(documentOfSameSide.getId(), documentData.getId())
                        )
                );
            }

            documentsOfSameType.put(documentVerification.getSide(), documentData);
        }

        return documentsByTypeAndSide;
    }

    private static void putDocumentCrosscheckData(final List<DocumentVerificationParsedResponse.Result> documentExtractedData, final Map<String, List<String>> documentsCrosscheckData) throws DocumentVerificationException {
        final var firstName = documentExtractedData.stream()
                .filter(r -> "FirstName".equals(r.field()))
                .findFirst()
                .map(DocumentVerificationParsedResponse.Result::value)
                .map(String::toLowerCase)
                .orElseThrow(() -> new DocumentVerificationException("Field FirstName not found in extracted data"));

        final var lastName = documentExtractedData.stream()
                .filter(r -> "LastName".equals(r.field()))
                .findFirst()
                .map(DocumentVerificationParsedResponse.Result::value)
                .map(String::toLowerCase)
                .orElseThrow(() -> new DocumentVerificationException("Field LastName not found in extracted data"));

        final var dateOfBirthResult = documentExtractedData.stream()
                .filter(r -> "DateOfBirth".equals(r.field()))
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException("Field DateOfBirth not found in extracted data"));

        final var dateOfBirth = parseDate(dateOfBirthResult).toString();

        documentsCrosscheckData.computeIfAbsent("FirstName", k -> new ArrayList<>()).add(firstName);
        documentsCrosscheckData.computeIfAbsent("LastName", k -> new ArrayList<>()).add(lastName);
        documentsCrosscheckData.computeIfAbsent("DateOfBirth", k -> new ArrayList<>()).add(dateOfBirth);
    }

    private static LocalDate parseDate(final DocumentVerificationParsedResponse.Result result) throws DocumentVerificationException {
        final var year = Optional.ofNullable(result.year())
                .orElseThrow(() -> new DocumentVerificationException("Year of field DateOfBirth was not extracted"));

        final var month = Optional.ofNullable(result.month())
                .orElseThrow(() -> new DocumentVerificationException("Month of field DateOfBirth was not extracted"));

        final var day = Optional.ofNullable(result.day())
                .orElseThrow(() -> new DocumentVerificationException("Day of field DateOfBirth was not extracted"));

        return LocalDate.of(year, month, day);
    }

    private List<DocumentVerificationResult> buildDocumentVerificationResults(
            final String documentFrontUploadId,
            final String documentBackUploadId,
            final DocumentVerificationParsedResponse response
    ) {
        final var responseJson = response.responseJson();

        final var documentFrontVerificationResult = new DocumentVerificationResult();
        documentFrontVerificationResult.setUploadId(documentFrontUploadId);
        documentFrontVerificationResult.setVerificationResult(responseJson);
        documentFrontVerificationResult.setExtractedData(response.extractionFrontJson());

        final var documentBackVerificationResult = new DocumentVerificationResult();
        documentBackVerificationResult.setUploadId(documentBackUploadId);
        documentBackVerificationResult.setVerificationResult(responseJson);
        documentBackVerificationResult.setExtractedData(response.extractionBackJson());

        return List.of(documentFrontVerificationResult, documentBackVerificationResult);
    }

    private static void verifyDocumentsCrosscheck(final Map<String, List<String>> documentsCrosscheckData) throws DocumentVerificationException {
        for (final var extractedDataEntry : documentsCrosscheckData.entrySet()) {
            final var fieldName = extractedDataEntry.getKey();
            final var extractedValues = extractedDataEntry.getValue();

            final var distinctValuesCount = extractedValues.stream().distinct().count();
            if (distinctValuesCount != 1) {
                throw new DocumentVerificationException("Cross-check of extracted data failed on field %s".formatted(fieldName));
            }
        }
    }

    private static void verifyDocumentType(final DocumentType claimedDocumentType, final String extractedType) throws DocumentVerificationException {
        final var extractedDocumentType = switch (extractedType) {
            case "Id" -> DocumentType.ID_CARD;
            case "Passport" -> DocumentType.PASSPORT;
            case "Dl" -> DocumentType.DRIVING_LICENSE;
            default -> throw new DocumentVerificationException("Unsupported extracted document type %s".formatted(extractedType));
        };

        if (extractedDocumentType != claimedDocumentType) {
            throw new DocumentVerificationException(
                    "Extracted document type %s does not match claimed type %s".formatted(
                            extractedDocumentType,
                            claimedDocumentType
                    )
            );
        }

    }

    private String fetchMobilePlatform(final String activationId) throws RemoteCommunicationException {
        try {
            logger.debug("Fetching mobile platform for activationId: {}", activationId);

            final var response = powerAuthClient.getActivationStatus(activationId);
            final var platform = response.getPlatform();

            logger.debug("Fetched mobile platform: {}", platform);
            return platform;
        } catch (PowerAuthClientException e) {
            throw new RemoteCommunicationException("Error when fetching mobile platform", e);
        }
    }
}
