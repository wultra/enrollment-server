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

import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
class PresenceCheckServiceTest {

    @Mock
    private IdentityVerificationService identityVerificationService;

    @InjectMocks
    private PresenceCheckService service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void selectPhotoForPresenceCheckTest() throws Exception {
        OwnerId ownerId = new OwnerId();

        // Two documents with person photo in reversed order of preference
        DocumentVerificationEntity docPhotoDrivingLicense = new DocumentVerificationEntity();
        docPhotoDrivingLicense.setPhotoId("drivingLicensePhotoId");
        docPhotoDrivingLicense.setType(DocumentType.DRIVING_LICENSE);

        DocumentVerificationEntity docPhotoIdCard = new DocumentVerificationEntity();
        docPhotoIdCard.setPhotoId("idCardPhotoId");
        docPhotoIdCard.setType(DocumentType.ID_CARD);

        List<DocumentVerificationEntity> documentsReversedOrder = List.of(docPhotoDrivingLicense, docPhotoIdCard);

        service.selectPhotoForPresenceCheck(ownerId, documentsReversedOrder);
        when(identityVerificationService.getPhotoById(docPhotoIdCard.getPhotoId(), ownerId)).thenReturn(Image.builder().build());
        verify(identityVerificationService, times(1)).getPhotoById(docPhotoIdCard.getPhotoId(), ownerId);

        // Unknown document with a person photo
        DocumentVerificationEntity docPhotoUnknown = new DocumentVerificationEntity();
        docPhotoUnknown.setPhotoId("unknownPhotoId");
        docPhotoUnknown.setType(DocumentType.UNKNOWN);

        List<DocumentVerificationEntity> documentUnknown = List.of(docPhotoUnknown);

        service.selectPhotoForPresenceCheck(ownerId, documentUnknown);
        when(identityVerificationService.getPhotoById(docPhotoUnknown.getPhotoId(), ownerId)).thenReturn(Image.builder().build());
        verify(identityVerificationService, times(1)).getPhotoById(docPhotoUnknown.getPhotoId(), ownerId);
    }

}
