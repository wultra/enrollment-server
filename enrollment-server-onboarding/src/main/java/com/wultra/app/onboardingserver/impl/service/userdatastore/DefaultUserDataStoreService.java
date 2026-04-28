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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserDataStoreService}.
 *
 * @implSpec Processing {@link ProcessedDocumentDataEntity} which is used by {@link MicroblinkDocumentVerificationProvider} but not by {@link ZenidDocumentVerificationProvider}.
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@RequiredArgsConstructor
@Slf4j
class DefaultUserDataStoreService implements UserDataStoreService {

    private static final int MAX_ATTEMPTS = 3;

    private static final String DATA_TYPE_CLAIMS = "claims";

    private final UserDataStoreClient userDataStoreClient;

    private final UserDataStoreConfigProperties config;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final ObjectMapper objectMapper;

    private final RetryTemplate retryTemplate = RetryTemplate.builder()
            .maxAttempts(MAX_ATTEMPTS)
            .exponentialBackoff(200, 2.0, 2_000)
            .build();

    @Transactional(readOnly = true)
    @Override
    public List<DocumentCreateRequest> collectDocumentData(final String processId) {
        logger.info("action: collectDocumentData, state: initiated, processId: {}", processId);

        final var process = onboardingProcessRepository.findById(processId).orElse(null);
        if (process == null) {
            logger.warn("action: collectDocumentData, state: failed, reason: process_not_found, processId: {}", processId);
            return List.of();
        }

        final var identityVerification = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(process.getActivationId()).orElse(null);
        if (identityVerification == null) {
            logger.warn("action: collectDocumentData, state: failed, reason: identity_not_found, processId: {}", processId);
            return List.of();
        }

        final var documentVerifications = fetchDocumentVerifications(identityVerification, config.getDocumentType());
        if (documentVerifications.primaryDocuments().getValue().isEmpty()) {
            logger.info("action: collectDocumentData, state: skipped, reason: no_document_verification, processId: {}", processId);
            return List.of();
        }

        final List<DocumentCreateRequest> documentRequests = new ArrayList<>();
        documentRequests.add(createTrustedDocumentRequest(process, documentVerifications.primaryDocuments()));

        for (final var entry : documentVerifications.otherDocuments().entrySet()) {
            documentRequests.add(createDocumentRequest(process, entry));
        }

        logger.info("action: collectDocumentData, state: finished, processId: {}, count: {}", processId, documentRequests.size());
        return documentRequests;
    }

    @Override
    public void storeDocumentData(final List<DocumentCreateRequest> requests) throws UserDataStoreClientException {
        logger.info("action: storeDocumentData, state: initiated, count: {}", requests.size());

        for (final var request : requests) {
            retryTemplate.execute(context -> callCreateDocument(request, context));
        }
        logger.info("action: storeDocumentData, state: finished");
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

        return createDocumentRequest(process, source, Map.of("trustedImage", true));
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

        final Map.Entry<DocumentType, List<DocumentVerificationEntity>> primaryDocuments = DocumentType.PREFERRED_SOURCE_OF_PERSON_PHOTO.stream()
                .map(type -> Map.entry(type, documentVerifications.get(type)))
                .filter(entry -> entry.getValue() != null)
                .findFirst()
                .orElseGet(() -> {
                    logger.warn("Unable to select a preferred source of person photo, selecting the first one, identityVerificationId: {}", idVerification);
                    return documentVerifications.entrySet().iterator().next();
                });

        documentVerifications.remove(primaryDocuments.getKey());

        return switch (documentType) {
            case ALL -> new DocumentsWrapper(primaryDocuments, documentVerifications);
            case WITH_TRUSTED_IMAGE -> new DocumentsWrapper(primaryDocuments, Map.of());
        };
    }

    Void callCreateDocument(final DocumentCreateRequest request, final RetryContext context) throws UserDataStoreClientException {
        final int attempt = context.getRetryCount() + 1;
        logger.info("action: callCreateDocument, state: initiated, userId: {}, externalId: {}, documentType: {}, dataType: {}, attempt {}/{}",
                request.userId(), request.externalId(), request.documentType(), request.dataType(), attempt, MAX_ATTEMPTS);

        try {
            final var response = userDataStoreClient.createDocument(request);
            logger.info("action: callCreateDocument, state: succeeded, documentId: {}, photoIds: {}", response.id(), collectPhotoIds(response));
            return null;
        } catch (final UserDataStoreClientException e) {
            logger.warn("action: callCreateDocument, state: failed, attempt {}/{}, errorMessage: {}", attempt, MAX_ATTEMPTS, e.getMessage(), e);
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

        List<DocumentExtractedDataValue> extractedData =
                documentVerifications.stream()
                .map(DocumentVerificationEntity::getResults)
                .map(it -> it.stream().findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(DocumentResultEntity::getExtractedData)
                .map(this::convert)
                .filter(Objects::nonNull)
                .toList();

        final DocumentExtractedDataValue mergedData = merge(extractedData);

        try {
            return objectMapper.writeValueAsString(mergedData);
        } catch (JsonProcessingException e) {
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
            logger.warn("Failed to parse extracted data, source: {}", source, e);
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
