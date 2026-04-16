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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.impl.service.OnboardingProcessConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RequiredDocumentTypesCheck}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class RequiredDocumentTypesCheckTest {

    @Spy
    @SuppressWarnings("unused") // Used by Mockito in @InjectMocks
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    @InjectMocks
    private RequiredDocumentTypesCheck tested;

    @Test
    void testProcessIdNotFound() {
        // given
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenThrow(new IllegalArgumentException("process not found test exception"));

        // when
        final var exception = assertThrows(IllegalArgumentException.class, () -> tested.evaluate(List.of(), "1"));

        // then
        assertEquals("process not found test exception", exception.getMessage());
    }

    @Test
    void testEmptyCollection() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        // when
        final var result = tested.evaluate(Collections.emptyList(), "1");

        // then
        assertFalse(result);
    }

    @Test
    void testOnlyDrivingLicence() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testOnlyIdCardFailed() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testOnlyIdCardSuccessful() {
        // given
        final var processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .totalRequiredDocumentsCount((byte) 1)
                        .groups(Set.of(
                                OnboardingProcessConfigurationValue.Group.builder()
                                        .requiredDocumentsCount((byte) 1)
                                        .items(Set.of(
                                                OnboardingProcessConfigurationValue.Document.builder()
                                                        .type(OnboardingProcessConfigurationValue.DocumentType.ID_CARD)
                                                        .sideCount((byte) 2)
                                                        .build(),
                                                OnboardingProcessConfigurationValue.Document.builder()
                                                        .type(OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENSE)
                                                        .sideCount((byte) 1)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();

        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testIdCardAndDrivingLicence() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testIdCardOneSideOnlyAndDrivingLicence() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testIdCardSameSidesAndDrivingLicence() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testTravelPassportAndDrivingLicenceSuccessful() {
        // given
        final var processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                                .totalRequiredDocumentsCount((byte) 2)
                                .groups(Set.of(
                                        OnboardingProcessConfigurationValue.Group.builder()
                                                .requiredDocumentsCount((byte) 1)
                                                .items(Set.of(
                                                        OnboardingProcessConfigurationValue.Document.builder()
                                                                .type(OnboardingProcessConfigurationValue.DocumentType.ID_CARD)
                                                                .sideCount((byte) 2)
                                                                .build(),
                                                        OnboardingProcessConfigurationValue.Document.builder()
                                                                .type(OnboardingProcessConfigurationValue.DocumentType.PASSPORT)
                                                                .sideCount((byte) 1)
                                                                .build()
                                                ))
                                                .build()
                                ))
                        .build())
                .build();

        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.PASSPORT),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testTravelPassportAndDrivingLicenceFailed() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.PASSPORT),
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testIdCardAndTravelPassport() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK),
                createDocumentVerification(DocumentType.PASSPORT));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testTwoIdCards() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testOtherDocumentsCards() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK),
                createDocumentVerification(DocumentType.PASSPORT),
                createDocumentVerification(DocumentType.UNKNOWN),
                createDocumentVerification(DocumentType.SELFIE_PHOTO));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testTwoTravelPassports() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.PASSPORT),
                createDocumentVerification(DocumentType.PASSPORT));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testTravelPassportAndDrivingLicenceButInvalidStatus() {
        // given
        final var processConfig = buildProcessConfiguration();
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final DocumentVerificationEntity travelPassport = createDocumentVerification(DocumentType.PASSPORT);
        travelPassport.setStatus(DocumentStatus.VERIFICATION_IN_PROGRESS);
        final var documentVerifications = List.of(
                travelPassport,
                createDocumentVerification(DocumentType.DRIVING_LICENSE));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testEvaluate_countryNotRequiredAndDocumentResultIsMissing_validationPass() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry(null);
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testEvaluate_countryNotRequiredAndCountryIsExtracted_validationPass() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry(null);
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), "{\"country\":\"CZE\"}")
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testEvaluate_countryRequiredAndDocumentResultIsMissing_validationFail() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testEvaluate_countryRequiredAndExtractedDataIsMissing_validationFail() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), null)
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testEvaluate_countryRequiredAndCountryNotInExtractedData_validationFail() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), "{}")
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testEvaluate_requiredCountryDoesNotMatchExtractedOne_validationFail() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), "{\"country\":\"DEU\"}")
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    @Test
    void testEvaluate_requiredCountryMatchesExtractedOne_validationPass() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), "{\"country\":\"CZE\"}")
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testEvaluate_countryRequiredAndMultipleDocumentResults_validationPass() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now().minusMinutes(1), "{\"country\":\"DEU\"}"),
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), "{\"country\":\"CZE\"}")
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK));

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testEvaluate_countryRequiredAndFrontAndBackSideContainsDifferentCountry_validationFail() {
        // given
        final var processConfig = buildProcessConfigurationWithIdCardAndCountry("CZE");
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var idCardFrontVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT);
        idCardFrontVerification.setResults(Set.of(
                createDocumentResult(idCardFrontVerification, LocalDateTime.now(), "{\"country\":\"DEU\"}")
        ));

        final var idCardBackVerification = createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK);
        idCardBackVerification.setResults(Set.of(
                createDocumentResult(idCardBackVerification, LocalDateTime.now(), "{\"country\":\"CZE\"}")
        ));

        final var documentVerifications = List.of(
                idCardFrontVerification,
                idCardBackVerification);

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertFalse(result);
    }

    private DocumentVerificationEntity createDocumentVerification(final DocumentType type) {
        return createDocumentVerification(type, null);
    }

    private static DocumentVerificationEntity createDocumentVerification(final DocumentType type, final CardSide side) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setType(type);
        documentVerification.setSide(side);
        documentVerification.setStatus(DocumentStatus.ACCEPTED);
        return documentVerification;
    }

    private static OnboardingProcessConfigurationValue buildProcessConfiguration() {
        return OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .totalRequiredDocumentsCount((byte) 2)
                        .groups(Set.of(
                                OnboardingProcessConfigurationValue.Group.builder()
                                        .requiredDocumentsCount((byte) 1)
                                        .items(Set.of(
                                                OnboardingProcessConfigurationValue.Document.builder()
                                                        .type(OnboardingProcessConfigurationValue.DocumentType.ID_CARD)
                                                        .sideCount((byte) 2)
                                                        .build()
                                        ))
                                        .build(),
                                OnboardingProcessConfigurationValue.Group.builder()
                                        .items(Set.of(
                                                OnboardingProcessConfigurationValue.Document.builder()
                                                        .type(OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENSE)
                                                        .sideCount((byte) 1)
                                                        .build(),
                                                OnboardingProcessConfigurationValue.Document.builder()
                                                        .type(OnboardingProcessConfigurationValue.DocumentType.PASSPORT)
                                                        .sideCount((byte) 1)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
    }

    private static OnboardingProcessConfigurationValue buildProcessConfigurationWithIdCardAndCountry(final String country) {
        return OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .totalRequiredDocumentsCount((byte) 1)
                        .groups(Set.of(
                                OnboardingProcessConfigurationValue.Group.builder()
                                        .requiredDocumentsCount((byte) 1)
                                        .items(Set.of(
                                                OnboardingProcessConfigurationValue.Document.builder()
                                                        .type(OnboardingProcessConfigurationValue.DocumentType.ID_CARD)
                                                        .sideCount((byte) 2)
                                                        .country(country)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
    }

    private static DocumentResultEntity createDocumentResult(final DocumentVerificationEntity documentVerification, final LocalDateTime created, final String extractedData) {
        final var entity = new DocumentResultEntity();
        entity.setDocumentVerification(documentVerification);
        entity.setTimestampCreated(Date.from(created.toInstant(ZoneOffset.UTC)));
        entity.setExtractedData(extractedData);
        return entity;
    }
}
