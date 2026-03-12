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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.entity.DocumentExtractedDataValue;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.impl.service.OnboardingProcessConfigurationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private final ObjectMapper objectMapper;
    private final OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    /**
     * Evaluate whether the provided documents meet all requirements defined in {@link OnboardingProcessConfigurationValue#documents()}.
     *
     * The following checks are performed:
     * - Minimal total document count is provided
     * - Each document has a required sides count
     * - Minimal document count from each group is provided
     * - Country extracted from a document matches the required one, if set in the configuration
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

        final var groups = List.copyOf(processConfig.documents().groups());

        final var validationErrors = IntStream.range(0, groups.size())
                .mapToObj(groupIndex -> validateGroup(documentVerificationsByType, groups.get(groupIndex), groupIndex))
                .flatMap(Collection::stream)
                .toList();

        final var result = validationErrors.isEmpty();
        logger.debug("Required documents validation for processId: {}, result: {}, errors: {}", processId, result, validationErrors);
        return result;
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

    private List<String> validateGroup(
            final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
            final OnboardingProcessConfigurationValue.Group group,
            final int groupIndex
    ) {
        final var documentRequirementByDocumentType = group.items().stream()
                .collect(Collectors.toMap(i -> convert(i.type()), RequiredDocumentTypesCheck::convert));

        final var errors = new ArrayList<String>();

        for (final var entry : documentVerificationsByType.entrySet()) {
            final var documentType = entry.getKey();

            if (!documentRequirementByDocumentType.containsKey(documentType)) {
                continue;
            }

            final var documentRequirements = documentRequirementByDocumentType.get(documentType);

            final var documentVerifications = entry.getValue();

            final var sidesCount = documentVerifications.stream()
                    .map(DocumentVerificationEntity::getSide)
                    .distinct()
                    .count();
            if (sidesCount < documentRequirements.sideCount()) {
                errors.add("group %s, documentType %s: sideCount not matched".formatted(groupIndex, documentType));
            }

            final var country = getCountry(documentVerifications);
            final var requiredCountry = documentRequirements.country();
            if (!requiredCountry.isEmpty() && !requiredCountry.equals(country)) {
                errors.add("group %s, documentType %s: country not matched".formatted(groupIndex, documentType));
            }
        }

        final var documentsCount = documentRequirementByDocumentType.keySet().stream()
                .filter(documentVerificationsByType::containsKey)
                .count();

        if (documentsCount < group.requiredDocumentsCount()) {
            errors.add("group %s: requiredDocumentsCount not matched".formatted(groupIndex));
        }

        return errors;
    }

    private static DocumentType convert(final OnboardingProcessConfigurationValue.DocumentType documentType) {
        return switch (documentType) {
            case ID_CARD -> ID_CARD;
            case PASSPORT -> PASSPORT;
            case DRIVING_LICENCE -> DRIVING_LICENSE;
        };
    }

    private static DocumentValidationItem convert(final OnboardingProcessConfigurationValue.Document source) {
        final var country = Optional.ofNullable(source.country())
                .map(Set::of)
                .orElse(Set.of());

        return DocumentValidationItem.builder()
                .sideCount(source.sideCount())
                .country(country)
                .build();
    }

    private Set<String> getCountry(final List<DocumentVerificationEntity> source) {
        return source.stream()
                .map(this::getCountry)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String getCountry(final DocumentVerificationEntity entity) {
        return entity.getResults().stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(DocumentResultEntity::getTimestampCreated))
                .map(this::parseExtractedDataCountry)
                .orElse(null);
    }

    private String parseExtractedDataCountry(final DocumentResultEntity documentResult) {
        try {
            final var extractedData = documentResult.getExtractedData();
            if (extractedData == null) {
                return null;
            }

            return objectMapper.readValue(extractedData, DocumentExtractedDataValue.class)
                    .country();
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse extracted data for document result id {}", documentResult.getId(), e);
            return null;
        }
    }

    /*
     * The 'country' is a 'Set'. A document can have more than one side, and each side may have a different
     * country for various reasons (e.g., incorrect extraction by the provider, or the user uses different documents
     * for the front and back sides).
     */
    @Builder
    private record DocumentValidationItem(
        int sideCount,
        Set<String> country
    ) {}
}
