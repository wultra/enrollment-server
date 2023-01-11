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

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.COMPLETED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.OTP_VERIFICATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.ACCEPTED;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

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
    private AuditService auditService;

    @Mock
    private IdentityVerificationPrecompleteCheck identityVerificationPrecompleteCheck;

    @InjectMocks
    private IdentityVerificationService tested;

    @Test
    void testProcessDocumentVerificationResult_valid() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        when(identityVerificationPrecompleteCheck.evaluate(idVerification))
                .thenReturn(IdentityVerificationPrecompleteCheck.Result.successful());

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        final ArgumentCaptor<IdentityVerificationEntity> argumentCaptor = ArgumentCaptor.forClass(IdentityVerificationEntity.class);
        verify(identityVerificationRepository, atLeastOnce()).save(argumentCaptor.capture());
        final IdentityVerificationEntity savedIdentityVerification = argumentCaptor.getValue();

        assertThat(savedIdentityVerification.getPhase(), equalTo(COMPLETED));
        assertThat(savedIdentityVerification.getStatus(), equalTo(ACCEPTED));
    }

    @Test
    void testProcessDocumentVerificationResult_invalidPrecompleteGuard() throws Exception {
        final IdentityVerificationEntity idVerification = new IdentityVerificationEntity();
        idVerification.setPhase(OTP_VERIFICATION);
        idVerification.setStatus(ACCEPTED);

        when(identityVerificationPrecompleteCheck.evaluate(idVerification))
                .thenReturn(IdentityVerificationPrecompleteCheck.Result.failed("Not valid OTP"));

        tested.processDocumentVerificationResult(new OwnerId(), idVerification);

        final ArgumentCaptor<IdentityVerificationEntity> argumentCaptor = ArgumentCaptor.forClass(IdentityVerificationEntity.class);
        verify(identityVerificationRepository, atLeastOnce()).save(argumentCaptor.capture());
        final IdentityVerificationEntity savedIdentityVerification = argumentCaptor.getValue();

        assertThat(savedIdentityVerification.getPhase(), equalTo(COMPLETED));
        assertThat(savedIdentityVerification.getStatus(), equalTo(FAILED));
        assertThat(savedIdentityVerification.getErrorDetail(), equalTo("documentVerificationFailed"));
        assertThat(savedIdentityVerification.getErrorOrigin(), equalTo(ErrorOrigin.FINAL_VALIDATION));
    }
}
