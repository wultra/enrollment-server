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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.microblink.com/">Microblink</a>.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "microblink")
@Component
public class MicroblinkDocumentVerificationProvider implements DocumentVerificationProvider {

    private final Cache<String, MicroblinkVerificationData> verificationDataCache;
    private final Cache<String, String> photoCache;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public MicroblinkDocumentVerificationProvider(
            @Qualifier("microblinkDocumentsCache") Cache<String, MicroblinkVerificationData> verificationDataCache,
            @Qualifier("microblinkPhotoCache") Cache<String, String> photoCache,
            @Qualifier("microblinkRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.verificationDataCache = verificationDataCache;
        this.photoCache = photoCache;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) {
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Microblink provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> documents) {
        final var microblinkVerificationData = buildMicroblinkVerificationData(documents);
        verificationDataCache.put(ownerId.getActivationId(), microblinkVerificationData);

        final var results = microblinkVerificationData.documents().stream()
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

        final var verificationData = Optional.ofNullable(verificationDataCache.getIfPresent(activationId))
                .orElseThrow(() -> new DocumentVerificationException("Verification data not found for activationId %s".formatted(activationId)));

        final var allDocuments = verificationData.documents();
        if (CollectionUtils.isEmpty(allDocuments)) {
            throw new DocumentVerificationException("No uploaded documents for activationId %s".formatted(activationId));
        }

        final var documents = filterDocumentsByUploadId(allDocuments, uploadIds, activationId);

        final var documentsByTypeAndSide = groupDocumentsByTypeAndSide(documents, activationId);
        final var facePhotoExtractionDocumentType = DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO.stream()
                .filter(documentsByTypeAndSide::containsKey)
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException("No document of preferred type for face photo extraction found for activationId %s".formatted(activationId)));

        final var documentCheckResults = new ArrayList<CheckResult>();
        final var documentsCrosscheckData = new HashMap<String, List<String>>();
        final var documentVerificationResults = new ArrayList<DocumentVerificationResult>();

        for (final var documentsOfSameType : documentsByTypeAndSide.entrySet()) {
            final var documentType = documentsOfSameType.getKey();
            final var documentFront = findDocumentBySide(documentsOfSameType, CardSide.FRONT, activationId);
            final var documentBack = findDocumentBySide(documentsOfSameType, CardSide.BACK, activationId);

            final var apiResponse = sendApiRequest(ownerId, documentFront, documentBack);
            documentCheckResults.add(apiResponse.getVerification().getResult());

            // check extracted document type
            final var extractedType = apiResponse.getExtraction().getClassInfo().getType();
            verifyDocumentType(documentType, extractedType, activationId);

            final var overallExtraction = apiResponse.getExtraction().getOverall();
            putDocumentCrosscheckData(overallExtraction, documentsCrosscheckData);

            if (documentType == facePhotoExtractionDocumentType) {
                final var faceImageBase64 = apiResponse.getImages()
                        .stream()
                        .filter(image -> image.getName().equals("FaceImage"))
                        .findFirst()
                        .map(ImageResult::getBase64)
                        .orElseThrow();

                photoCache.put(verificationData.facePhotoId(), faceImageBase64);
            }

            final var documentsVerificationResult = buildDocumentVerificationResults(documentFront.uploadId(), documentBack.uploadId(), apiResponse, activationId);
            documentVerificationResults.addAll(documentsVerificationResult);
        }

        verifyDocumentsCrosscheck(documentsCrosscheckData, activationId);

        final var allChecksPassed = documentCheckResults.stream()
                .allMatch(r -> r == CheckResult.PASS);

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
    public Image getPhoto(String photoId) {
        final var photoBase64 = Optional.ofNullable(photoCache.getIfPresent(photoId))
                .orElseThrow();

        return Image.builder()
                .filename("FaceImage.jpg")
                .data(Base64.getDecoder().decode(photoBase64))
                .build();
    }

    @Override
    public void cleanupDocuments(OwnerId ownerId, List<String> uploadIds) {
        final var verificationData = verificationDataCache.getIfPresent(ownerId.getActivationId());

        if (verificationData != null) {
            final var documents = verificationData.documents().stream()
                    .filter(d -> !uploadIds.contains(d.uploadId()))
                    .toList();

            final var updatedVerificationData = verificationData.toBuilder()
                    .documents(documents)
                    .build();

            verificationDataCache.put(ownerId.getActivationId(), updatedVerificationData);
        }
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        // TODO
        throw new NotImplementedException();
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

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setUseCase(useCase);
        return request;
    }

    private static MicroblinkVerificationData buildMicroblinkVerificationData(final List<SubmittedDocument> submittedDocuments) {
        final var documents = submittedDocuments.stream()
                .map(r -> MicroblinkVerificationData.Document.builder()
                        .documentId(r.getDocumentId())
                        .uploadId(UUID.randomUUID().toString())
                        .type(r.getType())
                        .side(r.getSide())
                        .image(r.getPhoto())
                        .build())
                .toList();

        return MicroblinkVerificationData.builder()
                .documents(documents)
                .facePhotoId(UUID.randomUUID().toString())
                .build();
    }

    private DocumentVerificationResponse sendApiRequest(final OwnerId ownerId, final MicroblinkVerificationData.Document frontDocument, final MicroblinkVerificationData.Document backDocument) throws DocumentVerificationException, RemoteCommunicationException {
        try {
            final var request = buildRequest(frontDocument, backDocument);

            final var response = restClient.post("/api/v2/docver", request, new ParameterizedTypeReference<DocumentVerificationResponse>() {});
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new DocumentVerificationException("Response body is empty"));
        } catch (final RestClientException e) {
            throw new RemoteCommunicationException(
                    "Failed REST call to verify documents %s in Microblink, statusCode=%s, responseBody='%s', %s".formatted(
                            String.join(",", List.of(frontDocument.uploadId(), backDocument.uploadId())),
                            e.getStatusCode(),
                            e.getResponse(),
                            ownerId
                    ),
                    e);
        }
    }

    private static List<MicroblinkVerificationData.Document> filterDocumentsByUploadId(final List<MicroblinkVerificationData.Document> documents, final List<String> uploadIds, final String activationId) throws DocumentVerificationException {
        final var uploadIdSet = new HashSet<>(uploadIds);

        final var filteredDocuments = new ArrayList<MicroblinkVerificationData.Document>();
        for (final var document : documents) {
            final var uploadId = document.uploadId();

            if (uploadIdSet.remove(uploadId)) {
                filteredDocuments.add(document);
            }
        }

        if (!uploadIdSet.isEmpty()) {
            throw new DocumentVerificationException("Documents with uploadIds %s not found for activationId %s".formatted(String.join(",", uploadIdSet), activationId));
        }

        return filteredDocuments;
    }

    private static Map<DocumentType, Map<CardSide, MicroblinkVerificationData.Document>> groupDocumentsByTypeAndSide(final List<MicroblinkVerificationData.Document> documents, final String activationId) throws DocumentVerificationException {
        final var documentsByTypeAndSide = new HashMap<DocumentType, Map<CardSide, MicroblinkVerificationData.Document>>();
        for (final var document : documents) {
            final var documentsOfSameType = documentsByTypeAndSide.computeIfAbsent(document.type(), k -> new HashMap<>());

            final var documentOfSameSide = documentsOfSameType.getOrDefault(document.side(), null);
            if (documentOfSameSide != null) {
                throw new DocumentVerificationException(
                        "Multiple documents of type %s and side %s found for activationId %s. Document ids: %s".formatted(
                                document.type(),
                                document.side(),
                                activationId,
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
            final CardSide side,
            final String activationId
    ) throws DocumentVerificationException {
        final var documentType = documentsOfSameType.getKey();
        final var document = documentsOfSameType.getValue().getOrDefault(side, null);

        return Optional.ofNullable(document)
                .orElseThrow(() -> new DocumentVerificationException("Document of type %s and side %s not found for activationId %s".formatted(documentType, side, activationId)));
    }

    private static void putDocumentCrosscheckData(final List<Result> documentExtractedData, final Map<String, List<String>> documentsCrosscheckData) {
        final var firstName = documentExtractedData.stream()
                .filter(r -> "FirstName".equals(r.getType()))
                .findFirst()
                .map(i -> (StringResult) i.getResults())
                .map(StringResult::getValue)
                .map(String::toLowerCase)
                .orElseThrow();

        final var lastName = documentExtractedData.stream()
                .filter(r -> "LastName".equals(r.getField()))
                .findFirst()
                .map(i -> (StringResult) i.getResults())
                .map(StringResult::getValue)
                .map(String::toLowerCase)
                .orElseThrow();

        final var dateOfBirth = documentExtractedData.stream()
                .filter(r -> "DateOfBirth".equals(r.getField()))
                .findFirst()
                .map(i -> (DateResult) i.getResults())
                .map(r -> LocalDate.of(r.getYear(), r.getMonth(), r.getDay()))
                .map(LocalDate::toString)
                .orElseThrow();

        documentsCrosscheckData.computeIfAbsent("FirstName", k -> new ArrayList<>()).add(firstName);
        documentsCrosscheckData.computeIfAbsent("LastName", k -> new ArrayList<>()).add(lastName);
        documentsCrosscheckData.computeIfAbsent("DateOfBirth", k -> new ArrayList<>()).add(dateOfBirth);
    }

    private List<DocumentVerificationResult> buildDocumentVerificationResults(
            final String documentFrontUploadId,
            final String documentBackUploadId,
            final DocumentVerificationResponse apiResponse,
            final String activationId
    ) throws DocumentVerificationException {
        try {
            final var verificationResult = objectMapper.writeValueAsString(apiResponse.getVerification());

            final var documentFrontVerificationResult = new DocumentVerificationResult();
            documentFrontVerificationResult.setUploadId(documentFrontUploadId);
            documentFrontVerificationResult.setVerificationResult(verificationResult);
            documentFrontVerificationResult.setExtractedData(objectMapper.writeValueAsString(apiResponse.getExtraction().getViz().getFront()));

            final var documentBackVerificationResult = new DocumentVerificationResult();
            documentBackVerificationResult.setUploadId(documentBackUploadId);
            documentBackVerificationResult.setVerificationResult(verificationResult);
            documentBackVerificationResult.setExtractedData(objectMapper.writeValueAsString(apiResponse.getExtraction().getViz().getBack()));

            return List.of();
        } catch (final JsonProcessingException e) {
            throw new DocumentVerificationException(
                    "Error when parsing verification result for documents with uploadIds: [%s, %s], activationId: %s".formatted(documentFrontUploadId, documentBackUploadId, activationId),
                    e
            );
        }
    }

    private static void verifyDocumentsCrosscheck(final Map<String, List<String>> documentsCrosscheckData, final String activationId) throws DocumentVerificationException {
        for (final var extractedDataEntry : documentsCrosscheckData.entrySet()) {
            final var fieldName = extractedDataEntry.getKey();
            final var extractedValues = extractedDataEntry.getValue();

            final var distinctValuesCount = extractedValues.stream().distinct().count();
            if (distinctValuesCount != 1) {
                throw new DocumentVerificationException(
                        "Cross-check of extracted data failed for activationId %s on field %s.".formatted(
                                activationId,
                                fieldName
                        )
                );
            }
        }
    }

    private static void verifyDocumentType(final DocumentType claimedDocumentType, final Type extractedType, final String activationId) throws DocumentVerificationException {
        final var extractedDocumentType = switch (extractedType) {
            case ID -> DocumentType.ID_CARD;
            case PASSPORT -> DocumentType.PASSPORT;
            case DL -> DocumentType.DRIVING_LICENSE;
            default -> throw new DocumentVerificationException("Unsupported extracted document type %s for activationId %s.".formatted(extractedType, activationId));
        };

        if (extractedDocumentType != claimedDocumentType) {
            throw new DocumentVerificationException(
                    "Extracted document type %s does not match claimed type %s for activationId %s.".formatted(
                            extractedDocumentType,
                            claimedDocumentType,
                            activationId
                    )
            );
        }

    }
}
