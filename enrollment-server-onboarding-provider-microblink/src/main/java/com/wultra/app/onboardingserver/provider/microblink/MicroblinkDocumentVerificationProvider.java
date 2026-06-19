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
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponse;
import com.wultra.app.onboardingserver.provider.microblink.api.DocumentVerificationResponseBundle;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationImageSource;
import com.wultra.app.onboardingserver.provider.microblink.model.api.DocumentVerificationRequest;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.util.CollectionUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.*;

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
        logger.info("Check document upload not supported", action("checkDocumentUpload"), kv("state", "unsupported"), kv("provider", "microblink"), kv("ownerId", ownerId));
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Microblink provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId ownerId, List<SubmittedDocument> submittedDocuments) throws DocumentVerificationException, RemoteCommunicationException {
        final var documentIds = submittedDocuments.stream()
                .map(SubmittedDocument::getDocumentId)
                .toList();

        logger.info("Submit documents initiated", action("submitDocuments"), stateInitiated(), kv("provider", "microblink"), kv("ownerId", ownerId), kv("documentIds", documentIds));

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

        logger.info("Submit documents succeeded", action("submitDocuments"), stateSucceeded(), kv("provider", "microblink"), kv("rejectReason", result.getRejectReason()));
        return result;
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId ownerId, String verificationId) {
        logger.info("Get verification result not supported", action("getVerificationResult"), kv("state", "unsupported"), kv("provider", "microblink"), kv("ownerId", ownerId), kv("verificationId", verificationId));
        throw new UnsupportedOperationException("Method getVerificationResult is not supported by Microblink provider.");
    }

    @Override
    public Image getPhoto(String photoId) throws DocumentVerificationException {
        try {
            logger.info("Get photo initiated", action("getPhoto"), stateInitiated(), kv("provider", "microblink"), kv("photoId", photoId));

            final var photoDocumentData = processedDocumentDataRepository.findById(photoId)
                    .orElseThrow(() -> new DocumentVerificationException("Photo with id %s not found".formatted(photoId)));

            final var image = Image.builder()
                    .filename("FaceImage.jpg")
                    .data(photoDocumentData.getData())
                    .build();

            logger.info("Get photo succeeded", action("getPhoto"), stateSucceeded(), kv("provider", "microblink"));
            return image;
        } catch (final DocumentVerificationException e) {
            logger.info("Get photo failed", action("getPhoto"), stateFailed(), kv("provider", "microblink"));
            throw e;
        }
    }

    @Override
    public void cleanupDocuments(OwnerId ownerId, List<String> uploadIds) {
        logger.debug("Cleanup documents skipped", action("cleanupDocuments"), state("skipped"), kv("provider", "microblink"), kv("ownerId", ownerId), kv("uploadIds", uploadIds));
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) {
        logger.info("Parse rejection reasons initiated", action("parseRejectionReasons"), stateInitiated(), kv("provider", "microblink"), kv("documentResultId", docResult.getId()));

        final var result = List.of(docResult.getVerificationResult());

        logger.info("Parse rejection reasons succeeded", action("parseRejectionReasons"), stateSucceeded(), kv("provider", "microblink"), kv("rejectionReasons", String.join(",", result)));
        return result;
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId ownerId, Map<String, String> initAttributes) {
        logger.info("Init verification sdk initiated", action("initVerificationSdk"), stateInitiated(), kv("provider", "microblink"), kv("ownerId", ownerId), kv("initAttributes", initAttributes));

        final var origin = initAttributes.getOrDefault("origin", null);
        final var platform = initAttributes.getOrDefault("platform", null);
        final var licenseKey = licenseKeyByOriginByPlatform.getOrDefault(origin, new HashMap<>())
                .getOrDefault(platform, null);

        final var sdkInfo = Optional.ofNullable(licenseKey)
                .map(it -> new VerificationSdkInfo(Map.of("license-key", it)))
                .orElse(new VerificationSdkInfo());

        logger.info("Init verification sdk succeeded", action("initVerificationSdk"), stateSucceeded(), kv("provider", "microblink"), kv("sdkInfo", MicroblinkLogSanitizationUtils.sanitizeSdkInfo(sdkInfo)));
        return sdkInfo;
    }

    @Override
    public boolean shouldStoreSelfie() {
        logger.info("Should store selfie succeeded", action("shouldStoreSelfie"), stateSucceeded(), kv("provider", "microblink"), kv("result", false));
        return false;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId ownerId, List<String> uploadIds) {
        final var verificationId = UUID.randomUUID().toString();

        logger.info("Verify documents initiated", action("verifyDocuments"), stateInitiated(), kv("provider", "microblink"), kv("ownerId", ownerId), kv("uploadIds", uploadIds), kv("verificationId", verificationId));

        try {
            final var result = verifyDocuments(uploadIds, verificationId);

            logger.info("Verify documents succeeded", action("verifyDocuments"), stateSucceeded(), kv("provider", "microblink"), kv("result", result.getStatus()));
            return result;
        } catch (final DocumentVerificationException | RuntimeException e) {
            logger.warn("Verify documents failed", action("verifyDocuments"), stateFailed(), kv("provider", "microblink"), e);

            final var errorMessage = "Microblink provider exception: %s %s".formatted(e.getClass().getSimpleName(), e.getMessage());

            return DocumentsVerificationResult.builder()
                    .status(DocumentVerificationStatus.FAILED)
                    .verificationId(verificationId)
                    .results(List.of())
                    .errorDetail(errorMessage)
                    .build();
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
        logger.info("Send microblink request initiated", action("sendMicroblinkRequest"), stateInitiated(), kv("frontDocumentUploadId", frontDocument != null ? frontDocument.uploadId() : null), kv("backDocumentUploadId", backDocument != null ? backDocument.uploadId() : null));

        try {
            final var request = buildRequest(frontDocument, backDocument);
            logger.debug("Send microblink request initiated", action("sendMicroblinkRequest"), stateInitiated(), kv("requestBody", (Supplier<String>) () -> MicroblinkLogSanitizationUtils.sanitizeDocumentVerificationRequest(request)));

            final var response = microblinkRestClient.post("/api/v2/docver", request, new ParameterizedTypeReference<String>() {});
            final var body = Optional.ofNullable(response)
                    .map(HttpEntity::getBody)
                    .orElseThrow(() -> new DocumentVerificationException("Response body is empty"));

            final var parsedResponse = parseMicroblinkResponse(body)
                    .orElseThrow(() -> new DocumentVerificationException("Failed to parse Microblink API response"));

            logger.info("Send microblink request succeeded", action("sendMicroblinkRequest"), stateSucceeded(), kv("verificationResult", Optional.ofNullable(parsedResponse.getParsedResponseBody())
                    .map(DocumentVerificationResponse::verification)
                    .map(DocumentVerificationResponse.Verification::result)
                    .orElse(null)), kv("microblinkTraceId", Optional.ofNullable(parsedResponse.getParsedResponseBody())
                    .map(DocumentVerificationResponse::runtime)
                    .map(DocumentVerificationResponse.Runtime::traceId)
                    .orElse(null)));

            return parsedResponse;
        } catch (final RestClientException e) {
            logger.info("Send microblink request failed", action("sendMicroblinkRequest"), stateFailed(), kv("statusCode", e.getStatusCode()), kv("response", e.getResponse()));

            throw new RemoteCommunicationException(
                    "Failed REST API call to Microblink, statusCode=%s, responseBody='%s'".formatted(
                            e.getStatusCode(),
                            e.getResponse()
                    ),
                    e
            );
        }
    }

    private Optional<DocumentVerificationResponseBundle> parseMicroblinkResponse(final String responseBodyJson) {
        try {
            final var parsedResponseBody = objectMapper.readValue(responseBodyJson, DocumentVerificationResponse.class);
            final var responseJson = objectMapper.readTree(responseBodyJson);

            return Optional.of(new DocumentVerificationResponseBundle(parsedResponseBody, (ObjectNode) responseJson));
        } catch (final JacksonException e) {
            final var traceId = Optional.ofNullable(responseBodyJson)
                    .map(json -> MICROBLINK_TRACE_ID_PATTERN.matcher(responseBodyJson))
                    .filter(Matcher::find)
                    .map(matcher -> matcher.group(1))
                    .orElse(null);

            logger.warn("Failed to parse Microblink API response, response body is not valid JSON. Microblink traceId: {}", traceId, kv("traceId", traceId), e);
            return Optional.empty();
        }
    }

    private List<DocumentSubmitResult> processMicroblinkResults(
            final List<DocumentVerificationData> documentsVerificationData,
            final Map<DocumentType, DocumentVerificationResponseBundle> microblinkResponseByDocumentType
    ) {
        final var results = new ArrayList<DocumentSubmitResult>();

        for (final var documentVerificationData : documentsVerificationData) {
            final var microblinkResponse = microblinkResponseByDocumentType.get(documentVerificationData.type());

            final var extractedData = microblinkResponse.getOverallExtraction();
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
        logger.info("Face photo extracted from document type {} and stored with id {}", facePhotoDocumentType, facePhotoDocumentId, kv("facePhotoDocumentId", facePhotoDocumentId));

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

        final var documentVerificationResultBundles = uploadIds.stream()
                .map(uploadId -> verifyDocument(documentsVerificationByUploadId, uploadId))
                .toList();

        final var documentResults = documentVerificationResultBundles.stream()
                .map(DocumentVerificationResultBundle::result)
                .toList();

        final var rejectReasons = documentResults.stream()
                .filter(DocumentVerificationResult::isRejected)
                .map(it -> "uploadId=%s, rejectReason=%s".formatted(it.getUploadId(), it.getRejectReason()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (properties.isExtractedDataCheckEnabled() && rejectReasons.isEmpty()) {
            final var crosscheckFailedFields = performDocumentsCrosscheck(documentVerificationResultBundles);
            final var crosscheckPassed = crosscheckFailedFields.isEmpty();
            logger.info("Document data crosscheck passed: {}, failedFields: {}, uploadIds: {}", crosscheckPassed, crosscheckFailedFields, uploadIds, kv("uploadIds", uploadIds));

            if (!crosscheckPassed) {
                rejectReasons.add("Document data crosscheck failed for fields: %s".formatted(crosscheckFailedFields));
            }
        }

        return DocumentsVerificationResult.builder()
                .verificationId(verificationId)
                .results(documentResults)
                .rejectReason(rejectReasons.isEmpty() ? null : rejectReasons.toString())
                .status(rejectReasons.isEmpty() ? DocumentVerificationStatus.ACCEPTED : DocumentVerificationStatus.REJECTED)
                .build();
    }

    @SneakyThrows
    private DocumentVerificationResultBundle verifyDocument(
            final Map<String, DocumentVerificationEntity> documentsVerificationByUploadId,
            final String uploadId
    ) {
        final var documentVerificationResultBuilder = DocumentVerificationResult.builder()
                .uploadId(uploadId);

        if (!documentsVerificationByUploadId.containsKey(uploadId)) {
            throw new DocumentVerificationException("Document verification data not found for uploadId=%s".formatted(uploadId));
        }

        final var documentVerification = documentsVerificationByUploadId.get(uploadId);

        final var documentType = documentVerification.getType();
        final var documentResult = documentVerification.getResults()
                .stream()
                .findFirst()
                .orElse(null);

        if (documentResult == null) {
            throw new DocumentVerificationException("Document result not found for uploadId=" + uploadId);
        }

        final var microblinkResponse = parseMicroblinkResponse(documentResult.getVerificationResult())
                .orElseThrow(() -> new DocumentVerificationException("Failed to parse provider response for uploadId=%s".formatted(uploadId)));

        final var microblinkVerification = Optional.of(microblinkResponse)
                .map(DocumentVerificationResponseBundle::getParsedResponseBody)
                .map(DocumentVerificationResponse::verification);

        final var microblinkCheckResult = microblinkVerification.map(DocumentVerificationResponse.Verification::result)
                .orElse(null);

        final var score = microblinkVerification.map(DocumentVerificationResponse.Verification::certaintyLevel)
                .map(MicroblinkDocumentVerificationProvider::convertScore)
                .orElse(0);

        if (!MICROBLINK_VALIDATION_PASS_RESULT.equalsIgnoreCase(microblinkCheckResult)) {
            final var rejectReasons = Optional.of(microblinkResponse)
                    .map(DocumentVerificationResponseBundle::getParsedResponseBody)
                    .map(DocumentVerificationResponse::messages)
                    .orElse(List.of())
                    .stream()
                    .map(message -> "%s %s".formatted(message.code(), message.message()))
                    .toList();

            documentVerificationResultBuilder.rejectReason("Rejected by provider " + rejectReasons);
            documentVerificationResultBuilder.verificationResult(documentResult.getVerificationResult());
            documentVerificationResultBuilder.extractedData(documentResult.getExtractedData());
            documentVerificationResultBuilder.verificationScore(score);
            return createDocumentVerificationResultBundle(documentVerificationResultBuilder, null);
        }

        documentVerificationResultBuilder.verificationResult(documentResult.getVerificationResult());
        documentVerificationResultBuilder.extractedData(documentResult.getExtractedData());
        documentVerificationResultBuilder.verificationScore(score);

        if (properties.isExtractedDataCheckEnabled()) {
            final var extraction = Optional.of(microblinkResponse)
                    .map(DocumentVerificationResponseBundle::getParsedResponseBody)
                    .map(DocumentVerificationResponse::extraction);

            final var extractedType = extraction.map(DocumentVerificationResponse.Extraction::classInfo)
                    .map(DocumentVerificationResponse.ExtractionClassInfo::type)
                    .orElse(null);

            final var extractedData = documentResult.getExtractedData();

            final var isDocumentTypeValid = verifyDocumentType(uploadId, documentType, extractedType);

            if (!isDocumentTypeValid) {
                final var rejectReason = "Extracted document type %s does not match claimed type %s".formatted(extractedType, documentType);

                documentVerificationResultBuilder.rejectReason(rejectReason);
                documentVerificationResultBuilder.verificationResult(documentResult.getVerificationResult());
                documentVerificationResultBuilder.extractedData(extractedData);
                documentVerificationResultBuilder.verificationScore(score);
                return createDocumentVerificationResultBundle(documentVerificationResultBuilder, null);
            }

            final var crosscheckData = buildCrosscheckData(extractedData);
            return createDocumentVerificationResultBundle(documentVerificationResultBuilder, crosscheckData);
        }

        return createDocumentVerificationResultBundle(documentVerificationResultBuilder, null);
    }

    private static DocumentVerificationResultBundle createDocumentVerificationResultBundle(
            final DocumentVerificationResult.DocumentVerificationResultBuilder documentVerificationResultBuilder,
            final DocumentCrosscheckData crosscheckData
    ) {
        final var finalCrosscheckData = Optional.ofNullable(crosscheckData)
                .orElse(DocumentCrosscheckData.builder().build());

        return DocumentVerificationResultBundle.builder()
                .result(documentVerificationResultBuilder.build())
                .crosscheckData(finalCrosscheckData)
                .build();
    }

    private static Integer convertScore(final String source) {
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

    private DocumentVerificationRequest buildRequest(
            final DocumentVerificationData frontDocument,
            final DocumentVerificationData backDocument
    ) {
        final var requestOptions = properties.getRequestOptions();
        final var requestUseCase = properties.getRequestUseCase();

        final var frontImageSource = buildImageSource(frontDocument);
        final var backImageSource = buildImageSource(backDocument);

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setOptions(requestOptions);
        request.setUseCase(requestUseCase);
        return request;
    }

    private DocumentCrosscheckData buildCrosscheckData(final String extractedData) {
        final var extractedDataValue = parseExtractedDataValue(extractedData);

        final var firstName = extractedDataValue.map(DocumentExtractedDataValue::givenNames)
                .map(String::toLowerCase)
                .orElse(null);

        final var lastName = extractedDataValue.map(DocumentExtractedDataValue::surname)
                .map(String::toLowerCase)
                .orElse(null);

        final var dateOfBirth = extractedDataValue.map(DocumentExtractedDataValue::dateOfBirth)
                .orElse(null);

        return DocumentCrosscheckData.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(dateOfBirth)
                .build();
    }

    private Optional<DocumentExtractedDataValue> parseExtractedDataValue(final String extractedData) {
        try {
            final var parsedValue = objectMapper.readValue(extractedData, DocumentExtractedDataValue.class);
            return Optional.of(parsedValue);
        } catch (final RuntimeException e) {
            logger.warn("Failed to parse extracted data value", e);
            return Optional.empty();
        }
    }

    private static List<String> performDocumentsCrosscheck(final List<MicroblinkDocumentVerificationProvider.DocumentVerificationResultBundle> documentVerificationResultBundles) {
        final var crosscheckData = documentVerificationResultBundles.stream()
                .map(DocumentVerificationResultBundle::crosscheckData)
                .toList();

        final var failedFields = new ArrayList<String>();

        Optional.ofNullable(performFieldCrosscheck("firstName", crosscheckData, DocumentCrosscheckData::firstName))
                .ifPresent(failedFields::add);

        Optional.ofNullable(performFieldCrosscheck("lastName", crosscheckData, DocumentCrosscheckData::lastName))
                .ifPresent(failedFields::add);

        Optional.ofNullable(performFieldCrosscheck("dateOfBirth", crosscheckData, DocumentCrosscheckData::dateOfBirth))
                .ifPresent(failedFields::add);

        return failedFields;
    }

    private static String performFieldCrosscheck(
            final String fieldName,
            final Collection<DocumentCrosscheckData> documentsCrosscheckData,
            final Function<DocumentCrosscheckData, Object> fieldExtractor
    ) {
        final var values = documentsCrosscheckData.stream()
                .map(fieldExtractor)
                .distinct()
                .toList();

        final var checkPassed = values.size() == 1 && !values.contains(null);

        return checkPassed ? null : fieldName;
    }

    private static boolean verifyDocumentType(final String uploadId, final DocumentType claimedDocumentType, final String extractedType) {
        if (extractedType == null) {
            logger.warn("Extracted document type is missing for document uploadId={}", uploadId, kv("uploadId", uploadId));
            return false;
        }

        final var extractedDocumentType = switch (extractedType) {
            case "Id" -> DocumentType.ID_CARD;
            case "Passport" -> DocumentType.PASSPORT;
            case "Dl" -> DocumentType.DRIVING_LICENSE;
            default -> DocumentType.UNKNOWN;
        };

        if (extractedDocumentType == DocumentType.UNKNOWN) {
            logger.warn("Unsupported document type '{}' for document uploadId={}", extractedType, uploadId, kv("uploadId", uploadId));
            return false;
        }

        return extractedDocumentType == claimedDocumentType;
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

    @Builder
    record DocumentVerificationResultBundle(
            DocumentVerificationResult result,
            DocumentCrosscheckData crosscheckData
    ) {}
}