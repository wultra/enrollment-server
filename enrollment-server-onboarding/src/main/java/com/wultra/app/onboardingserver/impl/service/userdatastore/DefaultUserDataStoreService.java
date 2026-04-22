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

import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.security.userdatastore.client.UserDataStoreClient;
import com.wultra.security.userdatastore.client.model.error.UserDataStoreClientException;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.request.EmbeddedPhotoCreateRequest;
import com.wultra.security.userdatastore.client.model.response.DocumentCreateResponse;
import com.wultra.security.userdatastore.client.model.response.EmbeddedPhotoCreateResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserDataStoreService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@AllArgsConstructor
@Slf4j
class DefaultUserDataStoreService implements UserDataStoreService {

    private static final String DATA_TYPE_CLAIMS = "claims";

    private final UserDataStoreClient userDataStoreClient;

    private final UserDataStoreConfigProperties config;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

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

        final var documentVerifications = identityVerification.getDocumentVerifications();
        if (CollectionUtils.isEmpty(documentVerifications)) {
            logger.info("action: collectDocumentData, state: skipped, reason: no_document_verification, processId: {}", processId);
            return List.of();
        }

        final var processedData = fetchProcessedDocumentData(documentVerifications);

        final List<DocumentCreateRequest> documentRequests = documentVerifications.stream()
                .map(documentVerification -> createDocumentRequest(processId, process.getUserId(), documentVerification, processedData))
                .filter(Objects::nonNull)
                .toList();

        logger.info("action: collectDocumentData, state: finished, processId: {}, count: {}", processId, documentRequests.size());
        return documentRequests;
    }

    @Override
    public void storeDocumentData(final List<DocumentCreateRequest> requests) {
        logger.info("action: storeDocumentData, state: initiated, count: {}", requests.size());
        for (final var request : requests) {
            callCreateDocument(request);
        }
        logger.info("action: storeDocumentData, state: finished");
    }

    private Map<String, List<ProcessedDocumentDataEntity>> fetchProcessedDocumentData(final Collection<DocumentVerificationEntity> documentVerifications) {
        final var documentVerificationIds = documentVerifications.stream()
                .map(DocumentVerificationEntity::getId)
                .collect(Collectors.toSet());
        return processedDocumentDataRepository.findAllByDocumentVerificationIds(documentVerificationIds).stream()
                .collect(Collectors.groupingBy(ProcessedDocumentDataEntity::getDocumentVerificationId));
    }

    private @Nullable DocumentCreateRequest createDocumentRequest(final String processId, final String userId, final DocumentVerificationEntity documentVerification, final Map<String, List<ProcessedDocumentDataEntity>> processedData) {
        if (config.getDocumentType() == UserDataStoreConfigProperties.DocumentType.WITH_TRUSTED_IMAGE && !documentVerification.isUsedForVerification()) {
            return null;
        }

        final var results = documentVerification.getResults();
        if (CollectionUtils.isEmpty(results)) {
            return null;
        }
        final var latestResult = results.iterator().next();

        final List<EmbeddedPhotoCreateRequest> photos = fetchPhotos(processedData.getOrDefault(documentVerification.getId(), List.of()));

        return DocumentCreateRequest.builder()
                .userId(userId)
                .documentType(convert(documentVerification.getType()))
                .dataType(DATA_TYPE_CLAIMS)
                .externalId(processId)
                .documentData(fetchExtractedData(latestResult))
                .attributes(Map.of("trustedImage", documentVerification.isUsedForVerification()))
                .photos(photos)
                .build();
    }

    void callCreateDocument(final DocumentCreateRequest request) {
        logger.info("action: callCreateDocument, state: initiated, userId: {}, externalId: {}, documentType: {}, dataType: {}", request.userId(), request.externalId(), request.documentType(), request.dataType());

        try {
            final var response = userDataStoreClient.createDocument(request);
            logger.info("action: callCreateDocument, state: succeeded, documentId: {}, photoIds: {}", response.id(), collectPhotoIds(response));
        } catch (final UserDataStoreClientException e) {
            // TODO Lubos retry pattern, could be done in #1725
            logger.error("action: callCreateDocument, state: failed, errorMessage: {}", e.getMessage(), e);
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

    private @Nullable String fetchExtractedData(final DocumentResultEntity documentResult) {
        if (!config.isStoreExtractedData()) {
            return null;
        }

        return documentResult.getExtractedData();
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
        final var photoType = switch (source.getDataType()) {
            case FACE_IMAGE -> "person";
            case DOCUMENT_FRONT_SIDE -> "document_front_side";
            case DOCUMENT_BACK_SIDE -> "document_back_side";
        };
        final var photoData = Base64.getEncoder().encodeToString(source.getData());
        return new EmbeddedPhotoCreateRequest(photoType, photoData, source.getId());
    }
}
