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
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.request.EmbeddedPhotoCreateRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
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
@Service
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

        final var processOptional = onboardingProcessRepository.findById(processId);
        if (processOptional.isEmpty()) {
            logger.warn("action: storeDocumentData, state: failed, reason: process_not_found, processId: {}", processId);
            return;
        }

        final var process = processOptional.get();
        final var identityOptional = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(process.getActivationId());
        if (identityOptional.isEmpty()) {
            logger.warn("action: storeDocumentData, state: failed, reason: identity_not_found, processId: {}", processId);
            return;
        }
        final var identity = identityOptional.get();

        final var documentVerifications = identity.getDocumentVerifications();
        if (CollectionUtils.isEmpty(documentVerifications)) {
            logger.info("action: storeDocumentData, state: skipped, reason: no_document_verification, processId: {}", processId);
            return;
        }

        final var documentVerificationIds = documentVerifications.stream().map(DocumentVerificationEntity::getId).collect(Collectors.toSet());
        final var processedDataList = processedDocumentDataRepository.findAllByDocumentVerificationIds(documentVerificationIds);
        final var processedDataMap = processedDataList.stream()
                .collect(Collectors.groupingBy(ProcessedDocumentDataEntity::getDocumentVerificationId));

        for (final var verification : documentVerifications) {
            if (config.getDocumentType() == UserDataStoreConfigurationProperties.DocumentType.WITH_TRUSTED_IMAGE && !verification.isUsedForVerification()) {
                continue;
            }

            final var results = verification.getResults();
            if (results == null || results.isEmpty()) {
                continue;
            }
            final var latestResult = results.iterator().next();

            final List<EmbeddedPhotoCreateRequest> photos = new ArrayList<>();
            if (config.isStoreDocumentImageScan()) {
                final var currentProcessedData = processedDataMap.getOrDefault(verification.getId(), Collections.emptyList());
                for (final var pd : currentProcessedData) {
                    photos.add(mapToPhotoRequest(pd));
                }
            }

            final String documentData = fetchDocumentData(verification, latestResult);

            final var request = DocumentCreateRequest.builder()
                    .userId(process.getUserId())
                    .documentType(mapDocumentType(verification.getType()))
                    .dataType("claims")
                    .externalId(processId)
                    .documentData(documentData)
                    .attributes(Map.of("trustedImage", verification.isUsedForVerification()))
                    .photos(photos)
                    .build();

            try {
                userDataStoreClient.createDocument(request);
            } catch (Exception e) {
                logger.error("action: storeDocumentData, state: failed, processId: {}, error: {}", processId, e.getMessage(), e);
            }
        }
        logger.info("action: storeDocumentData, state: succeeded");
    }

    private @Nullable String fetchDocumentData(final DocumentVerificationEntity verification, final DocumentResultEntity documentResult) {
        if (config.isStoreExtractedData()) {
            return String.format("""
                    {"documentData":%s,"country":"%s"}""",
                    documentResult.getExtractedData(),
                    verification.getCountry());
        } else {
            return null;
        }
    }

    private static String mapDocumentType(final com.wultra.app.enrollmentserver.model.enumeration.DocumentType source) {
        return switch (source) {
            case ID_CARD -> "personal_id";
            case PASSPORT -> "passport";
            case DRIVING_LICENSE -> "drivers_license";
            case SELFIE_PHOTO -> "photo";
            default -> source.name().toLowerCase();
        };
    }

    private static EmbeddedPhotoCreateRequest mapToPhotoRequest(final ProcessedDocumentDataEntity source) {
        final var photoType = switch (source.getDataType()) {
            case FACE_IMAGE -> "person";
            case DOCUMENT_FRONT_SIDE -> "document_front_side";
            case DOCUMENT_BACK_SIDE -> "document_back_side";
        };
        final var photoData = Base64.getEncoder().encodeToString(source.getData());
        return new EmbeddedPhotoCreateRequest(photoType, photoData, source.getId());
    }
}
