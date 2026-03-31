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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingOtpEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuditService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final String TYPE_PROCESS = "process";
    private static final String TYPE_OTP = "otp";
    private static final String TYPE_IDENTITY_VERIFICATION = "identityVerification";
    private static final String TYPE_ACTIVATION = "activation";
    private static final String TYPE_DOCUMENT_VERIFICATION = "documentVerification";
    private static final String TYPE_PRESENCE_CHECK_PROVIDER = "presenceCheckProvider";
    private static final String TYPE_DOCUMENT_VERIFICATION_PROVIDER = "documentVerificationProvider";
    private static final String TYPE_ONBOARDING_PROVIDER = "onboardingProvider";

    private static final String IDENTITY_VERIFICATION_ID_PARAM = "identityVerificationId";
    private static final String PROCESS_ID_PARAM = "processId";
    private static final String ACTIVATION_ID_PARAM = "activationId";
    private static final String TARGET_ACTIVATION_ID_PARAM = "targetActivationId";
    private static final String USER_ID_PARAM = "userId";
    private static final String OTP_ID_PARAM = "otpId";
    private static final String DOCUMENT_ID_PARAM = "documentId";
    private static final String DOCUMENT_VERIFICATION_ID_PARAM = "documentVerificationId";
    private static final String DOCUMENT_RESPONSE_JSON_PARAM = "documentResponseJson";

    private static final String PROCESS_ID = "test-process-id";
    private static final String USER_ID = "test-user-id";
    private static final String ACTIVATION_ID = "test-activation-id";
    private static final String TARGET_ACTIVATION_ID = "test-target-activation-id";
    private static final String IDENTITY_VERIFICATION_ID = "test-identity-verification-id";
    private static final String OTP_ID = "test-otp-id";
    private static final String DOCUMENT_ID = "test-document-id";
    private static final String DOCUMENT_VERIFICATION_ID = "test-document-verification-id";

    private static final String MESSAGE = "Test message";

    @Mock
    private Audit audit;

    @InjectMocks
    private AuditService tested;

    @Captor
    private ArgumentCaptor<AuditDetail> auditDetailCaptor;

    @Test
    void testAuditProcess() {
        final OnboardingProcessEntity process = createProcess();

        tested.audit(process, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_PROCESS, auditDetail.getType());
        assertEquals(Map.of(
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                TARGET_ACTIVATION_ID_PARAM, TARGET_ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditProcessWithoutOptionalFields() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);
        process.setUserId(USER_ID);

        tested.audit(process, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_PROCESS, auditDetail.getType());
        assertEquals(Map.of(
                PROCESS_ID_PARAM, PROCESS_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditProcessWithIdentityVerification() {
        final OnboardingProcessEntity process = createProcess();
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.audit(process, identityVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_PROCESS, auditDetail.getType());
        assertEquals(Map.of(
                PROCESS_ID_PARAM, PROCESS_ID,
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                TARGET_ACTIVATION_ID_PARAM, TARGET_ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditOtpWithIdentityVerification() {
        final OnboardingProcessEntity process = createProcess();
        final OnboardingOtpEntity otp = createOtp(process);
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.audit(otp, identityVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_OTP, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID,
                OTP_ID_PARAM, OTP_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditOtp() {
        final OnboardingProcessEntity process = createProcess();
        final OnboardingOtpEntity otp = createOtp(process);

        tested.audit(otp, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_OTP, auditDetail.getType());
        assertEquals(Map.of(
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                USER_ID_PARAM, USER_ID,
                OTP_ID_PARAM, OTP_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditDebugOtp() {
        final OnboardingProcessEntity process = createProcess();
        final OnboardingOtpEntity otp = createOtp(process);

        tested.auditDebug(otp, MESSAGE);

        verify(audit).debug(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_OTP, auditDetail.getType());
        assertEquals(Map.of(
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                USER_ID_PARAM, USER_ID,
                OTP_ID_PARAM, OTP_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditDocumentVerification() {
        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final DocumentVerificationEntity documentVerification = createDocumentVerification(identityVerification);

        tested.audit(documentVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_DOCUMENT_VERIFICATION, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID,
                DOCUMENT_ID_PARAM, DOCUMENT_ID,
                DOCUMENT_VERIFICATION_ID_PARAM, DOCUMENT_VERIFICATION_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditDebugDocumentVerification() {
        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final DocumentVerificationEntity documentVerification = createDocumentVerification(identityVerification);

        tested.auditDebug(documentVerification, MESSAGE);

        verify(audit).debug(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_DOCUMENT_VERIFICATION, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID,
                DOCUMENT_ID_PARAM, DOCUMENT_ID,
                DOCUMENT_VERIFICATION_ID_PARAM, DOCUMENT_VERIFICATION_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditIdentityVerification() {
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.audit(identityVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_IDENTITY_VERIFICATION, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditDocumentVerificationProviderWithIdentityVerification() {
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.auditDocumentVerificationProvider(identityVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_DOCUMENT_VERIFICATION_PROVIDER, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditPresenceCheckProvider() {
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.auditPresenceCheckProvider(identityVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_PRESENCE_CHECK_PROVIDER, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditActivation() {
        final OnboardingProcessEntity process = createProcess();

        tested.auditActivation(process, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_ACTIVATION, auditDetail.getType());
        assertEquals(Map.of(
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                TARGET_ACTIVATION_ID_PARAM, TARGET_ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditOnboardingProviderWithProcess() {
        final OnboardingProcessEntity process = createProcess();

        tested.auditOnboardingProvider(process, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_ONBOARDING_PROVIDER, auditDetail.getType());
        assertEquals(Map.of(
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                TARGET_ACTIVATION_ID_PARAM, TARGET_ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditOnboardingProviderDebug() {
        final OnboardingProcessEntity process = createProcess();

        tested.auditOnboardingProviderDebug(process, MESSAGE);

        verify(audit).debug(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_ONBOARDING_PROVIDER, auditDetail.getType());
        assertEquals(Map.of(
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                TARGET_ACTIVATION_ID_PARAM, TARGET_ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditOnboardingProviderWithIdentityVerification() {
        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.auditOnboardingProvider(identityVerification, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_ONBOARDING_PROVIDER, auditDetail.getType());
        assertEquals(Map.of(
                IDENTITY_VERIFICATION_ID_PARAM, IDENTITY_VERIFICATION_ID,
                PROCESS_ID_PARAM, PROCESS_ID,
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID
        ), auditDetail.getParam());
    }

    @Test
    void testAuditDocumentVerificationProviderWithOwnerId() {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(ACTIVATION_ID);
        ownerId.setUserId(USER_ID);

        final var documentResponseJson = JsonNodeFactory.instance.objectNode();

        tested.auditDocumentVerificationProvider(ownerId, documentResponseJson, MESSAGE);

        verify(audit).info(eq(MESSAGE), auditDetailCaptor.capture(), any(Object[].class));
        final AuditDetail auditDetail = auditDetailCaptor.getValue();
        assertEquals(TYPE_DOCUMENT_VERIFICATION_PROVIDER, auditDetail.getType());
        assertEquals(Map.of(
                ACTIVATION_ID_PARAM, ACTIVATION_ID,
                USER_ID_PARAM, USER_ID,
                DOCUMENT_RESPONSE_JSON_PARAM, documentResponseJson
        ), auditDetail.getParam());
    }

    private static OnboardingProcessEntity createProcess() {
        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId(PROCESS_ID);
        process.setUserId(USER_ID);
        process.setActivationId(ACTIVATION_ID);
        process.setTargetActivationId(TARGET_ACTIVATION_ID);
        return process;
    }

    private static IdentityVerificationEntity createIdentityVerification() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId(IDENTITY_VERIFICATION_ID);
        identityVerification.setProcessId(PROCESS_ID);
        identityVerification.setActivationId(ACTIVATION_ID);
        identityVerification.setUserId(USER_ID);
        return identityVerification;
    }

    private static OnboardingOtpEntity createOtp(final OnboardingProcessEntity process) {
        final OnboardingOtpEntity otp = new OnboardingOtpEntity();
        otp.setId(OTP_ID);
        otp.setProcess(process);
        return otp;
    }

    private static DocumentVerificationEntity createDocumentVerification(final IdentityVerificationEntity identityVerification) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setId(DOCUMENT_ID);
        documentVerification.setVerificationId(DOCUMENT_VERIFICATION_ID);
        documentVerification.setIdentityVerification(identityVerification);
        return documentVerification;
    }
}
