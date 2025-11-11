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
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationParsedResponse;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponseParser;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.microblink.com/">Microblink</a>.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Slf4j
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "microblink")
@Component
public class MicroblinkDocumentVerificationProvider implements DocumentVerificationProvider {

    private final Cache verificationDataCache;
    private final Cache photoCache;
    private final RestClient restClient;
    private final DocumentVerificationResponseParser responseParser;

    @Autowired
    public MicroblinkDocumentVerificationProvider(
            @Qualifier("microblinkCacheManager") CacheManager cacheManager,
            @Qualifier("microblinkRestClient") RestClient restClient,
            @Qualifier("microblinkDocumentVerificationResponseParser") DocumentVerificationResponseParser responseParser
    ) {
        this.verificationDataCache = cacheManager.getCache(MicroblinkConfigProperties.DOCUMENTS_CACHE_NAME);
        this.photoCache = cacheManager.getCache(MicroblinkConfigProperties.PHOTO_CACHE_NAME);
        this.restClient = restClient;
        this.responseParser = responseParser;
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) {
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Microblink provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> submittedDocuments) {
        final var activationId = ownerId.getActivationId();
        logger.info("Submitting documents {} for activationId {}", submittedDocuments.stream().map(SubmittedDocument::getDocumentId).toList(), activationId);

        var microblinkVerificationData = verificationDataCache.get(activationId, MicroblinkVerificationData.class);

        final var addedDocuments = submittedDocuments.stream()
                .map(MicroblinkDocumentVerificationProvider::buildMicroblinkVerificationDocument)
                .toList();

        if (microblinkVerificationData == null) {
            microblinkVerificationData = MicroblinkVerificationData.builder()
                    .documents(addedDocuments)
                    .facePhotoId(UUID.randomUUID().toString())
                    .build();
        } else {
            final var submittedDocumentIds = submittedDocuments.stream()
                    .map(SubmittedDocument::getDocumentId)
                    .collect(Collectors.toSet());

            var newDocuments = new ArrayList<>(microblinkVerificationData.documents()
                    .stream()
                    .filter(document -> !submittedDocumentIds.contains(document.documentId()))
                    .toList());

            newDocuments.addAll(addedDocuments);

            microblinkVerificationData = microblinkVerificationData.toBuilder()
                    .documents(newDocuments)
                    .build();
        }

        verificationDataCache.put(ownerId.getActivationId(), microblinkVerificationData);

        final var results = addedDocuments.stream()
                .map(document -> {
                    final var result = new DocumentSubmitResult();
                    result.setDocumentId(document.documentId());
                    result.setUploadId(document.uploadId());
                    return result;
                })
                .toList();

        final var result = new DocumentsSubmitResult();
        result.setResults(results);
        result.setExtractedPhotoId(microblinkVerificationData.facePhotoId());
        return result;
    }

    @Override
    public boolean shouldStoreSelfie() {
        return false;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId ownerId, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        final var activationId = ownerId.getActivationId();
        logger.info("Verifying documents with uploadIds {} for activationId {}", String.join(",", uploadIds), activationId);

        final var verificationData = Optional.ofNullable(verificationDataCache.get(activationId, MicroblinkVerificationData.class))
                .orElseThrow(() -> new DocumentVerificationException("Verification data not found"));

        final var allDocuments = verificationData.documents();
        if (CollectionUtils.isEmpty(allDocuments)) {
            throw new DocumentVerificationException("Verification data without documents");
        }

        final var documents = filterDocumentsByUploadId(allDocuments, uploadIds);

        final var documentsByTypeAndSide = groupDocumentsByTypeAndSide(documents);
        final var facePhotoExtractionDocumentType = DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO.stream()
                .filter(documentsByTypeAndSide::containsKey)
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException("No document of preferred type for face photo extraction found"));

        final var documentCheckResults = new ArrayList<String>();
        final var documentsCrosscheckData = new HashMap<String, List<String>>();
        final var documentVerificationResults = new ArrayList<DocumentVerificationResult>();

        for (final var documentsOfSameType : documentsByTypeAndSide.entrySet()) {
            final var documentType = documentsOfSameType.getKey();
            final var documentFront = findDocumentBySide(documentsOfSameType, CardSide.FRONT);
            final var documentBack = findDocumentBySide(documentsOfSameType, CardSide.BACK);

            final var parsedResponse = sendApiRequest(documentFront, documentBack);

            documentCheckResults.add(parsedResponse.verification().result());

            final var extractedType = parsedResponse.extraction().classInfo().type();
            verifyDocumentType(documentType, extractedType);

            final var overallExtraction = parsedResponse.extraction().overall();
            putDocumentCrosscheckData(overallExtraction, documentsCrosscheckData);

            if (documentType == facePhotoExtractionDocumentType) {
                final var faceImageBase64 = Optional.ofNullable(parsedResponse.images())
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(image -> image.name().equals("FaceImage"))
                        .findFirst()
                        .map(DocumentVerificationParsedResponse.Image::base64)
                        .orElseThrow(() -> new DocumentVerificationException("Face image not extracted from document of type %s".formatted(documentType)));

                photoCache.put(verificationData.facePhotoId(), faceImageBase64);
            }

            final var documentsVerificationResult = buildDocumentVerificationResults(documentFront.uploadId(), documentBack.uploadId(), parsedResponse);
            documentVerificationResults.addAll(documentsVerificationResult);
        }

        verifyDocumentsCrosscheck(documentsCrosscheckData);

        final var allChecksPassed = documentCheckResults.stream()
                .allMatch("Pass"::equalsIgnoreCase);

        final var result = new DocumentsVerificationResult();
        result.setStatus(allChecksPassed ? DocumentVerificationStatus.ACCEPTED : DocumentVerificationStatus.REJECTED);
        result.setResults(documentVerificationResults);
        return result;
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) {
        throw new UnsupportedOperationException("Method getVerificationResult is not supported by Microblink provider.");
    }

    @Override
    public Image getPhoto(String photoId) throws DocumentVerificationException {
        final var photoBase64 = Optional.ofNullable(photoCache.get(photoId, String.class))
                .orElseThrow(() -> new DocumentVerificationException("Photo with id %s not found".formatted(photoId)));

        return Image.builder()
                .filename("FaceImage.jpg")
                .data(Base64.getDecoder().decode(photoBase64))
                .build();
    }

    @Override
    public void cleanupDocuments(OwnerId ownerId, List<String> uploadIds) {
        final var activationId = ownerId.getActivationId();
        logger.info("Cleaning up documents with uploadIds {} for activationId {}", String.join(",", uploadIds), activationId);

        final var verificationData = verificationDataCache.get(activationId, MicroblinkVerificationData.class);

        if (verificationData != null) {
            final var documents = verificationData.documents().stream()
                    .filter(d -> !uploadIds.contains(d.uploadId()))
                    .toList();

            final var updatedVerificationData = verificationData.toBuilder()
                    .documents(documents)
                    .build();

            verificationDataCache.put(activationId, updatedVerificationData);
        }
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) {
        return List.of(docResult.getVerificationResult());
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) {
        return new VerificationSdkInfo();
    }

    private static DocumentVerificationImageSource buildImageSource(final Image image) {
        final var imageBase64 = Base64.getEncoder().encodeToString(image.getData());

        final var imageSource = new DocumentVerificationImageSource();
        imageSource.setBase64(imageBase64);
        return imageSource;
    }

    private static DocumentVerificationRequest buildRequest(final MicroblinkVerificationData.Document frontDocument, final MicroblinkVerificationData.Document backDocument) {
        final var frontImageSource = buildImageSource(frontDocument.image());
        final var backImageSource = buildImageSource(backDocument.image());

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

    private static MicroblinkVerificationData.Document buildMicroblinkVerificationDocument(final SubmittedDocument submittedDocument) {
        return MicroblinkVerificationData.Document.builder()
                .documentId(submittedDocument.getDocumentId())
                .uploadId(UUID.randomUUID().toString())
                .type(submittedDocument.getType())
                .side(submittedDocument.getSide())
                .image(submittedDocument.getPhoto())
                .build();
    }

    private DocumentVerificationParsedResponse sendApiRequest(
            final MicroblinkVerificationData.Document frontDocument,
            final MicroblinkVerificationData.Document backDocument
    ) throws DocumentVerificationException, RemoteCommunicationException {
        try {
            final var request = buildRequest(frontDocument, backDocument);

            final var response = restClient.post("/api/v2/docver", request, new ParameterizedTypeReference<String>() {});
            final var body = Optional.ofNullable(response)
                    .map(HttpEntity::getBody)
                    .orElseThrow(() -> new DocumentVerificationException("Response body is empty"));

            return responseParser.parseResponse(body);
        } catch (final RestClientException e) {
            throw new RemoteCommunicationException(
                    "Failed REST API call to Microblink, statusCode=%s, responseBody='%s'".formatted(
                            e.getStatusCode(),
                            e.getResponse()
                    ),
                    e
            );
        } catch (JsonProcessingException e) {
            throw new DocumentVerificationException("Failed to parse Microblink API response");
        }
    }

    private static List<MicroblinkVerificationData.Document> filterDocumentsByUploadId(
            final List<MicroblinkVerificationData.Document> documents,
            final List<String> uploadIds
    ) throws DocumentVerificationException {
        final var uploadIdSet = new HashSet<>(uploadIds);

        final var filteredDocuments = new ArrayList<MicroblinkVerificationData.Document>();
        for (final var document : documents) {
            final var uploadId = document.uploadId();

            if (uploadIdSet.remove(uploadId)) {
                filteredDocuments.add(document);
            }
        }

        if (!uploadIdSet.isEmpty()) {
            throw new DocumentVerificationException("Documents with uploadIds %s not found".formatted(String.join(",", uploadIdSet)));
        }

        return filteredDocuments;
    }

    private static Map<DocumentType, Map<CardSide, MicroblinkVerificationData.Document>> groupDocumentsByTypeAndSide(
            final List<MicroblinkVerificationData.Document> documents
    ) throws DocumentVerificationException {
        final var documentsByTypeAndSide = new EnumMap<DocumentType, Map<CardSide, MicroblinkVerificationData.Document>>(DocumentType.class);
        for (final var document : documents) {
            final var documentsOfSameType = documentsByTypeAndSide.computeIfAbsent(document.type(), k -> new EnumMap<>(CardSide.class));

            final var documentOfSameSide = documentsOfSameType.getOrDefault(document.side(), null);
            if (documentOfSameSide != null) {
                throw new DocumentVerificationException(
                        "Multiple documents of type %s and side %s found. Document ids: %s".formatted(
                                document.type(),
                                document.side(),
                                String.join(",", List.of(documentOfSameSide.documentId(), document.documentId()))
                        )
                );
            }

            documentsOfSameType.put(document.side(), document);
        }

        return documentsByTypeAndSide;
    }

    private static MicroblinkVerificationData.Document findDocumentBySide(
            final Map.Entry<DocumentType, Map<CardSide, MicroblinkVerificationData.Document>> documentsOfSameType,
            final CardSide side
    ) throws DocumentVerificationException {
        final var documentType = documentsOfSameType.getKey();
        final var document = documentsOfSameType.getValue().getOrDefault(side, null);

        return Optional.ofNullable(document)
                .orElseThrow(() -> new DocumentVerificationException("Document of type %s and side %s not found".formatted(documentType, side)));
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
        final var verificationJson = response.verificationJson();

        final var documentFrontVerificationResult = new DocumentVerificationResult();
        documentFrontVerificationResult.setUploadId(documentFrontUploadId);
        documentFrontVerificationResult.setVerificationResult(verificationJson);
        documentFrontVerificationResult.setExtractedData(response.extractionFrontJson());

        final var documentBackVerificationResult = new DocumentVerificationResult();
        documentBackVerificationResult.setUploadId(documentBackUploadId);
        documentBackVerificationResult.setVerificationResult(verificationJson);
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
}
