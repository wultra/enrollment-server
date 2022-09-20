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
package com.wultra.app.onboardingserver.statemachine;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.common.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@MockBean(IdentityVerificationRepository.class)
class DocumentUploadTransitionsTest extends AbstractStateMachineTest {

    @MockBean
    private DocumentVerificationRepository documentVerificationRepository;

    @Test
    void testDocumentUploadInProgress() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        when(documentVerificationRepository.findAllUsedForVerification(idVerification))
                .thenReturn(Collections.emptyList());

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.DOCUMENT_UPLOAD_IN_PROGRESS)
                .and()
                .build()
                .test();
    }

    @Test
    void testDocumentVerificationPending() throws Exception {
        IdentityVerificationEntity idVerification =
                createIdentityVerification(IdentityVerificationPhase.DOCUMENT_UPLOAD, IdentityVerificationStatus.IN_PROGRESS);
        StateMachine<OnboardingState, OnboardingEvent> stateMachine = createStateMachine(idVerification);

        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setStatus(DocumentStatus.VERIFICATION_PENDING);

        when(documentVerificationRepository.findAllUsedForVerification(idVerification))
                .thenReturn(List.of(documentVerification));

        Message<OnboardingEvent> message =
                stateMachineService.createMessage(OWNER_ID, idVerification.getProcessId(), OnboardingEvent.EVENT_NEXT_STATE);

        prepareTest(stateMachine)
                .sendEvent(message)
                .expectState(OnboardingState.DOCUMENT_UPLOAD_VERIFICATION_PENDING)
                .and()
                .build()
                .test();
    }

}
