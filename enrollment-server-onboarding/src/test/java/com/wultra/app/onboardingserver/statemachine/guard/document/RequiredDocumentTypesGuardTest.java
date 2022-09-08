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
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test for {@link RequiredDocumentTypesGuard}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class RequiredDocumentTypesGuardTest {

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private StateContext<OnboardingState, OnboardingEvent> context;

    @Mock
    private ExtendedState state;

    private final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();

    @InjectMocks
    private RequiredDocumentTypesGuard tested;

    @BeforeEach
    void prepareMocks() {
        when(context.getExtendedState())
                .thenReturn(state);
        when(state.get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class))
                .thenReturn(identityVerification);
    }

    @Test
    void testOnlyDrivingLicence() {
        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(createDocumentVerification(DocumentType.DRIVING_LICENSE)));

        boolean result = tested.evaluate(context);
        assertFalse(result);
    }

    @Test
    void testOnlyIdCard() {
        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(createDocumentVerification(DocumentType.ID_CARD)));

        boolean result = tested.evaluate(context);
        assertFalse(result);
    }

    @Test
    void testIdCardAndDrivingLicence() {
        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(
                        createDocumentVerification(DocumentType.ID_CARD),
                        createDocumentVerification(DocumentType.DRIVING_LICENSE)));

        boolean result = tested.evaluate(context);
        assertTrue(result);
    }

    @Test
    void testTravelPassportAndDrivingLicence() {
        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(
                        createDocumentVerification(DocumentType.PASSPORT),
                        createDocumentVerification(DocumentType.DRIVING_LICENSE)));

        boolean result = tested.evaluate(context);
        assertTrue(result);
    }

    @Test
    void testIdCardAndDrivingLicenceButInvalidStatus() {
        final DocumentVerificationEntity idCard = createDocumentVerification(DocumentType.ID_CARD);
        idCard.setStatus(DocumentStatus.VERIFICATION_PENDING);

        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(
                        idCard,
                        createDocumentVerification(DocumentType.DRIVING_LICENSE)));

        boolean result = tested.evaluate(context);
        assertFalse(result);
    }

    private DocumentVerificationEntity createDocumentVerification(final DocumentType type) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setType(type);
        documentVerification.setStatus(DocumentStatus.ACCEPTED);
        return documentVerification;
    }
}
