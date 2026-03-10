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

package com.wultra.app.onboardingserver.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentExtractedDataValue;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;

/**
 * Factory for creating {@link EvaluateClientRequest.DocumentCheckResult}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class ClientEvaluationDocumentCheckResultFactory {

    private final ObjectMapper mapper;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    public EvaluateClientRequest.DocumentCheckResult create(final Set<DocumentVerificationEntity> documents, final boolean includeExtractedData) {
        if (CollectionUtils.isEmpty(documents)) {
            return EvaluateClientRequest.DocumentCheckResult.builder()
                    .documents(List.of())
                    .build();
        }

        return includeExtractedData ? createWithExtractedData(documents) : createWithoutExtractedData(documents);
    }

    private EvaluateClientRequest.DocumentCheckResult createWithExtractedData(final Set<DocumentVerificationEntity> documentsVerification) {
        final var documentVerificationIds = documentsVerification.stream()
                .map(DocumentVerificationEntity::getId)
                .collect(toSet());

        final var processedDocumentByDocumentVerificationId = fetchProcessedDocuments(documentVerificationIds);

        final var documentsVerificationByDocumentType = documentsVerification.stream()
                .collect(Collectors.groupingBy(
                        DocumentVerificationEntity::getType,
                        () -> new EnumMap<>(DocumentType.class),
                        Collectors.toList()
                ));

        final var documents = documentsVerificationByDocumentType.keySet().stream()
                .map(it -> createDocument(it, documentsVerificationByDocumentType, processedDocumentByDocumentVerificationId))
                .toList();

        final var person = buildPerson(documents);

        return new EvaluateClientRequest.DocumentCheckResult(documents, person);
    }

    private static EvaluateClientRequest.Person buildPerson(final List<EvaluateClientRequest.Document> documents) {
        String surname = null;
        String givenNames = null;
        LocalDate dateOfBirth = null;

        for (final var document : documents) {
            final var documentData = document.data();
            if (documentData == null) {
                return null;
            }

            if (surname == null) {
                surname = documentData.surname();
            }

            if (givenNames == null) {
                givenNames = documentData.givenNames();
            }

            if (dateOfBirth == null) {
                dateOfBirth = documentData.dateOfBirth();
            }
        }

        return EvaluateClientRequest.Person.builder()
                .surname(surname)
                .givenNames(givenNames)
                .dateOfBirth(dateOfBirth)
                .build();
    }

    private EvaluateClientRequest.Document createDocument(
            final DocumentType documentType,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentsVerificationByDocumentType,
            final Map<String, List<ProcessedDocumentDataEntity>> processedDocumentByDocumentVerificationId
    ) {
        final var documentVerifications = documentsVerificationByDocumentType.get(documentType);
        final var documentsResult = documentVerifications.stream()
                .map(ClientEvaluationDocumentCheckResultFactory::selectLatestDocumentResult)
                .toList();

        final var extractedData = documentsResult.stream()
                .map(this::parseExtractedData)
                .filter(Objects::nonNull)
                .toList();

        final var documentData = buildDocumentData(extractedData);

        final var country = extractedData.stream()
                .map(DocumentExtractedDataValue::country)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        final var documentVerificationIds = documentVerifications.stream()
                .map(DocumentVerificationEntity::getId)
                .collect(toSet());

        final var processedDocuments = processedDocumentByDocumentVerificationId.entrySet().stream()
                .filter(it -> documentVerificationIds.contains(it.getKey()))
                .flatMap(it -> it.getValue().stream())
                .filter(Objects::nonNull)
                .toList();

        final var images = buildImages(processedDocuments);

        final var score = documentVerifications.stream()
                .map(DocumentVerificationEntity::getVerificationScore)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        final var documentResult = documentsResult.stream()
                .map(DocumentResultEntity::getVerificationResult)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return EvaluateClientRequest.Document.builder()
                .type(documentType)
                .country(country)
                .status(EvaluateClientRequest.Status.SUCCESS) // so far the request is sent only in case of success
                .score(score)
                .data(documentData)
                .images(images)
                .rawData(documentResult)
                .build();
    }

    private static EvaluateClientRequest.DocumentData buildDocumentData(final List<DocumentExtractedDataValue> extractedData) {
        return EvaluateClientRequest.DocumentData.builder()
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
                .build();
    }

    private static <T> T findFirstValue(final Function<DocumentExtractedDataValue, T> getter, final List<DocumentExtractedDataValue> values) {
        return values.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static DocumentResultEntity selectLatestDocumentResult(final DocumentVerificationEntity documentVerificationEntity) {
        return documentVerificationEntity.getResults().stream()
                .max(Comparator.comparing(DocumentResultEntity::getTimestampCreated))
                .orElseThrow(() -> new IllegalStateException("Missing document result for documentVerificationId: %s".formatted(documentVerificationEntity.getId())));
    }

    private DocumentExtractedDataValue parseExtractedData(final DocumentResultEntity documentResult) {
        try {
            final var extractedData = documentResult.getExtractedData();

            return StringUtils.hasLength(extractedData) ?
                    mapper.readValue(extractedData, DocumentExtractedDataValue.class) :
                    null;
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse extracted data for document result id {}", documentResult.getId(), e);
            return null;
        }
    }

    private Map<String, List<ProcessedDocumentDataEntity>> fetchProcessedDocuments(final Set<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        return processedDocumentDataRepository.findAllByDocumentVerificationIds(ids).stream()
                .collect(Collectors.groupingBy(
                        ProcessedDocumentDataEntity::getDocumentVerificationId,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(
                                        ProcessedDocumentDataEntity::getDataType,
                                        Collectors.maxBy(Comparator.comparing(ProcessedDocumentDataEntity::getTimestampCreated))
                                ),
                                typeMap -> typeMap.values().stream()
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .toList()
                        )
                ));
    }

    private static List<EvaluateClientRequest.Image> buildImages(final List<ProcessedDocumentDataEntity> processedDocuments) {
        if (processedDocuments == null) {
            return List.of();
        }

        return processedDocuments.stream()
                .map(it -> EvaluateClientRequest.Image.builder()
                        .type(it.getDataType())
                        .data(it.getData())
                        .build())
                .toList();
    }

    private static EvaluateClientRequest.DocumentCheckResult createWithoutExtractedData(final Set<DocumentVerificationEntity> documentsVerification) {
        final var documents = documentsVerification.stream()
                .map(DocumentVerificationEntity::getType)
                .distinct()
                .map(ClientEvaluationDocumentCheckResultFactory::buildDocumentWithoutExtractedData)
                .toList();

        return new EvaluateClientRequest.DocumentCheckResult(documents, null);
    }

    private static EvaluateClientRequest.Document buildDocumentWithoutExtractedData(final DocumentType documentType) {
        return EvaluateClientRequest.Document.builder()
                .type(documentType)
                .status(EvaluateClientRequest.Status.SUCCESS)
                .images(new ArrayList<>())
                .build();
    }
}
