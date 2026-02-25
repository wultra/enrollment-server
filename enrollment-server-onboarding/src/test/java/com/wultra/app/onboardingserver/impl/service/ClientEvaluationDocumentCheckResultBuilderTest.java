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

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientEvaluationDocumentCheckResultBuilder}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ClientEvaluationDocumentCheckResultBuilderTest {

    private static final String NATIONALITY = "\"Czech\"";
    private static final String DATE_OF_EXPIRY = "\"2022-12-21\"";
    private static final String VERIFICATION_RESULT = "{}";
    private static final String PHOTO_ID = "3f6a3a6b-6c4b-4c4b-9b1a-3f3f9b9b1f6d";
    private static final int SCORE = 10;
    private static final String COUNTRY = "CZE";
    private static final String DOCUMENT_VERIFICATION_ID = "03e059b0-ff6f-40ab-8ba3-a62e65c0f31d";

    @Mock
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    @InjectMocks
    private ClientEvaluationDocumentCheckResultBuilder tested;

    @Test
    void testBuild_nullDocumentVerifications() {
        // given
        // -

        // when
        final var result = tested.build(null, false);

        // then
        final var expected = buildExpectedEmptyResult();
        assertEquals(expected, result);
    }

    @Test
    void testBuild_emptyDocumentVerifications() {
        // given
        // -

        // when
        final var result = tested.build(Set.of(), false);

        // then
        final var expected = buildExpectedEmptyResult();
        assertEquals(expected, result);
    }

    @Test
    void testBuild_withoutExtractedData() {
        // given
        final var documentVerifications = Set.of(buildDocumentVerification(null, null, null));

        // when
        final var result = tested.build(documentVerifications, false);

        // then
        final var expected = buildExpectedResult(null, List.of(), null, null, null, null);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_withoutExtractedDataMergingDocumentSides() {
        // given
        final var documentVerifications = Set.of(
                buildDocumentVerification(null, null, CardSide.FRONT),
                buildDocumentVerification(null, null, CardSide.BACK));

        // when
        final var result = tested.build(documentVerifications, false);

        // then
        final var expected = buildExpectedResult(null, List.of(), null, null, null, null);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_withExtractedData() {
        // given
        final var documentResults = Set.of(buildDocumentResult(
                buildExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, PHOTO_ID, null));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expected = buildExpectedResult(
                buildExpectedDocumentData(),
                buildExpectedImages(),
                buildExpectedPerson(),
                SCORE,
                VERIFICATION_RESULT,
                COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_documentResultNotFound() {
        // given
        final var documentVerifications = Set.of(buildDocumentVerification(Set.of(), null, null));

        // when
        final var exception = assertThrows(IllegalStateException.class,() -> tested.build(documentVerifications, true));

        // then
        assertEquals("Missing document result for documentVerificationId: 03e059b0-ff6f-40ab-8ba3-a62e65c0f31d", exception.getMessage());
    }

    @Test
    void testBuild_photoIdNotFound() {
        // given
        final var documentResults = Set.of(
                buildDocumentResult(buildExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, null, null));

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expected = buildExpectedResult(buildExpectedDocumentData(), List.of(), buildExpectedPerson(), SCORE, VERIFICATION_RESULT, COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_processedDocumentNotFound() {
        // given
        final var documentResults = Set.of(
                buildDocumentResult(buildExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, PHOTO_ID, null));

        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(List.of());

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expected = buildExpectedResult(buildExpectedDocumentData(), List.of(), buildExpectedPerson(), SCORE, VERIFICATION_RESULT, COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_manyDocumentResults_theLatestOneIsUsed() {
        // given
        final var documentResults = List.of(
                buildDocumentResult(buildExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT),
                buildDocumentResult("{}", LocalDateTime.now().minusHours(1), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(new HashSet<>(documentResults), PHOTO_ID, null));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expected = buildExpectedResult(
                buildExpectedDocumentData(),
                buildExpectedImages(),
                buildExpectedPerson(),
                SCORE,
                VERIFICATION_RESULT,
                COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_extractedDataParsingError() {
        // given
        final var documentResults = Set.of(
                buildDocumentResult("invalid_json", LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, PHOTO_ID, null));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var exptectedDocumentData = EvaluateClientRequest.DocumentData.builder().build();
        final var expectedPerson = EvaluateClientRequest.Person.builder().build();
        final var expected = buildExpectedResult(exptectedDocumentData, buildExpectedImages(), expectedPerson, SCORE, VERIFICATION_RESULT, null);

        assertEquals(expected, result);
    }

    @Test
    void testBuild_mergingDocumentSides() {
        // given
        final var documentResultsFrontDoc = Set.of(
                buildDocumentResult(buildExtractedDataJson(null, DATE_OF_EXPIRY), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentResultsBackDoc = Set.of(
                buildDocumentResult(buildExtractedDataJson(NATIONALITY, null), LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResultsFrontDoc, PHOTO_ID, CardSide.FRONT),
                buildDocumentVerification(documentResultsBackDoc, PHOTO_ID, CardSide.BACK));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expected = buildExpectedResult(
                buildExpectedDocumentData(),
                buildExpectedImages(),
                buildExpectedPerson(),
                SCORE,
                VERIFICATION_RESULT,
                COUNTRY);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_emptyExtractedData() {
        // given
        final var documentResults = Set.of(
                buildDocumentResult("{}", LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, PHOTO_ID, null));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expectedData = EvaluateClientRequest.DocumentData.builder().build();
        final var expectedPerson = EvaluateClientRequest.Person.builder().build();

        final var expected = buildExpectedResult(expectedData, buildExpectedImages(), expectedPerson, 10, VERIFICATION_RESULT, null);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_nullExtractedData() {
        // given
        final var documentResults = Set.of(
                buildDocumentResult(null, LocalDateTime.now(), VERIFICATION_RESULT));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, PHOTO_ID, null));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expectedData = EvaluateClientRequest.DocumentData.builder().build();
        final var expectedPerson = EvaluateClientRequest.Person.builder().build();

        final var expected = buildExpectedResult(expectedData, buildExpectedImages(), expectedPerson, 10, VERIFICATION_RESULT, null);
        assertEquals(expected, result);
    }

    @Test
    void testBuild_verificationResultIsNull() {
        // given
        final var documentResults = Set.of(
                buildDocumentResult(buildExtractedDataJson(NATIONALITY, DATE_OF_EXPIRY), LocalDateTime.now(), null));
        final var documentVerifications = Set.of(
                buildDocumentVerification(documentResults, PHOTO_ID, null));

        final var processedDocuments = List.of(buildProcessedDocument());
        when(processedDocumentDataRepository.findAllById(Set.of(PHOTO_ID))).thenReturn(processedDocuments);

        // when
        final var result = tested.build(documentVerifications, true);

        // then
        final var expected = buildExpectedResult(
                buildExpectedDocumentData(),
                buildExpectedImages(),
                buildExpectedPerson(),
                SCORE,
                null,
                COUNTRY);
        assertEquals(expected, result);
    }

    private static DocumentVerificationEntity buildDocumentVerification(
            final Set<DocumentResultEntity> documentResults,
            final String photoId,
            final CardSide side
    ) {
        final var entity = new DocumentVerificationEntity();
        entity.setId(DOCUMENT_VERIFICATION_ID);
        entity.setType(DocumentType.ID_CARD);
        entity.setVerificationId("6d1f0b3e-2a8c-4f7c-b9e1-0c3f8a7b2d14");
        entity.setResults(documentResults);
        entity.setPhotoId(photoId);
        entity.setSide(side);

        Optional.ofNullable(documentResults)
                .ifPresent(it -> it.forEach(r -> r.setDocumentVerification(entity)));

        return entity;
    }

    private static DocumentResultEntity buildDocumentResult(
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

    private static EvaluateClientRequest.DocumentCheckResult buildExpectedEmptyResult() {
        return EvaluateClientRequest.DocumentCheckResult.builder()
                .documents(List.of())
                .build();
    }

    private static EvaluateClientRequest.DocumentCheckResult buildExpectedResult(
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

    private static EvaluateClientRequest.DocumentData buildExpectedDocumentData() {
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

    private static EvaluateClientRequest.Person buildExpectedPerson() {
        return EvaluateClientRequest.Person.builder()
                .givenNames("John")
                .surname("Doe")
                .dateOfBirth(LocalDate.of(1999, 2, 15))
                .build();
    }

    private static ProcessedDocumentDataEntity buildProcessedDocument() {
        final var entity = new ProcessedDocumentDataEntity();
        entity.setId(PHOTO_ID);
        entity.setDataType(ProcessedDocumentDataType.FACE_IMAGE);
        entity.setData(new byte[] { 0, 1, 2 });
        return entity;
    }

    private static String buildExtractedDataJson(final String nationality, final String dateOfExpiry) {
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

    private static List<EvaluateClientRequest.Image> buildExpectedImages() {
        return List.of(
                EvaluateClientRequest.Image.builder()
                        .type(ProcessedDocumentDataType.FACE_IMAGE)
                        .data(new byte[] { 0, 1, 2 })
                        .build()
        );
    }
}
