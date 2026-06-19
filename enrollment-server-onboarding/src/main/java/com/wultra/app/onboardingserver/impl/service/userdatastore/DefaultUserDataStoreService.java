/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.impl.service.userdatastore;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.provider.microblink.MicroblinkDocumentVerificationProvider;
import com.wultra.app.onboardingserver.provider.zenid.ZenidDocumentVerificationProvider;
import com.wultra.security.userdatastore.client.UserDataStoreClient;
import com.wultra.security.userdatastore.client.model.error.UserDataStoreClientException;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.request.EmbeddedPhotoCreateRequest;
import com.wultra.security.userdatastore.client.model.response.DocumentCreateResponse;
import com.wultra.security.userdatastore.client.model.response.EmbeddedPhotoCreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.wultra.app.onboardingserver.common.logging.StructuredLogging.*;

/**
 * Implementation of {@link UserDataStoreService}.
 *
 * @implSpec Processing {@link ProcessedDocumentDataEntity} which is used by {@link MicroblinkDocumentVerificationProvider} but not by {@link ZenidDocumentVerificationProvider}.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
class DefaultUserDataStoreService implements UserDataStoreService {

    private static final String DATA_TYPE_CLAIMS = "claims";
    private static final String ATTRIBUTE_TRUSTED_IMAGE = "trustedImage";

    private final UserDataStoreClient userDataStoreClient;

    private final UserDataStoreConfigProperties config;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final ObjectMapper objectMapper;

    private final RetryTemplate retryTemplate;

    DefaultUserDataStoreService(
            final UserDataStoreClient userDataStoreClient,
            final UserDataStoreConfigProperties config,
            final OnboardingProcessRepository onboardingProcessRepository,
            final IdentityVerificationRepository identityVerificationRepository,
            final ProcessedDocumentDataRepository processedDocumentDataRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final ObjectMapper objectMapper) {

        this.userDataStoreClient = userDataStoreClient;
        this.config = config;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.identityVerificationRepository = identityVerificationRepository;
        this.processedDocumentDataRepository = processedDocumentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.objectMapper = objectMapper;
        this.retryTemplate = new RetryTemplate(
                RetryPolicy.builder()
                        .maxRetries(Math.max(0, config.getMaxAttempts() - 1))
                        .delay(Duration.ofMillis(200))
                        .multiplier(2.0)
                        .maxDelay(Duration.ofMillis(2_000))
                        .build());
    }

    @Transactional(readOnly = true)
    @Override
    public List<DocumentCreateRequest> collectDocumentData(final String processId) {
        logger.info("Collect document data initiated", action("collectDocumentData"), stateInitiated(), kv("processId", processId));

        final var process = onboardingProcessRepository.findById(processId).orElse(null);
        if (process == null) {
            logger.warn("Collect document data failed: process_not_found", action("collectDocumentData"), stateFailed(), kv("reason", "process_not_found"), kv("processId", processId));
            return List.of();
        }

        final var identityVerification = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(process.getActivationId()).orElse(null);
        if (identityVerification == null) {
            logger.warn("Collect document data failed: identity_not_found", action("collectDocumentData"), stateFailed(), kv("reason", "identity_not_found"), kv("processId", processId));
            return List.of();
        }

        final var documentVerifications = fetchDocumentVerifications(identityVerification, config.getDocumentType());
        if (documentVerifications.primaryDocuments() == null) {
            logger.info("Collect document data skipped: no_document_verification", action("collectDocumentData"), state("skipped"), kv("reason", "no_document_verification"), kv("processId", processId));
            return List.of();
        }

        final var documentRequests = new ArrayList<DocumentCreateRequest>();
        documentRequests.add(createTrustedDocumentRequest(process, documentVerifications.primaryDocuments()));

        for (final var entry : documentVerifications.otherDocuments().entrySet()) {
            documentRequests.add(createDocumentRequest(process, entry));
        }

        logger.info("Collect document data finished", action("collectDocumentData"), kv("state", "finished"), kv("processId", processId), kv("count", documentRequests.size()));
        return documentRequests;
    }

    @Override
    public void storeDocumentData(final List<DocumentCreateRequest> requests) throws UserDataStoreClientException {
        logger.info("Store document data initiated", action("storeDocumentData"), stateInitiated(), kv("count", requests.size()));

        for (final var request : requests) {
            final AtomicInteger attemptCounter = new AtomicInteger();
            try {
                retryTemplate.execute(() -> callCreateDocument(request, attemptCounter));
            } catch (final RetryException e) {
                throw new UserDataStoreClientException("Too many attempts to create document", e);
            }
        }
        logger.info("Store document data finished", action("storeDocumentData"), kv("state", "finished"));
    }

    private List<ProcessedDocumentDataEntity> fetchProcessedDocumentData(final Collection<DocumentVerificationEntity> documentVerifications) {
        final var documentVerificationIds = documentVerifications.stream()
                .map(DocumentVerificationEntity::getId)
                .collect(Collectors.toSet());
        return processedDocumentDataRepository.findAllByDocumentVerificationIds(documentVerificationIds);
    }

    private @Nullable DocumentCreateRequest createTrustedDocumentRequest(
            final OnboardingProcessEntity process,
            final Map.Entry<DocumentType,List<DocumentVerificationEntity>> source) {

        return createDocumentRequest(process, source, Map.of(ATTRIBUTE_TRUSTED_IMAGE, true));
    }

    private @Nullable DocumentCreateRequest createDocumentRequest(
            final OnboardingProcessEntity process,
            final Map.Entry<DocumentType,List<DocumentVerificationEntity>> source) {

        return createDocumentRequest(process, source, null);
    }

    private @Nullable DocumentCreateRequest createDocumentRequest(
            final OnboardingProcessEntity process,
            final Map.Entry<DocumentType,List<DocumentVerificationEntity>> source,
            final Map<String, Object> attributes) {

        final List<ProcessedDocumentDataEntity> processedData = fetchProcessedDocumentData(source.getValue());
        final List<EmbeddedPhotoCreateRequest> photos = fetchPhotos(processedData);

        return DocumentCreateRequest.builder()
                .userId(process.getUserId())
                .documentType(convert(source.getKey()))
                .dataType(DATA_TYPE_CLAIMS)
                .externalId(process.getId())
                .documentData(fetchExtractedData(source.getValue()))
                .attributes(attributes)
                .photos(photos)
                .build();
    }

    private DocumentsWrapper fetchDocumentVerifications(final IdentityVerificationEntity idVerification, UserDataStoreConfigProperties.DocumentType documentType) {
        final Map<DocumentType, List<DocumentVerificationEntity>> documentVerifications = documentVerificationRepository.findAcceptedWithPhoto(idVerification).stream()
                .collect(Collectors.groupingBy(
                        DocumentVerificationEntity::getType,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (documentVerifications.isEmpty()) {
            return new DocumentsWrapper(null, Map.of());
        }

        final Map.Entry<DocumentType, List<DocumentVerificationEntity>> primaryDocuments = DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO.stream()
                .filter(documentVerifications::containsKey)
                .map(type -> Map.entry(type, documentVerifications.get(type)))
                .findFirst()
                .orElseGet(() -> {
                    logger.warn("Unable to select a preferred source of person photo, selecting the first one, identityVerificationId: {}", idVerification.getId());
                    return documentVerifications.entrySet().iterator().next();
                });

        documentVerifications.remove(primaryDocuments.getKey());

        return switch (documentType) {
            case ALL -> new DocumentsWrapper(primaryDocuments, documentVerifications);
            case WITH_TRUSTED_IMAGE -> new DocumentsWrapper(primaryDocuments, Map.of());
        };
    }

    Void callCreateDocument(final DocumentCreateRequest request, final AtomicInteger attemptCounter) throws UserDataStoreClientException {
        final int maxAttempts = config.getMaxAttempts();
        final int attempt = attemptCounter.incrementAndGet();
        logger.info("Call create document initiated", action("callCreateDocument"), stateInitiated(), kv("userId", request.userId()), kv("externalId", request.externalId()), kv("documentType", request.documentType()), kv("dataType", request.dataType()), kv("attempt", attempt), kv("maxAttempts", maxAttempts));

        try {
            final var response = userDataStoreClient.createDocument(request);
            logger.info("Call create document succeeded", action("callCreateDocument"), stateSucceeded(), kv("documentId", response.id()), kv("photoIds", collectPhotoIds(response)));
            return null;
        } catch (final UserDataStoreClientException e) {
            logger.warn("Call create document failed", action("callCreateDocument"), stateFailed(), kv("attempt", attempt), kv("maxAttempts", maxAttempts));
            throw e;
        }
    }

    private static List<String> collectPhotoIds(final DocumentCreateResponse response) {
        if (response.photos() == null) {
            return List.of();
        }

        return response.photos()
                .stream()
                .map(EmbeddedPhotoCreateResponse::id)
                .toList();
    }

    private List<EmbeddedPhotoCreateRequest> fetchPhotos(final List<ProcessedDocumentDataEntity> processedData) {
        if (!config.isStoreDocumentImageScan()) {
            return List.of();
        }

        return processedData.stream()
                .map(DefaultUserDataStoreService::convert)
                .toList();
    }

    private @Nullable String fetchExtractedData(final List<DocumentVerificationEntity> documentVerifications) {
        if (!config.isStoreExtractedData()) {
            return null;
        }

        final List<DocumentExtractedDataValue> extractedData =
                documentVerifications.stream()
                .map(DocumentVerificationEntity::getResults)
                .map(it -> it.stream().findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(DocumentResultEntity::getExtractedData)
                .filter(StringUtils::isNotBlank)
                .map(this::convert)
                .filter(Objects::nonNull)
                .toList();

        final DocumentExtractedDataValue mergedData = merge(extractedData);

        try {
            return objectMapper.writeValueAsString(mergedData);
        } catch (JacksonException e) {
            logger.warn("Failed to write extracted data, source: {}", mergedData, e);
            return null;
        }
    }

    private static DocumentExtractedDataValue merge(final List<DocumentExtractedDataValue> extractedData) {
        return DocumentExtractedDataValue.builder()
                .givenNames(findFirstValue(DocumentExtractedDataValue::givenNames, extractedData))
                .surname(findFirstValue(DocumentExtractedDataValue::surname, extractedData))
                .dateOfBirth(findFirstValue(DocumentExtractedDataValue::dateOfBirth, extractedData))
                .placeOfBirth(findFirstValue(DocumentExtractedDataValue::placeOfBirth, extractedData))
                .sex(findFirstValue(DocumentExtractedDataValue::sex, extractedData))
                .nationality(findFirstValue(DocumentExtractedDataValue::nationality, extractedData))
                .personalNumber(findFirstValue(DocumentExtractedDataValue::personalNumber, extractedData))
                .documentNumber(findFirstValue(DocumentExtractedDataValue::documentNumber, extractedData))
                .dateOfIssue(findFirstValue(DocumentExtractedDataValue::dateOfIssue, extractedData))
                .dateOfExpiry(findFirstValue(DocumentExtractedDataValue::dateOfExpiry, extractedData))
                .authority(findFirstValue(DocumentExtractedDataValue::authority, extractedData))
                .country(findFirstValue(DocumentExtractedDataValue::country, extractedData))
                .build();
    }

    private static <T> T findFirstValue(final Function<DocumentExtractedDataValue, T> getter, final List<DocumentExtractedDataValue> values) {
        return values.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private DocumentExtractedDataValue convert(final String source) {
        try {
            return objectMapper.readValue(source, DocumentExtractedDataValue.class);
        } catch (final Exception e) {
            logger.warn("Failed to parse extracted data", e);
            return null;
        }
    }

    private static String convert(final com.wultra.app.enrollmentserver.model.enumeration.DocumentType source) {
        return switch (source) {
            case ID_CARD -> "personal_id";
            case PASSPORT -> "passport";
            case DRIVING_LICENSE -> "drivers_license";
            case SELFIE_PHOTO -> "photo";
            default -> source.name().toLowerCase();
        };
    }

    private static EmbeddedPhotoCreateRequest convert(final ProcessedDocumentDataEntity source) {
        final var photoData = Base64.getEncoder().encodeToString(source.getData());
        return EmbeddedPhotoCreateRequest.builder()
                .photoType(convert(source.getDataType()))
                .photoData(photoData)
                .externalId(source.getId())
                .build();
    }

    private static String convert(final ProcessedDocumentDataType source) {
        return switch (source) {
            case FACE_IMAGE -> "person";
            case DOCUMENT_FRONT_SIDE -> "document_front_side";
            case DOCUMENT_BACK_SIDE -> "document_back_side";
        };
    }

    private record DocumentsWrapper(
            Map.Entry<DocumentType, List<DocumentVerificationEntity>> primaryDocuments,
            Map<DocumentType, List<DocumentVerificationEntity>> otherDocuments) {
    }
}
