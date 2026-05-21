/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.OTP_VERIFICATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test for {@link IdentityVerificationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class IdentityVerificationServiceTest {

    @Mock
    private IdentityVerificationRepository identityVerificationRepository;

    @Mock
    private IdentityVerificationPrecompleteCheck identityVerificationPrecompleteCheck;

    @InjectMocks
    private IdentityVerificationService tested;

    @Test
    void testProcessVerificationResult_valid() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        when(identityVerificationPrecompleteCheck.evaluate(idVerification))
                .thenReturn(IdentityVerificationPrecompleteCheck.Result.successful());

        final var result = tested.processVerificationResult(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.FinalVerificationResult.OK));
    }

    @Test
    void testProcessVerificationResult_invalidPrecompleteGuard() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        when(identityVerificationPrecompleteCheck.evaluate(idVerification))
                .thenReturn(IdentityVerificationPrecompleteCheck.Result.failed("Not valid OTP"));

        final var result = tested.processVerificationResult(new OwnerId(), idVerification);

        assertThat(result, equalTo(IdentityVerificationService.FinalVerificationResult.FAILED));
    }

    @Test
    void testCreateDocsMetadata_hideRejectedErrorDetail() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setErrorDetail("Hide specific error occurred.");

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertHidden(errors);
    }

    @Test
    void testCreateDocsMetadata_hideRejectedRejectReason() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setRejectReason("Hide specific rejection reason.");

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertHidden(errors);
    }

    @Test
    void testCreateDocsMetadata_hideFailedErrorDetail() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.FAILED);
        doc.setErrorDetail("Hide some error occurred.");

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertHidden(errors);
    }

    @Test
    void testCreateDocsMetadata_accepted() {
        final DocumentVerificationEntity doc = new DocumentVerificationEntity();
        doc.setStatus(DocumentStatus.ACCEPTED);

        final List<String> errors = tested.createDocsMetadata(List.of(doc)).get(0).getErrors();
        assertTrue(CollectionUtils.isEmpty(errors));
    }

    private static void assertHidden(final List<String> errors) {
        assertEquals(1, errors.size());
        assertEquals("Error verifying the document.", errors.get(0));
    }

}
