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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientEvaluationDocumentCheckResultFactory}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ClientEvaluationDocumentCheckResultFactoryTest {

    private static final String NATIONALITY = """
            "Czech"
            """;
    private static final String DATE_OF_EXPIRY = """
            "2022-12-21"
            """;
    private static final String VERIFICATION_RESULT = "{}";
    private static final String PHOTO_ID = "3f6a3a6b-6c4b-4c4b-9b1a-3f3f9b9b1f6d";
    private static final int SCORE = 10;
    private static final String COUNTRY = "CZE";
    private static final String DOCUMENT_VERIFICATION_ID = "03e059b0-ff6f-40ab-8ba3-a62e65c0f31d";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Mock
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    private ClientEvaluationDocumentCheckResultFactory tested;

    @BeforeEach
    void setUp() {
        tested = new ClientEvaluationDocumentCheckResultFactory(OBJECT_MAPPER, processedDocumentDataRepository);
    }

    @Test
    void testCreate_nullDocumentVerifications() {
        // given
        // -

        // when
        final var result = tested.create(null, false);

        // then
        final var expected = createExpectedEmptyResult();
        assertEquals(expected, result);
    }

    @Test
    void testCreate_emptyDocumentVerifications() {
        // given
        // -

        // when
        final var result = tested.create(Set.of(), false);

        // then
        final var expected = createExpectedEmptyResult();
        assertEquals(expected, result);
    }

    @Test
    void testCreate_withoutExtractedData() {
        // given
        final var documentVerifications = Set.of(createDocumentVerification(null, null, null, null));

        // when
        final var result = tested.create(documentVerifications, false);

        // then
        final var expected = createExpectedResult(null, List.of(), null, null, null, null);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_withoutExtractedDataMergingDocumentSides() {
        // given
        final var documentVerifications = Set.of(
                createDocumentVerification(null, null, CardSide.FRONT, null),
                createDocumentVerification(null, null, CardSide.BACK, null));

        // when
        final var result = tested.create(documentVerifications, false);

        // then
        final var expected = createExpectedResult(null, List.of(), null, null, null, null);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_withExtractedData() {
        // given
        final var documentResults = Set.of(createDocumentResult(
                createExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, PHOTO_ID, null, SCORE));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expected = createExpectedResult(
                createExpectedDocumentData(),
                createExpectedImages(),
                createExpectedPerson(),
                SCORE,
                VERIFICATION_RESULT,
                COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_documentResultNotFound() {
        // given
        final var documentVerifications = Set.of(createDocumentVerification(Set.of(), null, null, null));

        // when
        final var exception = assertThrows(IllegalStateException.class,() -> tested.create(documentVerifications, true));

        // then
        assertEquals("Missing document result for documentVerificationId: 03e059b0-ff6f-40ab-8ba3-a62e65c0f31d", exception.getMessage());
    }

    @Test
    void testCreate_photoIdNotFound() {
        // given
        final var documentResults = Set.of(
                createDocumentResult(createExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, null, null, SCORE));

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expected = createExpectedResult(createExpectedDocumentData(), List.of(), createExpectedPerson(), SCORE, VERIFICATION_RESULT, COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_processedDocumentNotFound() {
        // given
        final var documentResults = Set.of(
                createDocumentResult(createExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, PHOTO_ID, null, SCORE));

        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(List.of());

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expected = createExpectedResult(createExpectedDocumentData(), List.of(), createExpectedPerson(), SCORE, VERIFICATION_RESULT, COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_manyDocumentResults_theLatestOneIsUsed() {
        // given
        final var documentResults = List.of(
                createDocumentResult(createExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT),
                createDocumentResult("{}", LocalDateTime.now().minusHours(1), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(new HashSet<>(documentResults), PHOTO_ID, null, SCORE));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expected = createExpectedResult(
                createExpectedDocumentData(),
                createExpectedImages(),
                createExpectedPerson(),
                SCORE,
                VERIFICATION_RESULT,
                COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_extractedDataParsingError() {
        // given
        final var documentResults = Set.of(
                createDocumentResult("invalid_json", LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, PHOTO_ID, null, SCORE));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var exptectedDocumentData = EvaluateClientRequest.DocumentData.builder().build();
        final var expectedPerson = EvaluateClientRequest.Person.builder().build();
        final var expected = createExpectedResult(exptectedDocumentData, createExpectedImages(), expectedPerson, SCORE, VERIFICATION_RESULT, null);

        assertEquals(expected, result);
    }

    @Test
    void testCreate_mergingDocumentSides() {
        // given
        final var documentResultsFrontDoc = Set.of(
                createDocumentResult(createExtractedDataJson(null, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentResultsBackDoc = Set.of(
                createDocumentResult(createExtractedDataJson(NATIONALITY, null), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResultsFrontDoc, PHOTO_ID, CardSide.FRONT, SCORE),
                createDocumentVerification(documentResultsBackDoc, PHOTO_ID, CardSide.BACK, SCORE));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expected = createExpectedResult(
                createExpectedDocumentData(),
                createExpectedImages(),
                createExpectedPerson(),
                SCORE,
                VERIFICATION_RESULT,
                COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_emptyExtractedData() {
        // given
        final var documentResults = Set.of(
                createDocumentResult("{}", LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, PHOTO_ID, null, null));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expectedData = EvaluateClientRequest.DocumentData.builder().build();
        final var expectedPerson = EvaluateClientRequest.Person.builder().build();

        final var expected = createExpectedResult(expectedData, createExpectedImages(), expectedPerson, null, VERIFICATION_RESULT, null);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_nullExtractedData() {
        // given
        final var documentResults = Set.of(
                createDocumentResult(null, LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, PHOTO_ID, null, SCORE));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expectedData = EvaluateClientRequest.DocumentData.builder().build();
        final var expectedPerson = EvaluateClientRequest.Person.builder().build();

        final var expected = createExpectedResult(expectedData, createExpectedImages(), expectedPerson, 10, VERIFICATION_RESULT, null);
        assertEquals(expected, result);
    }

    @Test
    void testCreate_verificationResultIsNull() {
        // given
        final var documentResults = Set.of(
                createDocumentResult(createExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), null));
        final var documentVerifications = Set.of(
                createDocumentVerification(documentResults, PHOTO_ID, null, SCORE));

        final var processedDocuments = List.of(createProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.create(documentVerifications, true);

        // then
        final var expected = createExpectedResult(
                createExpectedDocumentData(),
                createExpectedImages(),
                createExpectedPerson(),
                SCORE,
                null,
                COUNTRY);
        assertEquals(expected, result);
    }

    private static DocumentVerificationEntity createDocumentVerification(
            final Set<DocumentResultEntity> documentResults,
            final String photoId,
            final CardSide side,
            final Integer score
    ) {
        final var entity = new DocumentVerificationEntity();
        entity.setId(DOCUMENT_VERIFICATION_ID);
        entity.setType(DocumentType.ID_CARD);
        entity.setVerificationId("6d1f0b3e-2a8c-4f7c-b9e1-0c3f8a7b2d14");
        entity.setResults(documentResults);
        entity.setPhotoId(photoId);
        entity.setSide(side);
        entity.setVerificationScore(score);

        Optional.ofNullable(documentResults)
                .ifPresent(it -> it.forEach(r -> r.setDocumentVerification(entity)));

        return entity;
    }

    private static DocumentResultEntity createDocumentResult(
            final String extractedData,
            final LocalDateTime timestampCreated,
            final String verificationResult
    ) {
        final var entity = new DocumentResultEntity();
        entity.setVerificationResult(verificationResult);
        entity.setExtractedData(extractedData);
        entity.setTimestampCreated(Date.from(timestampCreated.toInstant(java.time.ZoneOffset.UTC)));
        return entity;
    }

    private static EvaluateClientRequest.DocumentCheckResult createExpectedEmptyResult() {
        return EvaluateClientRequest.DocumentCheckResult.builder()
                .documents(List.of())
                .build();
    }

    private static EvaluateClientRequest.DocumentCheckResult createExpectedResult(
            final EvaluateClientRequest.DocumentData data,
            final List<EvaluateClientRequest.Image> images,
            final EvaluateClientRequest.Person person,
            final Integer score,
            final String rawData,
            final String country
    ) {
        return EvaluateClientRequest.DocumentCheckResult.builder()
                .documents(List.of(
                        EvaluateClientRequest.Document.builder()
                                .status(EvaluateClientRequest.Status.SUCCESS)
                                .type(DocumentType.ID_CARD)
                                .data(data)
                                .images(images)
                                .score(score)
                                .rawData(rawData)
                                .country(country)
                                .build()
                ))
                .person(person)
                .build();
    }

    private static EvaluateClientRequest.DocumentData createExpectedDocumentData() {
        return EvaluateClientRequest.DocumentData.builder()
                .givenNames("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1999, 2, 15))
                .placeOfBirth("Ostrava")
                .sex("M")
                .nationality("Czech")
                .personalNumber("123456789")
                .documentNumber("778899")
                .dateOfIssue(LocalDate.of(2012, 11, 20))
                .dateOfExpiry(LocalDate.of(2022, 12, 21))
                .authority("MeUO")
                .build();
    }

    private static EvaluateClientRequest.Person createExpectedPerson() {
        return EvaluateClientRequest.Person.builder()
                .givenNames("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1999, 2, 15))
                .build();
    }

    private static ProcessedDocumentDataEntity createProcessedDocument() {
        final var entity = new ProcessedDocumentDataEntity();
        entity.setId(PHOTO_ID);
        entity.setDataType(ProcessedDocumentDataType.FACE_IMAGE);
        entity.setData(new byte[] { 0, 1, 2 });
        return entity;
    }

    private static String createExtractedDataJson(final String nationality, final String dateOfExpiry) {
        return """
                {
                    "givenNames": "John",
                    "surname": "Doe",
                    "dateOfBirth": "1999-02-15",
                    "placeOfBirth": "Ostrava",
                    "sex": "M",
                    "nationality": %s,
                    "personalNumber": "123456789",
                    "documentNumber": "778899",
                    "dateOfIssue": "2012-11-20",
                    "dateOfExpiry": %s,
                    "authority": "MeUO",
                    "country": "CZE"
                }
                """.formatted(nationality, dateOfExpiry);
    }

    private static List<EvaluateClientRequest.Image> createExpectedImages() {
        return List.of(
                EvaluateClientRequest.Image.builder()
                        .type(ProcessedDocumentDataType.FACE_IMAGE)
                        .data(new byte[] { 0, 1, 2 })
                        .build()
        );
    }
}
