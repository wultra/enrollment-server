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

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.api.provider.PresenceCheckProvider;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.ScaResultRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.internal.JsonSerializationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.DOCUMENT_VERIFICATION_FINAL;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.PRESENCE_CHECK;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test for {@link PresenceCheckService}.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class PresenceCheckServiceTest {

    @MockBean
    private IdentityVerificationService identityVerificationService;

    @MockBean
    private DocumentVerificationRepository documentVerificationRepository;

    @MockBean
    private PresenceCheckLimitService presenceCheckLimitService;

    @MockBean
    private PresenceCheckProvider presenceCheckProvider;

    @Autowired
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

    @Test
    void initPresentCheckWithImage_withDocumentReferences() throws Exception {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("a1");

        final DocumentVerificationEntity page1 = new DocumentVerificationEntity();
        page1.setId("1");
        page1.setType(DocumentType.ID_CARD);
        page1.setSide(CardSide.FRONT);
        page1.setPhotoId("id_card_portrait");

        final DocumentVerificationEntity page2 = new DocumentVerificationEntity();
        page2.setId("2");
        page2.setType(DocumentType.ID_CARD);
        page2.setSide(CardSide.BACK);
        page2.setPhotoId("id_card_portrait");

        final DocumentVerificationEntity page3 = new DocumentVerificationEntity();
        page3.setId("3");
        page3.setType(DocumentType.DRIVING_LICENSE);
        page3.setSide(CardSide.FRONT);
        page3.setPhotoId("driving_licence_portrait");

        when(presenceCheckProvider.trustedPhotoSource()).thenReturn(PresenceCheckProvider.TrustedPhotoSource.REFERENCE);

        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setPhase(PRESENCE_CHECK);
        identityVerification.setStatus(NOT_INITIALIZED);

        when(documentVerificationRepository.findAllWithPhoto(identityVerification)).thenReturn(List.of(page1, page2, page3));
        when(identityVerificationService.findByOptional(ownerId)).thenReturn(Optional.of(identityVerification));
        when(presenceCheckProvider.startPresenceCheck(ownerId)).thenReturn(new SessionInfo());

        tested.init(ownerId, "p1");

        assertTrue(identityVerification.getSessionInfo().contains("\"primaryDocumentReference\":\"id_card_portrait\""));
        assertTrue(identityVerification.getSessionInfo().contains("\"otherDocumentsReferences\":[\"driving_licence_portrait\"]"));
        verify(presenceCheckProvider).initPresenceCheck(ownerId, null);
    }

}
