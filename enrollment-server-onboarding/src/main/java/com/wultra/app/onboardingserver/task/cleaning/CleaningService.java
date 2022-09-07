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
package com.wultra.app.onboardingserver.task.cleaning;

import com.google.common.collect.Lists;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.database.DocumentDataRepository;
import com.wultra.app.onboardingserver.database.DocumentVerificationRepository;
import com.wultra.app.onboardingserver.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.impl.service.OtpServiceImpl;
import com.wultra.app.onboardingserver.impl.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * Service with cleaning functionality.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
class CleaningService {

    /**
     * Maximum number of values in SQL IN operator list.
     */
    private static final int BATCH_SIZE = 1_000;

    private static final String ERROR_MESSAGE_DOCUMENT_VERIFICATION_EXPIRED = "expired";

    private final OnboardingConfig onboardingConfig;

    final IdentityVerificationConfig identityVerificationConfig;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final DocumentDataRepository documentDataRepository;

    private final OtpServiceImpl otpService;

    @Autowired
    public CleaningService(
            final OnboardingConfig onboardingConfig,
            final IdentityVerificationConfig identityVerificationConfig,
            final OnboardingProcessRepository onboardingProcessRepository,
            final IdentityVerificationRepository identityVerificationRepository,
            final DocumentVerificationRepository documentVerificationRepository,
            final DocumentDataRepository documentDataRepository,
            final OtpServiceImpl otpService) {

        this.onboardingConfig = onboardingConfig;
        this.identityVerificationConfig = identityVerificationConfig;
        this.onboardingProcessRepository = onboardingProcessRepository;
        this.identityVerificationRepository = identityVerificationRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentDataRepository = documentDataRepository;
        this.otpService = otpService;
    }

    /**
     * Terminate processes with expired activation.
     */
    @Transactional
    public void terminateExpiredProcessActivations() {
        final Duration activationExpiration = onboardingConfig.getActivationExpirationTime();
        final Date createdDateExpiredActivations = DateUtil.convertExpirationToCreatedDate(activationExpiration);
        final List<String> ids = onboardingProcessRepository.findExpiredProcessIdsByStatusAndCreatedDate(createdDateExpiredActivations, OnboardingStatus.ACTIVATION_IN_PROGRESS);
        terminateProcessesAndRelatedEntities(ids, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ACTIVATION);
    }

    /**
     * Terminate expired processes with expired verification.
     */
    @Transactional
    public void terminateExpiredProcessVerifications() {
        final Duration verificationExpiration = identityVerificationConfig.getVerificationExpirationTime();
        final Date createdDateExpiredVerifications = DateUtil.convertExpirationToCreatedDate(verificationExpiration);
        final List<String> ids = onboardingProcessRepository.findExpiredProcessIdsByStatusAndCreatedDate(createdDateExpiredVerifications, OnboardingStatus.VERIFICATION_IN_PROGRESS);
        terminateProcessesAndRelatedEntities(ids, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_IDENTITY_VERIFICATION);
    }

    /**
     * Terminate expired OTP codes.
     */
    @Transactional
    public void terminateExpiredOtpCodes() {
        final Duration otpExpiration = onboardingConfig.getOtpExpirationTime();
        final Date createdDateExpiredOtp = DateUtil.convertExpirationToCreatedDate(otpExpiration);
        otpService.terminateExpiredOtps(createdDateExpiredOtp);
    }

    /**
     * Terminate expired processes.
     */
    @Transactional
    public void terminateExpiredProcesses() {
        final Date now = new Date();
        final Duration processExpiration = onboardingConfig.getProcessExpirationTime();
        final Date createdDateExpiredProcesses = DateUtil.convertExpirationToCreatedDate(processExpiration);
        final List<String> ids = onboardingProcessRepository.findExpiredProcessIdsByCreatedDate(createdDateExpiredProcesses);
        if (ids.isEmpty()) {
            return;
        }
        logger.info("Terminating {} expired processes", ids.size());
        for (List<String> idsChunk : Lists.partition(ids, BATCH_SIZE)) {
            onboardingProcessRepository.terminate(idsChunk, now, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ONBOARDING, ErrorOrigin.PROCESS_LIMIT_CHECK);
        }
    }

    /**
     * Cleanup of large documents older than retention time.
     */
    @Transactional
    public void cleanupLargeDocuments() {
        documentDataRepository.cleanupDocumentData(getDataRetentionTime());
    }

    /**
     * Terminate expired document verifications.
     */
    @Transactional
    public void terminateExpiredDocumentVerifications() {
        final List<String> ids = documentVerificationRepository
                .findExpiredVerifications(getVerificationExpirationTime(), DocumentStatus.ALL_NOT_FINISHED);
        if (ids.isEmpty()) {
            return;
        }

        final Date now = new Date();
        for (List<String> idsChunk : Lists.partition(ids, BATCH_SIZE)) {
            documentVerificationRepository.terminate(idsChunk, now, ERROR_MESSAGE_DOCUMENT_VERIFICATION_EXPIRED, ErrorOrigin.PROCESS_LIMIT_CHECK);
            logger.info("Terminating {} expired document verifications", idsChunk.size());
        }
    }

    /**
     * Terminate expired identity verifications.
     */
    @Transactional
    public void terminateExpiredIdentityVerifications() {
        final List<String> ids = identityVerificationRepository.findNotCompletedIdentityVerifications(getVerificationExpirationTime());
        if (ids.isEmpty()) {
            return;
        }
        final Date now = new Date();
        final ErrorOrigin errorOrigin = ErrorOrigin.PROCESS_LIMIT_CHECK;

        for (List<String> idsChunk : Lists.partition(ids, BATCH_SIZE)) {
            logger.info("Terminating {} expired identity verifications", idsChunk.size());
            identityVerificationRepository.terminate(idsChunk, now, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ONBOARDING, errorOrigin);
        }
    }

    private Date getDataRetentionTime() {
        return DateUtil.convertExpirationToCreatedDate(identityVerificationConfig.getDataRetentionTime());
    }

    private Date getVerificationExpirationTime() {
        return DateUtil.convertExpirationToCreatedDate(identityVerificationConfig.getVerificationExpirationTime());
    }

    private void terminateProcessesAndRelatedEntities(final List<String> processIds, final String errorDetail) {
        if (processIds.isEmpty()) {
            return;
        }

        final Date now = new Date();
        final ErrorOrigin errorOrigin = ErrorOrigin.PROCESS_LIMIT_CHECK;

        for (List<String> processIdChunk : Lists.partition(processIds, BATCH_SIZE)) {
            logger.info("Terminating {} processes", processIdChunk.size());
            onboardingProcessRepository.terminate(processIdChunk, now, errorDetail, errorOrigin);

            final List<String> identityVerificationIds = identityVerificationRepository.findNotCompletedIdentityVerificationsByProcessIds(processIdChunk);
            logger.info("Terminating {} identity verifications", identityVerificationIds.size());
            identityVerificationRepository.terminate(identityVerificationIds, now, errorDetail, errorOrigin);

            final List<String> documentVerificationIds = documentVerificationRepository.findDocumentVerificationsByIdentityVerificationIdsAndStatuses(identityVerificationIds, DocumentStatus.ALL_NOT_FINISHED);
            logger.info("Terminating {} document verifications", documentVerificationIds.size());
            documentVerificationRepository.terminate(documentVerificationIds, now, errorDetail, errorOrigin);
        }
    }
}