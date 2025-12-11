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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final List<DocumentCheck> DOCUMENT_CHECKS = List.of(
            new MandatoryDocumentsPresentCheck(),
            new PrimaryDocumentsPresentCheck(),
            new AllDocumentSidesPresentCheck(),
            new TotalCountDocumentsPresentCheck()
    );

    private final OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    /**
     * Evaluate all required document types to be present and accepted.
     *
     * @param documentVerifications document verifications to evaluate
     * @param processId onboarding process identification
     * @return true when all required document types present and accepted
     */
    public boolean evaluate(
            final Collection<DocumentVerificationEntity> documentVerifications,
            final String processId
    ) {
        final var processConfig = onboardingProcessConfigurationService.findConfigByProcessId(processId);

        final var documentVerificationsByType = documentVerifications.stream()
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(Collectors.groupingBy(DocumentVerificationEntity::getType));

        for (final var check : DOCUMENT_CHECKS) {
            final var checkPassed = check.evaluate(documentVerificationsByType, processConfig);

            if (!checkPassed) {
                logger.debug("Check '{}' failed for onboarding process: {}", check.getName(), processId);
                return false;
            }
        }

        logger.debug("All required documents accepted for onboarding process: {}", processId);
        return true;
    }

    private static DocumentType convertDocumentType(final OnboardingProcessConfigurationValue.DocumentType documentType) {
        return switch (documentType) {
            case ID_CARD -> ID_CARD;
            case PASSPORT -> PASSPORT;
            case DRIVING_LICENCE -> DRIVING_LICENSE;
        };
    }

    private interface DocumentCheck {
        boolean evaluate(
                Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
                OnboardingProcessConfigurationValue processConfig
        );

        String getName();
    }

    private static class MandatoryDocumentsPresentCheck implements DocumentCheck {

        @Override
        public boolean evaluate(
                final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
                OnboardingProcessConfigurationValue processConfig
        ) {
            final var presentDocumentTypes = documentVerificationsByType.keySet();
            final var mandatoryDocumentTypes = processConfig.documents().mandatory()
                    .stream()
                    .map(RequiredDocumentTypesCheck::convertDocumentType)
                    .collect(Collectors.toSet());

            return presentDocumentTypes.containsAll(mandatoryDocumentTypes);
        }

        @Override
        public String getName() {
            return "MandatoryDocumentsPresentCheck";
        }
    }

    private static class PrimaryDocumentsPresentCheck implements DocumentCheck {

        @Override
        public boolean evaluate(
                final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
                OnboardingProcessConfigurationValue processConfig
        ) {
            final var presentDocumentTypes = documentVerificationsByType.keySet();
            final var primaryDocumentTypes = processConfig.documents().primary()
                    .stream()
                    .map(RequiredDocumentTypesCheck::convertDocumentType)
                    .collect(Collectors.toSet());
            final var requiredPrimaryDocumentsCount = processConfig.documents().requiredPrimaryDocumentsCount();

            return presentDocumentTypes.stream()
                    .filter(primaryDocumentTypes::contains)
                    .count() >= requiredPrimaryDocumentsCount;
        }

        @Override
        public String getName() {
            return "MandatoryDocumentsPresentCheck";
        }
    }

    private static class AllDocumentSidesPresentCheck implements DocumentCheck {

        @Override
        public boolean evaluate(
                final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
                OnboardingProcessConfigurationValue processConfig
        ) {
            final var presentDocumentSidesCountByType = documentVerificationsByType.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            i -> i.getValue().stream()
                                    .map(DocumentVerificationEntity::getSide)
                                    .distinct()
                                    .count())
                    );

            final var requiredDocumentSidesCountByType = processConfig.documents().items()
                    .stream()
                    .collect(Collectors.toMap(i -> convertDocumentType(i.type()), OnboardingProcessConfigurationValue.Document::sideCount));

            return presentDocumentSidesCountByType.entrySet()
                    .stream()
                    .allMatch(i -> i.getValue() >= requiredDocumentSidesCountByType.getOrDefault(i.getKey(), (byte) 0));
        }

        @Override
        public String getName() {
            return "AllDocumentSidesPresentCheck";
        }
    }

    private static class TotalCountDocumentsPresentCheck implements DocumentCheck {

        @Override
        public boolean evaluate(
                final Map<DocumentType, List<DocumentVerificationEntity>> documentVerificationsByType,
                OnboardingProcessConfigurationValue processConfig
        ) {
            final var documentsConfig = processConfig.documents();
            final var allowedDocumentTypes = Stream.of(documentsConfig.mandatory(), documentsConfig.primary(), documentsConfig.secondary())
                    .flatMap(Collection::stream)
                    .map(RequiredDocumentTypesCheck::convertDocumentType)
                    .collect(Collectors.toSet());

            final var presentDocumentCount = documentVerificationsByType.keySet()
                    .stream()
                    .filter(allowedDocumentTypes::contains)
                    .count();

            return presentDocumentCount >= documentsConfig.requiredTotalDocumentsCount();
        }

        @Override
        public String getName() {
            return "TotalCountDocumentsPresentCheck";
        }
    }
}
