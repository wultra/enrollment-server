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
package com.wultra.app.onboardingserver.impl.service.document;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DocumentVerificationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class DocumentVerificationServiceTest {

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @InjectMocks
    private DocumentVerificationService tested;

    @Test
    void testHasDocumentsVerificationPending_true() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);
        
        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(documentVerification));

        assertTrue(tested.hasDocumentsVerificationPending(identityVerification));
    }

    @Test
    void testHasDocumentsVerificationPending_false() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.ACCEPTED);

        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of(documentVerification));

        assertFalse(tested.hasDocumentsVerificationPending(identityVerification));
    }

    @Test
    void testHasDocumentsVerificationPending_empty() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();

        when(documentVerificationRepository.findAllUsedForVerification(identityVerification))
                .thenReturn(List.of());

        assertFalse(tested.hasDocumentsVerificationPending(identityVerification));
    }
}
