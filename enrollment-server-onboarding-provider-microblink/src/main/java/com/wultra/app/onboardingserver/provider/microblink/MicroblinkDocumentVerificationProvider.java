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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentDataEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponseBundle;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationImageSource;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationProcessingOptions;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationRequest;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationUseCaseOptions;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.microblink.com/">Microblink</a>.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Slf4j
public class MicroblinkDocumentVerificationProvider implements DocumentVerificationProvider {

    private static final Pattern MICROBLINK_TRACE_ID_PATTERN = Pattern.compile("\"traceId\"\\s*:\\s*\"([^\"]+)\"");
    private static final String MICROBLINK_VALIDATION_PASS_RESULT = "Pass";

    private final RestClient microblinkRestClient;
    private final ObjectMapper objectMapper;
    private final DocumentDataRepository documentDataRepository;
    private final ProcessedDocumentDataRepository processedDocumentDataRepository;
    private final DocumentVerificationRepository documentVerificationRepository;
    private final MicroblinkConfigProperties properties;
    private final MicroblinkExtractedDataParser microblinkExtractedDataParser;
    private final Map<String, Map<String, String>> licenseKeyByOriginByPlatform;

    public MicroblinkDocumentVerificationProvider(
            final RestClient microblinkRestClient,
            final ObjectMapper objectMapper,
            final MicroblinkConfigProperties properties,
            final DocumentDataRepository documentDataRepository,
            final ProcessedDocumentDataRepository processedDocumentDataRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final MicroblinkExtractedDataParser microblinkExtractedDataParser
    ) {
        this.microblinkRestClient = microblinkRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.documentDataRepository = documentDataRepository;
        this.processedDocumentDataRepository = processedDocumentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.microblinkExtractedDataParser = microblinkExtractedDataParser;

        licenseKeyByOriginByPlatform = buildLicenseKeyByOriginByPlatform(properties.getMobileSdkConfigs());
    }

    private static Map<String, Map<String, String>> buildLicenseKeyByOriginByPlatform(final List<MicroblinkConfigProperties.SdkConfig> sdkConfigs) {
         return sdkConfigs.stream().collect(
                Collectors.groupingBy(
                        MicroblinkConfigProperties.SdkConfig::origin,
                        Collectors.toMap(
                                MicroblinkConfigProperties.SdkConfig::platform,
                                MicroblinkConfigProperties.SdkConfig::licenseKey,
                                (a, b) -> {
                                    throw new IllegalStateException("Duplicate origin+platform combination");
                                }
                        )
                )
        );
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId ownerId, DocumentVerificationEntity document) {
        logger.info("action: checkDocumentUpload, state: unsupported, provider: microblink, ownerId: {}", ownerId);
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Microblink provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> submittedDocuments) throws DocumentVerificationException, RemoteCommunicationException {
        final var documentIds = submittedDocuments.stream()
                .map(SubmittedDocument::getDocumentId)
                .toList();

        logger.info("action: submitDocuments, state: initiated, provider: microblink, ownerId: {}, documentIds: {}", ownerId, documentIds);

        final var documentsVerificationData = submittedDocuments.stream()
                .map(MicroblinkDocumentVerificationProvider::buildDocumentVerificationData)
                .toList();

        saveDocumentsData(documentsVerificationData);

        final var documentsByTypeAndSide = groupDocumentsByTypeAndSide(documentsVerificationData);
        final var microblinkResponseByDocumentType = fetchMicroblinkResults(documentsByTypeAndSide);

        final var documentResults = processMicroblinkResults(documentsVerificationData, microblinkResponseByDocumentType);

        final var documentVerificationsByDocumentType = getDocumentVerificationsByDocumentType(ownerId, documentsByTypeAndSide.keySet());
        final var facePhotoId = saveProcessedImages(microblinkResponseByDocumentType, documentVerificationsByDocumentType);

        final var result = new DocumentsSubmitResult();
        result.setResults(documentResults);
        result.setExtractedPhotoId(facePhotoId);
        result.setAuditData(collectDataForAudit(microblinkResponseByDocumentType));

        final var rejectedDocuments = documentResults.stream()
                .filter(r -> r.getRejectReason() != null)
                .map(DocumentSubmitResult::getDocumentId)
                .toList();

        if (!rejectedDocuments.isEmpty()) {
            result.setRejectReason("Rejected documents: " + rejectedDocuments);
        }

        logger.info("action: submitDocuments, state: succeeded, provider: microblink, rejectReason: {}", result.getRejectReason());
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

            final var photoDocumentData = processedDocumentDataRepository.findById(photoId)
                    .orElseThrow(() -> new DocumentVerificationException("Photo with id %s not found".formatted(photoId)));

            final var image = Image.builder()
                    .filename("FaceImage.jpg")
                    .data(photoDocumentData.getData())
                    .build();

            logger.info("action: getPhoto, state: succeeded, provider: microblink");
            return image;
        } catch (final DocumentVerificationException e) {
            logger.info("action: getPhoto, state: failed, provider: microblink, error: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void cleanupDocuments(OwnerId ownerId, List<String> uploadIds) {
        logger.debug("action: cleanupDocuments, state: skipped, provider: microblink, ownerId: {}, uploadIds: {}", ownerId, uploadIds);
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) {
        logger.info("action: parseRejectionReasons, state: initiated, provider: microblink, documentResultId: {}", docResult.getId());

        final var result = List.of(docResult.getVerificationResult());

        logger.info("action: parseRejectionReasons, state: succeeded, provider: microblink, rejectionReasons: {}", String.join(",", result));
        return result;
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId ownerId, Map<String, String> initAttributes) {
        logger.info("action: initVerificationSdk, state: initiated, provider: microblink, ownerId: {}, initAttributes: {}", ownerId, initAttributes);

        final var origin = initAttributes.getOrDefault("origin", null);
        final var platform = initAttributes.getOrDefault("platform", null);
        final var licenseKey = licenseKeyByOriginByPlatform.getOrDefault(origin, new HashMap<>())
                .getOrDefault(platform, null);

        final var sdkInfo = Optional.ofNullable(licenseKey)
                .map(it -> new VerificationSdkInfo(Map.of("license-key", it)))
                .orElse(new VerificationSdkInfo());

        logger.info("action: initVerificationSdk, state: succeeded, provider: microblink, sdkInfo: {}", MicroblinkLogSanitizationUtils.sanitizeSdkInfo(sdkInfo));
        return sdkInfo;
    }

    @Override
    public boolean shouldStoreSelfie() {
        logger.info("action: shouldStoreSelfie, state: succeeded, provider: microblink, result: false");
        return false;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId ownerId, List<String> uploadIds) throws DocumentVerificationException {
        try {
            final var verificationId = UUID.randomUUID().toString();

            logger.info(
                    "action: verifyDocuments, state: initiated, provider: microblink, ownerId: {}, uploadIds: {}, verificationId: {}",
                    ownerId,
                    uploadIds,
                    verificationId
            );

            final var result = verifyDocuments(uploadIds, verificationId);

            logger.info("action: verifyDocuments, state: succeeded, provider: microblink, result: {}", result.getStatus());
            return result;
        } catch (final DocumentVerificationException e) {
            logger.info("action: verifyDocuments, state: failed, provider: microblink, error: {}", e.getMessage());
            throw e;
        }
    }

    private static DocumentVerificationData buildDocumentVerificationData(final SubmittedDocument submittedDocument) {
        return DocumentVerificationData.builder()
                .documentId(submittedDocument.getDocumentId())
                .uploadId(UUID.randomUUID().toString())
                .type(submittedDocument.getType())
                .side(submittedDocument.getSide())
                .image(submittedDocument.getPhoto())
                .build();
    }

    private void saveDocumentsData(final List<DocumentVerificationData> documents) {
        final var documentsData = documents.stream()
                .map(MicroblinkDocumentVerificationProvider::buildDocumentData)
                .toList();

        documentDataRepository.saveAll(documentsData);
    }

    private static Map<DocumentType, Map<CardSide, DocumentVerificationData>> groupDocumentsByTypeAndSide(
            final List<DocumentVerificationData> documentsVerificationData
    ) throws DocumentVerificationException {
        final var documentsByTypeAndSide = new EnumMap<DocumentType, Map<CardSide, DocumentVerificationData>>(DocumentType.class);
        for (final var documentVerificationData : documentsVerificationData) {
            final var documentType = documentVerificationData.type();
            final var documentSide = documentVerificationData.side();

            final var documentsOfSameType = documentsByTypeAndSide.computeIfAbsent(documentType, k -> new EnumMap<>(CardSide.class));

            final var documentOfSameSide = documentsOfSameType.getOrDefault(documentSide, null);
            if (documentOfSameSide != null) {
                throw new DocumentVerificationException(
                        "Multiple documents of type %s and side %s found. Document ids: %s".formatted(
                                documentType,
                                documentSide,
                                List.of(documentOfSameSide.documentId(), documentVerificationData.documentId())
                        )
                );
            }

            documentsOfSameType.put(documentSide, documentVerificationData);
        }

        return documentsByTypeAndSide;
    }

    private Map<DocumentType, DocumentVerificationResponseBundle> fetchMicroblinkResults(
            final Map<DocumentType, Map<CardSide, DocumentVerificationData>> documentsByTypeAndSide
    ) throws DocumentVerificationException, RemoteCommunicationException {
        final var results = new EnumMap<DocumentType, DocumentVerificationResponseBundle>(DocumentType.class);

        for (final var documentsOfSameType : documentsByTypeAndSide.entrySet()) {
            final var documentType = documentsOfSameType.getKey();
            final var documentDataFront = documentsOfSameType.getValue().getOrDefault(CardSide.FRONT, null);
            final var documentDataBack = documentsOfSameType.getValue().getOrDefault(CardSide.BACK, null);

            final var parsedResponse = sendApiRequest(documentDataFront, documentDataBack);

            results.put(documentType, parsedResponse);
        }

        return results;
    }

    private DocumentVerificationResponseBundle sendApiRequest(
            final DocumentVerificationData frontDocument,
            final DocumentVerificationData backDocument
    ) throws DocumentVerificationException, RemoteCommunicationException {
        logger.info("action: sendMicroblinkRequest, state: initiated, frontDocumentUploadId: {}, backDocumentUploadId: {}",
                frontDocument != null ? frontDocument.uploadId() : null,
                backDocument != null ? backDocument.uploadId() : null);

        try {
            final var request = buildRequest(frontDocument, backDocument, properties.getRequestOptions());
            logger.debug("action: sendMicroblinkRequest, state: initiated, requestBody: {}",
                    (Supplier<String>) () -> MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationRequest(request));

            final var response = microblinkRestClient.post("/api/v2/docver", request, new ParameterizedTypeReference<String>() {});
            final var body = Optional.ofNullable(response)
                    .map(HttpEntity::getBody)
                    .orElseThrow(() -> new DocumentVerificationException("Response body is empty"));

            final var parsedResponse = parseMicroblinkResponse(body);
            logger.info("action: sendMicroblinkRequest, state: succeeded, verificationResult: {}, microblinkTraceId: {}",
                    Optional.ofNullable(parsedResponse.getParsedResponseBody())
                            .map(DocumentVerificationResponse::verification)
                            .map(DocumentVerificationResponse.Verification::result)
                            .orElse(null),
                    Optional.ofNullable(parsedResponse.getParsedResponseBody())
                            .map(DocumentVerificationResponse::runtime)
                            .map(DocumentVerificationResponse.Runtime::traceId)
                            .orElse(null)
            );

            return parsedResponse;
        } catch (final RestClientException e) {
            logger.info("action: sendMicroblinkRequest, state: failed, exceptionMessage: {}, statusCode: {}, response: {}",
                    e.getMessage(),
                    e.getStatusCode(),
                    e.getResponse());

            throw new RemoteCommunicationException(
                    "Failed REST API call to Microblink, statusCode=%s, responseBody='%s'".formatted(
                            e.getStatusCode(),
                            e.getResponse()
                    ),
                    e
            );
        }
    }

    private DocumentVerificationResponseBundle parseMicroblinkResponse(final String responseBodyJson) throws DocumentVerificationException {
        try {
            final var parsedResponseBody = objectMapper.readValue(responseBodyJson, DocumentVerificationResponse.class);
            final var responseJson = objectMapper.readTree(responseBodyJson);

            return new DocumentVerificationResponseBundle(parsedResponseBody, (ObjectNode) responseJson);
        } catch (final JsonProcessingException e) {
            final var traceId = Optional.ofNullable(responseBodyJson)
                    .map(json -> MICROBLINK_TRACE_ID_PATTERN.matcher(responseBodyJson))
                    .filter(Matcher::find)
                    .map(matcher -> matcher.group(1))
                    .orElse(null);

            throw new DocumentVerificationException("Failed to parse Microblink API response. Microblink traceId: %s".formatted(traceId), e);
        }
    }

    private List<DocumentSubmitResult> processMicroblinkResults(
            final List<DocumentVerificationData> documentsVerificationData,
            final Map<DocumentType, DocumentVerificationResponseBundle> microblinkResponseByDocumentType
    ) {
        final var results = new ArrayList<DocumentSubmitResult>();

        for (final var documentVerificationData : documentsVerificationData) {
            final var microblinkResponse = microblinkResponseByDocumentType.get(documentVerificationData.type());

            final var extractedData = switch (documentVerificationData.side()) {
                case FRONT -> microblinkResponse.getExtractionFront();
                case BACK -> microblinkResponse.getExtractionBack();
            };

            final var responseBody = microblinkResponse.getParsedResponseBody();

            final var extractedDataValue = microblinkExtractedDataParser.parseExtractedData(extractedData, responseBody.extraction());

            final var result = new DocumentSubmitResult();
            result.setDocumentId(documentVerificationData.documentId());
            result.setUploadId(documentVerificationData.uploadId());
            result.setExtractedData(extractedDataValue);
            result.setValidationResult(microblinkResponse.getResponseWithoutImages());

            final var validation = responseBody.verification();

            if (!MICROBLINK_VALIDATION_PASS_RESULT.equalsIgnoreCase(validation.result())) {
                final var validationErrorMessages = responseBody.messages()
                        .stream()
                        .map(DocumentVerificationResponse.Message::message)
                        .toList();

                result.setRejectReason(validationErrorMessages.toString());
            }

            results.add(result);
        }

        return results;
    }

    private Map<DocumentType, List<DocumentVerificationEntity>> getDocumentVerificationsByDocumentType(final OwnerId ownerId, final Set<DocumentType> documentTypes) {
        final var activationId = ownerId.getActivationId();
        final var documentVerifications = documentVerificationRepository.findAllByActivationIdByTypes(activationId, documentTypes);

        final var latestDocumentVerificationByTypeAndSide = documentVerifications.stream()
                .collect(Collectors.groupingBy(
                        DocumentVerificationEntity::getType,
                        HashMap::new,
                        Collectors.toMap(
                                DocumentVerificationEntity::getSide,
                                Function.identity(),
                                (existing, current) -> current.getTimestampCreated().after(existing.getTimestampCreated()) ? current : existing,
                                HashMap::new
                        )
                ));

        return latestDocumentVerificationByTypeAndSide.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue().values()),
                        (a, b) -> b,
                        HashMap::new
                ));
    }

    private String saveProcessedImages(
            final Map<DocumentType, DocumentVerificationResponseBundle> microblinkResponseByDocumentType,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByDocumentType
    ) {
        final var facePhoto = createFacePhotoEntity(microblinkResponseByDocumentType, documentVerificationsByDocumentType);
        final var documents = createProcessedDocuments(microblinkResponseByDocumentType, documentVerificationsByDocumentType);

        final var processedDocumentData = new ArrayList<ProcessedDocumentDataEntity>();
        if (facePhoto != null) {
            processedDocumentData.add(facePhoto);
        }
        processedDocumentData.addAll(documents);

        processedDocumentDataRepository.saveAll(processedDocumentData);

        return Optional.ofNullable(facePhoto)
                .map(ProcessedDocumentDataEntity::getId)
                .orElse(null);
    }

    private List<ProcessedDocumentDataEntity> createProcessedDocuments(
            final Map<DocumentType, DocumentVerificationResponseBundle> microblinkResponseByDocumentType,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByDocumentType
    ) {
        return microblinkResponseByDocumentType.entrySet().stream()
                .flatMap(it -> Optional.ofNullable(it.getValue())
                        .map(DocumentVerificationResponseBundle::getParsedResponseBody)
                        .map(DocumentVerificationResponse::images)
                        .orElse(List.of())
                        .stream()
                        .map(image -> createDocumentImage(image, documentVerificationsByDocumentType, it.getKey()))
                )
                .filter(Objects::nonNull)
                .toList();
    }

    private static ProcessedDocumentDataEntity createDocumentImage(
            final DocumentVerificationResponse.Image image,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByDocumentType,
            final DocumentType documentType) {
        final var imageName = image.name();

        final var processedDocumentType = switch (imageName) {
            case "FullDocumentFrontImage" -> ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE;
            case "FullDocumentBackImage" -> ProcessedDocumentDataType.DOCUMENT_BACK_SIDE;
            default -> null;
        };

        if (processedDocumentType == null) {
            return null;
        }

        final var documentSide = processedDocumentType == ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE ? CardSide.FRONT : CardSide.BACK;
        final var documentVerificationId = findDocumentVerificationId(documentVerificationsByDocumentType, documentType, documentSide);

        return buildProcessedDocumentDataEntity(processedDocumentType, documentVerificationId, image.base64());
    }

    private ProcessedDocumentDataEntity createFacePhotoEntity(
            final Map<DocumentType, DocumentVerificationResponseBundle> microblinkResponseByDocumentType,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByDocumentType) {
        final var facePhotoDocumentType = DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO.stream()
                .filter(microblinkResponseByDocumentType::containsKey)
                .findFirst()
                .orElse(null);

        if (facePhotoDocumentType == null) {
            logger.warn("Not suitable document type found for face photo extraction");
            return null;
        }

        final var microblinkResponse = microblinkResponseByDocumentType.get(facePhotoDocumentType);

        final var faceImageBase64 = Optional.ofNullable(microblinkResponse)
                .map(DocumentVerificationResponseBundle::getParsedResponseBody)
                .map(DocumentVerificationResponse::images)
                .orElse(Collections.emptyList())
                .stream()
                .filter(image -> "FaceImage".equals(image.name()))
                .findFirst()
                .map(DocumentVerificationResponse.Image::base64)
                .orElse(null);

        if (faceImageBase64 == null) {
            logger.debug("Microblink response does not contain face image");
            return null;
        }

        final var documentVerificationId = findDocumentVerificationId(documentVerificationsByDocumentType, facePhotoDocumentType, CardSide.FRONT);

        final var facePhotoDocumentData = buildProcessedDocumentDataEntity(ProcessedDocumentDataType.FACE_IMAGE, documentVerificationId, faceImageBase64);

        final var facePhotoDocumentId = facePhotoDocumentData.getId();
        logger.info("Face photo extracted from document type {} and stored with id {}", facePhotoDocumentType, facePhotoDocumentId);

        return facePhotoDocumentData;
    }

    private static String findDocumentVerificationId(
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByDocumentType,
            final DocumentType documentType,
            final CardSide side
    ) {
        final var documentVerifications = documentVerificationsByDocumentType.getOrDefault(documentType, List.of());

        final var documentVerificationId = documentVerifications.stream()
                .filter(it -> it.getSide() == side)
                .map(DocumentVerificationEntity::getId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(documentVerifications.stream()
                        .findFirst()
                        .map(DocumentVerificationEntity::getId)
                        .orElse(null)
                );

        if (documentVerificationId == null) {
            logger.warn("Document verification ID not found for document type '{}' and side '{}'", documentType, side);
        }

        return documentVerificationId;
    }

    private static ProcessedDocumentDataEntity buildProcessedDocumentDataEntity(
            final ProcessedDocumentDataType dataType,
            final String documentVerificationId,
            final String dataBase64
    ) {
        final var dataBytes = convert(dataBase64, dataType);

        final var entity = new ProcessedDocumentDataEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setData(dataBytes);
        entity.setDataType(dataType);
        entity.setTimestampCreated(new Date());
        entity.setDocumentVerificationId(documentVerificationId);

        return entity;
    }

    private static byte[] convert(final String dataBase64, final ProcessedDocumentDataType dataType) {
        try {
            return Base64.getDecoder().decode(dataBase64);
        } catch (final RuntimeException e) {
            logger.warn("Exception when decoding base64 data for data type: {}", dataType, e);
            return new byte[0];
        }
    }

    private DocumentsVerificationResult verifyDocuments(List<String> uploadIds, String verificationId) throws DocumentVerificationException {
        final var documentsVerificationByUploadId = documentVerificationRepository.findAllByUploadIds(uploadIds)
                .stream()
                .collect(Collectors.toMap(DocumentVerificationEntity::getUploadId, Function.identity()));

        if (CollectionUtils.isEmpty(documentsVerificationByUploadId)) {
            throw new DocumentVerificationException("No document verification data found for uploadIds: %s".formatted(uploadIds));
        }

        final var crosscheckDataByDocumentType = new EnumMap<DocumentType, DocumentCrosscheckData>(DocumentType.class);
        final var microblinkCheckResults = new ArrayList<String>();
        final var documentVerificationResults = new ArrayList<DocumentVerificationResult>();
        final var rejectedDocumentUploadIds = new ArrayList<String>();

        for (final var uploadId : uploadIds) {
            final var documentVerification = Optional.ofNullable(documentsVerificationByUploadId.getOrDefault(uploadId, null))
                    .orElseThrow(() -> new DocumentVerificationException("No document verification data found for uploadId: " + uploadId));

            final var documentType = documentVerification.getType();
            final var documentResult = documentVerification.getResults()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new DocumentVerificationException("No document result data found for uploadId: " + documentVerification.getUploadId()));

            final var microblinkResponseBundle = parseMicroblinkResponse(documentResult.getVerificationResult());
            final var microblinkResponse = microblinkResponseBundle.getParsedResponseBody();

            final var microblinkCheckResult = microblinkResponse.verification().result();
            microblinkCheckResults.add(microblinkCheckResult);

            if (properties.isExtractedDataCheckEnabled() && !crosscheckDataByDocumentType.containsKey(documentType)) {
                final var extractedType = microblinkResponse.extraction().classInfo().type();
                verifyDocumentType(documentType, extractedType);

                final var overallExtraction = microblinkResponse.extraction().overall();
                final var crosscheckData = buildCrosscheckData(overallExtraction);
                crosscheckDataByDocumentType.put(documentType, crosscheckData);
            }

            final var documentVerificationResult = new DocumentVerificationResult();
            documentVerificationResult.setUploadId(documentVerification.getUploadId());
            documentVerificationResult.setVerificationResult(documentResult.getVerificationResult());
            documentVerificationResult.setExtractedData(documentResult.getExtractedData());
            documentVerificationResult.setVerificationScore(convertScore(microblinkResponse.verification().certaintyLevel()));

            if (!MICROBLINK_VALIDATION_PASS_RESULT.equalsIgnoreCase(microblinkCheckResult)) {
                final var rejectReasons = microblinkResponse.messages()
                        .stream()
                        .map(DocumentVerificationResponse.Message::message)
                        .toList();

                documentVerificationResult.setRejectReason(rejectReasons.toString());
                rejectedDocumentUploadIds.add(documentVerification.getUploadId());
            }

            documentVerificationResults.add(documentVerificationResult);
        }

        if (properties.isExtractedDataCheckEnabled()) {
            performDocumentsCrosscheck(crosscheckDataByDocumentType.values().stream().toList());
        }

        final var allChecksPassed = microblinkCheckResults.stream()
                .allMatch(MICROBLINK_VALIDATION_PASS_RESULT::equalsIgnoreCase);

        final var result = new DocumentsVerificationResult();
        result.setVerificationId(verificationId);
        result.setStatus(allChecksPassed ? DocumentVerificationStatus.ACCEPTED : DocumentVerificationStatus.REJECTED);
        result.setResults(documentVerificationResults);

        if (!rejectedDocumentUploadIds.isEmpty()) {
            result.setRejectReason("Rejected document upload ids: " + rejectedDocumentUploadIds);
        }

        return result;
    }

    private static int convertScore(final String source) {
        if (source == null) {
            return 0;
        }

        return switch (source) {
            case "Low" ->  1;
            case "Medium" ->  5;
            case "High" -> 10;
            // also includes Unknown, and NotPerformed
            default -> 0;
        };
    }

    private static DocumentVerificationImageSource buildImageSource(final DocumentVerificationData documentData) {
        if (documentData == null) {
            return null;
        }

        final var imageBase64 = Base64.getEncoder().encodeToString(documentData.image().getData());

        final var imageSource = new DocumentVerificationImageSource();
        imageSource.setBase64(imageBase64);
        return imageSource;
    }

    private static DocumentVerificationRequest buildRequest(
            final DocumentVerificationData frontDocument,
            final DocumentVerificationData backDocument,
            final DocumentVerificationProcessingOptions requestOptions) {
        final var frontImageSource = buildImageSource(frontDocument);
        final var backImageSource = buildImageSource(backDocument);

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setOptions(requestOptions);
        request.setUseCase(useCase);
        return request;
    }

    private static DocumentCrosscheckData buildCrosscheckData(final List<DocumentVerificationResponse.Result> documentExtractedData) throws DocumentVerificationException {
        final var firstName = documentExtractedData.stream()
                .filter(r -> "FirstName".equals(r.field()))
                .findFirst()
                .map(DocumentVerificationResponse.Result::value)
                .map(String::toLowerCase)
                .orElseThrow(() -> new DocumentVerificationException("Field FirstName not found in extracted data"));

        final var lastName = documentExtractedData.stream()
                .filter(r -> "LastName".equals(r.field()))
                .findFirst()
                .map(DocumentVerificationResponse.Result::value)
                .map(String::toLowerCase)
                .orElseThrow(() -> new DocumentVerificationException("Field LastName not found in extracted data"));

        final var dateOfBirthResult = documentExtractedData.stream()
                .filter(r -> "DateOfBirth".equals(r.field()))
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException("Field DateOfBirth not found in extracted data"));

        final var dateOfBirth = parseDate(dateOfBirthResult);

        return DocumentCrosscheckData.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(dateOfBirth)
                .build();
    }

    private static LocalDate parseDate(final DocumentVerificationResponse.Result result) throws DocumentVerificationException {
        final var year = Optional.ofNullable(result.year())
                .orElseThrow(() -> new DocumentVerificationException("Year of field DateOfBirth was not extracted"));

        final var month = Optional.ofNullable(result.month())
                .orElseThrow(() -> new DocumentVerificationException("Month of field DateOfBirth was not extracted"));

        final var day = Optional.ofNullable(result.day())
                .orElseThrow(() -> new DocumentVerificationException("Day of field DateOfBirth was not extracted"));

        return LocalDate.of(year, month, day);
    }
    private static void performDocumentsCrosscheck(final List<DocumentCrosscheckData> documentsCrosscheckData) throws DocumentVerificationException {
        performDocumentFieldCrosscheck("firstName", documentsCrosscheckData, DocumentCrosscheckData::firstName);
        performDocumentFieldCrosscheck("lastName", documentsCrosscheckData, DocumentCrosscheckData::lastName);
        performDocumentFieldCrosscheck("dateOfBirth", documentsCrosscheckData, DocumentCrosscheckData::dateOfBirth);
    }

    private static void performDocumentFieldCrosscheck(
            final String fieldName,
            final List<DocumentCrosscheckData> documentsCrosscheckData,
            final Function<DocumentCrosscheckData, Object> fieldExtractor
    ) throws DocumentVerificationException {
        final var checkPassed = documentsCrosscheckData.stream()
                .map(fieldExtractor)
                .distinct()
                .count() <= 1;

        if (!checkPassed) {
            throw new DocumentVerificationException("Crosscheck failed for field %s".formatted(fieldName));
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

    private static DocumentDataEntity buildDocumentData(final DocumentVerificationData document) {
        final var documentData = new DocumentDataEntity();
        documentData.setId(document.uploadId());
        documentData.setData(document.image().getData());
        documentData.setTimestampCreated(new Date());
        return documentData;
    }

    private static Map<DocumentType, ObjectNode> collectDataForAudit(final Map<DocumentType, DocumentVerificationResponseBundle> microblinkResponseByDocumentType) {
        return microblinkResponseByDocumentType.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getResponseWithoutPersonalData()
                ));
    }

    @Builder(toBuilder = true)
    record DocumentVerificationData(
            String documentId,
            String uploadId,
            DocumentType type,
            CardSide side,
            Image image
    ) {}

    @Builder
    record DocumentCrosscheckData(
            String firstName,
            String lastName,
            LocalDate dateOfBirth
    ) {}
}