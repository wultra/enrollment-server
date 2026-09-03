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
package com.wultra.app.onboardingserver.impl.service.verification;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.RejectOrigin;
import com.wultra.app.enrollmentserver.model.integration.DocumentVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.DocumentResultRepository;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ErrorDetail;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.impl.service.OnboardingEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link VerificationProcessingService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class VerificationProcessingServiceTest {

    @Mock
    private DocumentResultRepository documentResultRepository;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private OnboardingEventService onboardingEventService;

    @InjectMocks
    private VerificationProcessingService tested;

    @Test
    void testProcessVerificationResult_preservesRejectReasons() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);

        final DocumentVerificationEntity document = new DocumentVerificationEntity();
        document.setId("document-verification-id");
        document.setUploadId("upload-id");
        document.setStatus(DocumentStatus.VERIFICATION_PENDING);
        document.setIdentityVerification(identityVerification);

        final DocumentVerificationResult documentResult = DocumentVerificationResult.builder()
                .uploadId("upload-id")
                .rejectReason("Document expired")
                .build();
        final DocumentsVerificationResult result = DocumentsVerificationResult.builder()
                .verificationId("verification-id")
                .status(DocumentVerificationStatus.REJECTED)
                .rejectReason("At least one document was rejected")
                .results(List.of(documentResult))
                .build();

        tested.processVerificationResult(new OwnerId(), List.of(document), result);

        assertEquals(DocumentStatus.REJECTED, document.getStatus());
        assertEquals("At least one document was rejected", document.getRejectReason());
        assertEquals(RejectOrigin.DOCUMENT_VERIFICATION, document.getRejectOrigin());

        final ArgumentCaptor<DocumentResultEntity> resultCaptor = ArgumentCaptor.forClass(DocumentResultEntity.class);
        verify(documentResultRepository).save(resultCaptor.capture());
        assertEquals(DocumentProcessingPhase.VERIFICATION, resultCaptor.getValue().getPhase());
        assertEquals("Document expired", resultCaptor.getValue().getRejectReason());
        assertEquals(RejectOrigin.DOCUMENT_VERIFICATION, resultCaptor.getValue().getRejectOrigin());
        verify(onboardingEventService).publishDocumentVerificationFinished(document);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void testProcessVerificationResult_fallsBackForBlankRejectReason(final String rejectReason) {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setPhase(IdentityVerificationPhase.DOCUMENT_VERIFICATION);

        final DocumentVerificationEntity document = new DocumentVerificationEntity();
        document.setId("document-verification-id");
        document.setUploadId("upload-id");
        document.setStatus(DocumentStatus.VERIFICATION_PENDING);
        document.setIdentityVerification(identityVerification);

        final DocumentVerificationResult documentResult = DocumentVerificationResult.builder()
                .uploadId("upload-id")
                .build();
        final DocumentsVerificationResult result = DocumentsVerificationResult.builder()
                .verificationId("verification-id")
                .status(DocumentVerificationStatus.REJECTED)
                .rejectReason(rejectReason)
                .results(List.of(documentResult))
                .build();

        tested.processVerificationResult(new OwnerId(), List.of(document), result);

        assertEquals(DocumentStatus.REJECTED, document.getStatus());
        assertEquals(ErrorDetail.DOCUMENT_VERIFICATION_REJECTED, document.getRejectReason());
        assertEquals(RejectOrigin.DOCUMENT_VERIFICATION, document.getRejectOrigin());
        verify(onboardingEventService).publishDocumentVerificationFinished(document);
    }
}
