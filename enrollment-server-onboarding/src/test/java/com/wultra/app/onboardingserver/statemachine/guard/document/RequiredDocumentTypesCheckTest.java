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

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.impl.service.OnboardingProcessConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
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

    @Mock
    private OnboardingProcessConfigurationService onboardingProcessConfigurationService;

    @InjectMocks
    private RequiredDocumentTypesCheck tested;

    private OnboardingProcessConfigurationValue processConfig;

    @BeforeEach
    void setUp() {
        processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .requiredTotalDocumentsCount((byte) 2)
                        .requiredPrimaryDocumentsCount((byte) 1)
                        .primary(Set.of(OnboardingProcessConfigurationValue.DocumentType.ID_CARD))
                        .items(List.of(
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                        (byte) 2),
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE,
                                        (byte) 1)
                        ))
                        .build())
                .build();
    }

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
        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        // when
        final var result = tested.evaluate(Collections.emptyList(), "1");

        // then
        assertFalse(result);
    }

    @Test
    void testOnlyDrivingLicence() {
        // given
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
        processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .requiredTotalDocumentsCount((byte) 1)
                        .requiredPrimaryDocumentsCount((byte) 1)
                        .primary(Set.of(OnboardingProcessConfigurationValue.DocumentType.ID_CARD))
                        .items(List.of(
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                        (byte) 2),
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE,
                                        (byte) 1)
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
        processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .requiredTotalDocumentsCount((byte) 2)
                        .requiredPrimaryDocumentsCount((byte) 1)
                        .primary(Set.of(
                                OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                OnboardingProcessConfigurationValue.DocumentType.PASSPORT))
                        .items(List.of(
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                        (byte) 2),
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.PASSPORT,
                                        (byte) 1)
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
    void testEvaluate_allConfigPropertiesAreSpecifiedAndMatched_returnsTrue() {
        // given
        processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .requiredTotalDocumentsCount((byte) 3)
                        .requiredPrimaryDocumentsCount((byte) 1)
                        .mandatory(Set.of(OnboardingProcessConfigurationValue.DocumentType.ID_CARD))
                        .primary(Set.of(OnboardingProcessConfigurationValue.DocumentType.PASSPORT))
                        .secondary(Set.of(OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE))
                        .items(List.of(
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                        (byte) 2),
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.PASSPORT,
                                        (byte) 1),
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE,
                                        (byte) 1)
                        ))
                        .build())
                .build();

        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(
                createDocumentVerification(DocumentType.ID_CARD, CardSide.FRONT),
                createDocumentVerification(DocumentType.ID_CARD, CardSide.BACK),
                createDocumentVerification(DocumentType.PASSPORT),
                createDocumentVerification(DocumentType.DRIVING_LICENSE)
        );

        // when
        boolean result = tested.evaluate(documentVerifications, "1");

        // then
        assertTrue(result);
    }

    @Test
    void testEvaluate_anyMandatoryDocumentMissing_returnsFalse() {
        // given
        processConfig = OnboardingProcessConfigurationValue.builder()
                .documents(OnboardingProcessConfigurationValue.Documents.builder()
                        .requiredTotalDocumentsCount((byte) 2)
                        .mandatory(Set.of(
                                OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE))
                        .items(List.of(
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.ID_CARD,
                                        (byte) 2),
                                new OnboardingProcessConfigurationValue.Document(
                                        OnboardingProcessConfigurationValue.DocumentType.DRIVING_LICENCE,
                                        (byte) 1)
                        ))
                        .build())
                .build();

        when(onboardingProcessConfigurationService.findConfigByProcessId("1")).thenReturn(processConfig);

        final var documentVerifications = List.of(createDocumentVerification(DocumentType.ID_CARD));

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
}
