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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserDataStoreService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnProperty(name = "enrollment-server-onboarding.user-data-store.enabled", havingValue = "true")
@AllArgsConstructor
@Slf4j
class DefaultUserDataStoreService implements UserDataStoreService {

    private final UserDataStoreClient userDataStoreClient;

    private final UserDataStoreConfigurationProperties config;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    @Transactional(readOnly = true)
    @Override
    public void storeDocumentData(final String processId) {
        logger.info("action: storeDocumentData, state: initiated, processId: {}", processId);

        final var process = onboardingProcessRepository.findById(processId).orElse(null);
        if (process == null) {
            logger.warn("action: storeDocumentData, state: failed, reason: process_not_found, processId: {}", processId);
            return;
        }

        final var identityVerification = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(process.getActivationId()).orElse(null);
        if (identityVerification == null) {
            logger.warn("action: storeDocumentData, state: failed, reason: identity_not_found, processId: {}", processId);
            return;
        }

        final var documentVerifications = identityVerification.getDocumentVerifications();
        if (CollectionUtils.isEmpty(documentVerifications)) {
            logger.info("action: storeDocumentData, state: skipped, reason: no_document_verification, processId: {}", processId);
            return;
        }

        final var processedDataMap = fetchProcessedDocumentData(documentVerifications);

        for (final var documentVerification : documentVerifications) {
            storeDocumentVerification(processId, process.getUserId(), documentVerification, processedDataMap);
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

    private void storeDocumentVerification(final String processId, final String userId, final DocumentVerificationEntity documentVerification, final Map<String, List<ProcessedDocumentDataEntity>> processedDataMap) {
        if (config.getDocumentType() == UserDataStoreConfigurationProperties.DocumentType.WITH_TRUSTED_IMAGE && !documentVerification.isUsedForVerification()) {
            return;
        }

        final var results = documentVerification.getResults();
        if (CollectionUtils.isEmpty(results)) {
            return;
        }
        final var latestResult = results.iterator().next();

        final List<EmbeddedPhotoCreateRequest> photos = fetchPhotos(processedDataMap.getOrDefault(documentVerification.getId(), Collections.emptyList()));

        final String documentData = fetchDocumentData(documentVerification, latestResult);

        final var request = DocumentCreateRequest.builder()
                .userId(userId)
                .documentType(convert(documentVerification.getType()))
                .dataType("claims")
                .externalId(processId)
                .documentData(documentData)
                .attributes(Map.of("trustedImage", documentVerification.isUsedForVerification()))
                .photos(photos)
                .build();

        try {
            userDataStoreClient.createDocument(request);
            // TODO retry pattern
            logger.error("action: storeDocumentData, state: succeeded, processId: {}, documentVerificationId: {}", processId, documentVerification.getVerificationId());
        } catch (UserDataStoreClientException e) {
            logger.error("action: storeDocumentData, state: failed, processId: {}, documentVerificationId: {}, error: {}", processId, documentVerification.getVerificationId(), e.getMessage(), e);
        }
    }

    private List<EmbeddedPhotoCreateRequest> fetchPhotos(final List<ProcessedDocumentDataEntity> processedData) {
        if (!config.isStoreDocumentImageScan()) {
            return List.of();
        }

        return processedData.stream()
                .map(DefaultUserDataStoreService::convert)
                .toList();
    }

    private @Nullable String fetchDocumentData(final DocumentVerificationEntity verification, final DocumentResultEntity documentResult) {
        if (!config.isStoreExtractedData()) {
            return null;
        }

        return String.format("{\"documentData\":%s,\"country\":\"%s\"}",
                documentResult.getExtractedData(),
                verification.getCountry());
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
