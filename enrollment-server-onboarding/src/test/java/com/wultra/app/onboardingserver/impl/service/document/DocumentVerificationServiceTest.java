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
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.RejectOrigin;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.impl.service.OnboardingEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DocumentVerificationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class DocumentVerificationServiceTest {

    @Mock
    private DocumentVerificationProvider documentVerificationProvider;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private OnboardingProcessRepository onboardingProcessRepository;

    @Mock
    private OnboardingProcessLimitService processLimitService;

    @Mock
    private AuditService auditService;

    @Mock
    private OnboardingEventService onboardingEventService;

    @InjectMocks
    private DocumentVerificationService tested;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void testExecuteFinalDocumentVerification_fallsBackForBlankRejectReason(final String rejectReason)
            throws RemoteCommunicationException, DocumentVerificationException {

        final OwnerId ownerId = new OwnerId();
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setId("document-verification-id");
        documentVerification.setUploadId("upload-id");
        documentVerification.setUsedForVerification(true);
        documentVerification.setStatus(DocumentStatus.ACCEPTED);

        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("identity-verification-id");
        identityVerification.setProcessId("process-id");
        identityVerification.setDocumentVerifications(Set.of(documentVerification));

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        final DocumentsVerificationResult result = DocumentsVerificationResult.builder()
                .status(DocumentVerificationStatus.REJECTED)
                .rejectReason(rejectReason)
                .build();
        when(documentVerificationProvider.verifyDocuments(ownerId, List.of("upload-id")))
                .thenReturn(result);
        when(onboardingProcessRepository.findById("process-id"))
                .thenReturn(Optional.of(process));

        final DocumentVerificationService.FinalDocumentVerificationResult verificationResult =
                tested.executeFinalDocumentVerification(identityVerification, ownerId);

        assertEquals(DocumentVerificationService.FinalDocumentVerificationResult.REJECTED, verificationResult);
        assertEquals(DocumentStatus.REJECTED, documentVerification.getStatus());
        assertEquals("Other", documentVerification.getRejectReason());
        assertEquals(RejectOrigin.DOCUMENT_VERIFICATION, documentVerification.getRejectOrigin());
        verify(onboardingEventService).publishFinalDocumentVerificationRejected(
                identityVerification,
                "Other");
    }

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
