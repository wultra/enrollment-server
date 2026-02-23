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

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;

/**
 * Builder for {@link EvaluateClientRequest.DocumentCheckResult}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class ClientEvaluationDocumentCheckResultBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    public EvaluateClientRequest.DocumentCheckResult build(final Set<DocumentVerificationEntity> documents, final boolean includeExtractedData) {
        return includeExtractedData ? buildWithExtractedData(documents) : buildWithoutExtractedData(documents);
    }

    private EvaluateClientRequest.DocumentCheckResult buildWithExtractedData(final Set<DocumentVerificationEntity> documentsVerification) {
        final var photoIds = documentsVerification.stream()
                .map(DocumentVerificationEntity::getPhotoId)
                .filter(Objects::nonNull)
                .collect(toSet());

        final var processedDocumentByPhotoId = fetchProcessedDocuments(photoIds);

        final var documentsVerificationByDocumentType = documentsVerification.stream()
                .collect(Collectors.groupingBy(
                        DocumentVerificationEntity::getType,
                        () -> new EnumMap<>(DocumentType.class),
                        Collectors.toList()
                ));

        final var documents = documentsVerificationByDocumentType.keySet().stream()
                .map(it -> buildDocument(it, documentsVerificationByDocumentType, processedDocumentByPhotoId))
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

    private EvaluateClientRequest.Document buildDocument(
            final DocumentType documentType,
            final Map<DocumentType, List<DocumentVerificationEntity>> documentsVerificationByDocumentType,
            final Map<String, ProcessedDocumentDataEntity> processedDocumentByPhotoId
    ) {
        final var documentVerification = documentsVerificationByDocumentType.get(documentType);
        final var documentsResult = documentVerification.stream()
                .map(ClientEvaluationDocumentCheckResultBuilder::selectLatestDocumentResult)
                .toList();

        final var extractedData = documentsResult.stream()
                .map(this::parseExtractedData)
                .filter(Objects::nonNull)
                .toList();

        final var documentData = buildDocumentData(extractedData);

        final var country = extractedData.stream()
                .findFirst()
                .map(DocumentExtractedDataValue::country)
                .orElse(null);

        final var processedDocument = documentVerification.stream()
                .findFirst()
                .map(DocumentVerificationEntity::getPhotoId)
                .map(it -> processedDocumentByPhotoId.getOrDefault(it, null))
                .orElse(null);

        final var images = buildImages(processedDocument);

        final var documentResult = documentsResult.stream()
                .findFirst()
                .map(DocumentResultEntity::getVerificationResult)
                .orElse(null);

        return EvaluateClientRequest.Document.builder()
                .type(documentType)
                .country(country)
                .status(EvaluateClientRequest.Status.SUCCESS) // so far the request is sent only in case of success
                .score(10) // so far sending constant 10 as 100 percent confidence, possible future extension point
                .data(documentData)
                .images(images)
                .rawData(documentResult)
                .build();
    }

    private static EvaluateClientRequest.DocumentData buildDocumentData(final List<DocumentExtractedDataValue> extractedData) {
        if (extractedData == null) {
            return null;
        }

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
                .findFirst()
                .orElse(null);
    }

    private static DocumentResultEntity selectLatestDocumentResult(final DocumentVerificationEntity documentVerificationEntity) {
        return documentVerificationEntity.getResults().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing document result for %s".formatted(documentVerificationEntity)));
    }

    private DocumentExtractedDataValue parseExtractedData(final DocumentResultEntity documentResult) {
        try {
            return OBJECT_MAPPER.readValue(documentResult.getExtractedData(), DocumentExtractedDataValue.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse extracted data for document result id {}", documentResult.getId(), e);
            return null;
        }
    }

    private Map<String, ProcessedDocumentDataEntity> fetchProcessedDocuments(final Set<String> ids) {
        return StreamSupport.stream(processedDocumentDataRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(ProcessedDocumentDataEntity::getId, Function.identity()));
    }

    private static List<EvaluateClientRequest.Image> buildImages(final ProcessedDocumentDataEntity processedDocumentData) {
        if (processedDocumentData == null) {
            return List.of();
        }

        return List.of(
                EvaluateClientRequest.Image.builder()
                        .type(processedDocumentData.getDataType())
                        .data(processedDocumentData.getData())
                        .build()
        );
    }

    private static EvaluateClientRequest.DocumentCheckResult buildWithoutExtractedData(final Set<DocumentVerificationEntity> documentsVerification) {
        final var documents = new ArrayList<EvaluateClientRequest.Document>(documentsVerification.size());

        for (final DocumentVerificationEntity documentVerification : documentsVerification) {
            final var document = EvaluateClientRequest.Document.builder()
                    .type(documentVerification.getType())
                    .status(EvaluateClientRequest.Status.SUCCESS)
                    .images(new ArrayList<>())
                    .build();

            documents.add(document);
        }

        return new EvaluateClientRequest.DocumentCheckResult(documents, null);
    }
}
