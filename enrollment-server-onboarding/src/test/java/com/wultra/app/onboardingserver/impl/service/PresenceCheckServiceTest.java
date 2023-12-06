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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Test for {@link PresenceCheckService}.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class PresenceCheckServiceTest {

    @Mock
    private IdentityVerificationService identityVerificationService;

    @Mock
    private DocumentVerificationRepository documentVerificationRepository;

    @InjectMocks
    private PresenceCheckService tested;

    @Test
    void testFetchTrustedPhotoFromDocumentVerifier_reverseOrder() throws Exception {
        final OwnerId ownerId = new OwnerId();
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();

        final DocumentVerificationEntity docPhotoDrivingLicense = new DocumentVerificationEntity();
        docPhotoDrivingLicense.setPhotoId("drivingLicensePhotoId");
        docPhotoDrivingLicense.setType(DocumentType.DRIVING_LICENSE);

        final DocumentVerificationEntity docPhotoIdCard = new DocumentVerificationEntity();
        docPhotoIdCard.setPhotoId("idCardPhotoId");
        docPhotoIdCard.setType(DocumentType.ID_CARD);

        final List<DocumentVerificationEntity> documentsReversedOrder = List.of(docPhotoDrivingLicense, docPhotoIdCard);

        when(documentVerificationRepository.findAllWithPhoto(identityVerification))
                .thenReturn(documentsReversedOrder);
        when(identityVerificationService.getPhotoById(docPhotoIdCard.getPhotoId(), ownerId))
                .thenReturn(Image.builder().build());

        tested.fetchTrustedPhotoFromDocumentVerifier(ownerId, identityVerification);

        verify(identityVerificationService, times(1)).getPhotoById(docPhotoIdCard.getPhotoId(), ownerId);
    }

    @Test
    void testFetchTrustedPhotoFromDocumentVerifier_unknownDocument() throws Exception {
        final OwnerId ownerId = new OwnerId();
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();

        final DocumentVerificationEntity docPhotoUnknown = new DocumentVerificationEntity();
        docPhotoUnknown.setPhotoId("unknownPhotoId");
        docPhotoUnknown.setType(DocumentType.UNKNOWN);

        when(documentVerificationRepository.findAllWithPhoto(identityVerification))
                .thenReturn(List.of(docPhotoUnknown));
        when(identityVerificationService.getPhotoById(docPhotoUnknown.getPhotoId(), ownerId))
                .thenReturn(Image.builder().build());

        tested.fetchTrustedPhotoFromDocumentVerifier(ownerId, identityVerification);

        verify(identityVerificationService, times(1)).getPhotoById(docPhotoUnknown.getPhotoId(), ownerId);
    }

}
