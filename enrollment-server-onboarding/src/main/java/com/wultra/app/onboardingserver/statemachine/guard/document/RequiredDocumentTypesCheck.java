/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine.guard.document;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.impl.service.OnboardingProcessConfigurationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.wultra.app.enrollmentserver.model.enumeration.DocumentType.*;

/**
 * Validate presence of all required documents.
 * <p>
 * It means a primary document (ID card or travel passport), and another document (e.g. driving licence).
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO (racansky, 2022-09-09) should be Guard for Spring State Machine, but called from job so far
@Component
@AllArgsConstructor
@Slf4j
public class RequiredDocumentTypesCheck {

    private final OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    /**
     * Evaluate whether the provided documents meet all requirements defined in {@link OnboardingProcessConfigurationValue#documents()}.
     *
     * Following checks are performed:
     * - Minimal total document count is provided
     * - Each document has required sides count
     * - Minimal document count from each group is provided
     *
     * @param documentVerifications document verifications to evaluate
     * @param processId onboarding process identification
     * @return true when all requirements are met
     */
    public boolean evaluate(
            final Collection<DocumentVerificationEntity> documentVerifications,
            final String processId
    ) {
        final var processConfig = onboardingProcessConfigurationService.findConfigByProcessId(processId);

        final var documentVerificationsByType = documentVerifications.stream()
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(Collectors.groupingBy(DocumentVerificationEntity::getType));

        if (!isMinimalDocumentCountProvided(documentVerificationsByType, processConfig)) {
            return false;
        }

        final var groupChecksPassed = processConfig.documents().groups().stream()
                .allMatch(group ->
                        isMinimalDocumentCountFromGroupProvided(documentVerificationsByType, group)
                                && areAllDocumentsSidesProvided(documentVerificationsByType, group)
                );

        if (!groupChecksPassed) {
            return false;
        }

        logger.debug("All required documents accepted for onboarding process: {}", processId);
        return true;
    }

    private static boolean isMinimalDocumentCountProvided(
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
            final OnboardingProcessConfigurationValue processConfig
    ) {
        final var providedCount = documentVerificationsByType.size();
        final var requiredCount = processConfig.documents().totalRequiredDocumentsCount();

        if (providedCount < requiredCount) {
            logger.debug("Minimal document count not provided. Required: {}, provided: {}", requiredCount, providedCount);
            return false;
        }

        return true;
    }

    private static boolean isMinimalDocumentCountFromGroupProvided(
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
            final OnboardingProcessConfigurationValue.Group group
    ) {
        final var providedDocumentTypes = documentVerificationsByType.keySet();

        final var groupDocumentTypes = group.items()
                .stream()
                .map(OnboardingProcessConfigurationValue.Document::type)
                .map(RequiredDocumentTypesCheck::convertDocumentType)
                .collect(Collectors.toSet());

        final var providedCount = providedDocumentTypes.stream()
                .filter(groupDocumentTypes::contains)
                .count();

        final var requiredCount = group.requiredDocumentsCount();

        if (providedCount < requiredCount) {
            logger.debug("Minimal document count from group {} not provided. Required: {}, provided: {}", groupDocumentTypes, requiredCount, providedCount);
            return false;
        }

        return true;
    }

    private static boolean areAllDocumentsSidesProvided(
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
            final OnboardingProcessConfigurationValue.Group group
    ) {
        final var requiredDocumentTypeToSideCount = group.items()
                .stream()
                .collect(Collectors.toMap(
                        item -> convertDocumentType(item.type()),
                        OnboardingProcessConfigurationValue.Document::sideCount
                ));

        return requiredDocumentTypeToSideCount.entrySet().stream()
                .allMatch(i -> areAllDocumentSidesProvided(i.getKey(), i.getValue(), documentVerificationsByType));
    }

    private static boolean areAllDocumentSidesProvided(
            final DocumentType documentType,
            final byte requiredSideCount,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType
    ) {
        if (!documentVerificationsByType.containsKey(documentType)) {
            return true;
        }

        final var providedSideCount = documentVerificationsByType.get(documentType)
                .stream()
                .map(DocumentVerificationEntity::getSide)
                .distinct()
                .count();

        if (providedSideCount < requiredSideCount) {
            logger.debug("Not all sides provided for document type: {}. Required sides: {}, provided sides: {}", documentType, requiredSideCount, providedSideCount);
            return false;
        }

        return true;
    }

    private static DocumentType convertDocumentType(final OnboardingProcessConfigurationValue.DocumentType documentType) {
        return switch (documentType) {
            case ID_CARD -> ID_CARD;
            case PASSPORT -> PASSPORT;
            case DRIVING_LICENCE -> DRIVING_LICENSE;
        };
    }
}
