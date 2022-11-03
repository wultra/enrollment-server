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
package com.wultra.app.onboardingserver.statemachine.action.verification;

import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.enumeration.OnboardingProcessError;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.common.service.OnboardingProcessLimitService;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.*;
import static java.util.stream.Collectors.toList;

/**
 * Call final document verification at {@code DOCUMENT_VERIFICATION_FINAL} phase and move to {@code CLIENT_EVALUATION / IN_PROGRESS}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@Slf4j
public class DocumentVerificationFinalAction implements Action<OnboardingState, OnboardingEvent> {

    private final DocumentVerificationProvider documentVerificationProvider;

    private final IdentityVerificationService identityVerificationService;

    private final CommonOnboardingService processService;

    private final OnboardingProcessLimitService processLimitService;

    private final AuditService auditService;

    @Autowired
    public DocumentVerificationFinalAction(
            final DocumentVerificationProvider documentVerificationProvider,
            final IdentityVerificationService identityVerificationService,
            final CommonOnboardingService processService,
            final OnboardingProcessLimitService processLimitService,
            final AuditService auditService) {

        this.documentVerificationProvider = documentVerificationProvider;
        this.identityVerificationService = identityVerificationService;
        this.processService = processService;
        this.processLimitService = processLimitService;
        this.auditService = auditService;
    }

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        try {
            // TODO Lubos extract to service
            executeInternal(identityVerification, ownerId);
        } catch (RemoteCommunicationException | DocumentVerificationException | OnboardingProcessException e) {
            context.getStateMachine().setStateMachineError(e);
        }
    }

    private void executeInternal(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) throws RemoteCommunicationException, DocumentVerificationException, OnboardingProcessException {
        final List<DocumentVerificationEntity> documentVerifications = filterDocumentVerifications(identityVerification);

        final List<String> uploadIds = documentVerifications.stream()
                .map(DocumentVerificationEntity::getUploadId)
                .collect(toList());

        final DocumentsVerificationResult result = documentVerificationProvider.verifyDocuments(ownerId, uploadIds);
        final String verificationId = result.getVerificationId();
        final DocumentVerificationStatus status = result.getStatus();
        logger.info("Cross verified documents upload ID: {}, verification ID: {}, status: {}, {}", uploadIds, verificationId, status, ownerId);
        auditService.auditDocumentVerificationProvider(identityVerification, "Cross verified documents: {} for user: {}", status, ownerId.getUserId());

        documentVerifications.forEach(docVerification -> {
            docVerification.setVerificationId(result.getVerificationId());
            docVerification.setTimestampLastUpdated(ownerId.getTimestamp());
        });

        switch (status) {
            case ACCEPTED:
                accept(identityVerification, documentVerifications, ownerId);
                break;
            case FAILED:
                fail(identityVerification, result, documentVerifications, ownerId);
                break;
            case REJECTED:
                reject(identityVerification, result, documentVerifications, ownerId);
                break;
            case IN_PROGRESS:
                throw new DocumentVerificationException("Only sync mode is supported, " + ownerId);
            default:
                throw new DocumentVerificationException(String.format("Not supported status %s, %s", status, ownerId));
        }
    }

    private void accept(
            final IdentityVerificationEntity identityVerification,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) {
        documentVerifications.forEach(docVerification ->
            auditService.audit(docVerification, "Document accepted at final verification for user: {}", identityVerification.getUserId()));
        identityVerificationService.moveToPhaseAndStatus(identityVerification, CLIENT_EVALUATION, IN_PROGRESS, ownerId);
    }

    private void reject(
            final IdentityVerificationEntity identityVerification,
            final DocumentsVerificationResult result,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) throws OnboardingProcessException {

        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.REJECTED);
            docVerification.setRejectReason(result.getRejectReason());
            docVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
            auditService.audit(docVerification, "Document rejected at final verification for user: {}", identityVerification.getUserId());
        });
        identityVerification.setRejectReason(result.getRejectReason());
        identityVerification.setRejectOrigin(RejectOrigin.DOCUMENT_VERIFICATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
        identityVerificationService.moveToPhaseAndStatus(identityVerification, identityVerification.getPhase(), REJECTED, ownerId);
        final OnboardingProcessEntity process = processService.findProcess(identityVerification.getProcessId());
        processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_REJECTED, ownerId);
        processLimitService.checkOnboardingProcessErrorLimits(process);
    }

    private void fail(
            final IdentityVerificationEntity identityVerification,
            final DocumentsVerificationResult result,
            final List<DocumentVerificationEntity> documentVerifications,
            final OwnerId ownerId) throws OnboardingProcessException {

        documentVerifications.forEach(docVerification -> {
            docVerification.setStatus(DocumentStatus.FAILED);
            docVerification.setErrorDetail(result.getErrorDetail());
            docVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
            auditService.audit(docVerification, "Document failed at final verification for user: {}", identityVerification.getUserId());
        });
        identityVerification.setErrorDetail(result.getErrorDetail());
        identityVerification.setErrorOrigin(ErrorOrigin.DOCUMENT_VERIFICATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
        identityVerificationService.moveToPhaseAndStatus(identityVerification, identityVerification.getPhase(), FAILED, ownerId);
        final OnboardingProcessEntity process = processService.findProcess(identityVerification.getProcessId());
        processLimitService.incrementErrorScore(process, OnboardingProcessError.ERROR_DOCUMENT_VERIFICATION_FAILED, ownerId);
        processLimitService.checkOnboardingProcessErrorLimits(process);
    }

    private static List<DocumentVerificationEntity> filterDocumentVerifications(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(toList());
    }
}
