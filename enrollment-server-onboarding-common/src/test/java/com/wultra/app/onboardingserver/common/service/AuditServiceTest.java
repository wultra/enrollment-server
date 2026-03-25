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
package com.wultra.app.onboardingserver.common.service;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.core.audit.base.Audit;
import com.wultra.core.audit.base.model.AuditDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuditService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final String DOCUMENT_VERIFICATION_PROVIDER_CODE = "documentVerificationProvider";

    private static final String ACTIVATION_ID_PARAM = "activationId";
    private static final String USER_ID_PARAM = "userId";
    private static final String DOCUMENT_RESPONSE_JSON_PARAM = "documentResponseJson";

    private static final String ACTIVATION_ID = "test-activation-id";
    private static final String USER_ID = "test-user-id";

    @Mock
    private Audit audit;

    @InjectMocks
    private AuditService tested;

    @Captor
    private ArgumentCaptor<AuditDetail> auditDetailCaptor;

    @Test
    void testAuditDocumentVerificationProvider() {
        // given
        final var ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        ownerId.setUserId(USER_ID);

        final var documentResponseJson = """
                { "result": "PASSED" }""";

        final var message = "Test message for user: {}";

        // when
        tested.auditDocumentVerificationProvider(ownerId, documentResponseJson, message, USER_ID);

        // then
        final Map<String, Object> expectedParams = Map.of(
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID,
                DOCUMENT_RESPONSE_JSON_PARAM, documentResponseJson
        );

        verify(audit).info(eq(message), auditDetailCaptor.capture(), eq(USER_ID));

        assertAuditDetail(auditDetailCaptor.getValue(), DOCUMENT_VERIFICATION_PROVIDER_CODE, expectedParams);
    }

    private static void assertAuditDetail(final AuditDetail auditDetail, final String expectedType, final Map<String, Object> expectedParams) {
        assertEquals(expectedType, auditDetail.getType());
        assertEquals(expectedParams, auditDetail.getParam());
    }
}