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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.onboardingserver.common.database.*;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessPersonalDataIdsProjection;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.impl.util.DateUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service with cleaning functionality.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
class CleaningService {

    /**
     * Maximum number of values in SQL IN operator list.
     */
    private static final int BATCH_SIZE = 1_000;

    private static final String ERROR_MESSAGE_DOCUMENT_VERIFICATION_EXPIRED = "expired";

    private final OnboardingConfig onboardingConfig;

    private final IdentityVerificationConfig identityVerificationConfig;

    private final OnboardingProcessRepository onboardingProcessRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final DocumentVerificationRepository documentVerificationRepository;

    private final DocumentDataRepository documentDataRepository;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    private final OnboardingOtpRepository onboardingOtpRepository;

    private final SelfieRepository selfieRepository;

    private final AuditService auditService;

    private final DocumentResultRepository documentResultRepository;

    /**
     * Terminate processes with expired activation.
     */
    @Transactional
    public void terminateExpiredProcessActivations() {
        final Duration activationExpiration = onboardingConfig.getActivationExpirationTime();
        final Date createdDateExpiredActivations = DateUtil.convertExpirationToCreatedDate(activationExpiration);
        final List<String> ids = onboardingProcessRepository.findByTimestampAndStatusWithLock(createdDateExpiredActivations, OnboardingStatus.ACTIVATION_IN_PROGRESS);
        terminateProcessesAndRelatedEntities(ids, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ACTIVATION);
    }

    /**
     * Terminate expired processes with expired verification.
     */
    @Transactional
    public void terminateExpiredProcessVerifications() {
        final Duration verificationExpiration = identityVerificationConfig.getVerificationExpirationTime();
        final Date createdDateExpiredVerifications = DateUtil.convertExpirationToCreatedDate(verificationExpiration);
        final List<String> ids = onboardingProcessRepository.findByTimestampAndStatusWithLock(createdDateExpiredVerifications, OnboardingStatus.VERIFICATION_IN_PROGRESS);
        terminateProcessesAndRelatedEntities(ids, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_IDENTITY_VERIFICATION);
    }

    /**
     * Terminate expired OTP codes.
     */
    @Transactional
    public void terminateExpiredOtpCodes() {
        final Duration otpExpiration = onboardingConfig.getOtpExpirationTime();
        final Date createdDateExpiredOtp = DateUtil.convertExpirationToCreatedDate(otpExpiration);
        final List<String> otpIds = onboardingOtpRepository.findExpiredIds(createdDateExpiredOtp);
        final Date now = new Date();
        for (List<String> otpIdChunk : ListUtils.partition(otpIds, BATCH_SIZE)) {
            terminateAndAuditOtps(otpIdChunk, now);
        }
    }

    /**
     * Terminate expired processes.
     */
    @Transactional
    public void terminateExpiredProcesses() {
        final Date now = new Date();
        final Duration processExpiration = onboardingConfig.getProcessExpirationTime();
        final Date createdDateExpiredProcesses = DateUtil.convertExpirationToCreatedDate(processExpiration);
        final List<String> ids = onboardingProcessRepository.findActiveByTimestampWithLock(createdDateExpiredProcesses);
        if (ids.isEmpty()) {
            logger.debug("No expired process to terminate");
            return;
        }
        logger.info("Terminating {} expired processes", ids.size());
        for (List<String> idsChunk : ListUtils.partition(ids, BATCH_SIZE)) {
            terminateAndAuditProcesses(idsChunk, now, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ONBOARDING, ErrorOrigin.PROCESS_LIMIT_CHECK);
        }
    }

    /**
     * Terminate expired document verifications.
     */
    @Transactional
    public void terminateExpiredDocumentVerifications() {
        final var ids = documentVerificationRepository.findExpiredVerifications(getVerificationExpirationTime(), DocumentStatus.ALL_NOT_FINISHED);
        if (ids.isEmpty()) {
            logger.debug("No expired document verification to terminate");
            return;
        }

        final var now = new Date();
        for (List<String> idsChunk : ListUtils.partition(ids, BATCH_SIZE)) {
            logger.info("Terminating {} expired document verifications", idsChunk.size());
            terminateAndAuditDocuments(idsChunk, now, ERROR_MESSAGE_DOCUMENT_VERIFICATION_EXPIRED, ErrorOrigin.PROCESS_LIMIT_CHECK);
        }
    }

    /**
     * Terminate expired identity verifications.
     */
    @Transactional
    public void terminateExpiredIdentityVerifications() {
        final List<String> ids = identityVerificationRepository.findNotCompletedIdentityVerifications(getVerificationExpirationTime());
        if (ids.isEmpty()) {
            logger.debug("No expired identity verification to terminate");
            return;
        }
        final Date now = new Date();
        final ErrorOrigin errorOrigin = ErrorOrigin.PROCESS_LIMIT_CHECK;

        for (List<String> idsChunk : ListUtils.partition(ids, BATCH_SIZE)) {
            logger.info("Terminating {} expired identity verifications", idsChunk.size());
            terminateAndAuditIdentityVerifications(idsChunk, now, OnboardingProcessEntity.ERROR_PROCESS_EXPIRED_ONBOARDING, errorOrigin);
        }
    }

    /**
     * Cleanup personal data of completed processes after the retention time.
     */
    @Transactional
    public void cleanupCompletedProcessPersonalData() {
        final var retentionTime = DateUtil.convertExpirationToCreatedDate(identityVerificationConfig.getDataRetention());
        final var limit = onboardingConfig.getCleanupLimit();

        final var dataIds = onboardingProcessRepository.findIdentityDataForCleanup(OnboardingStatus.COMPLETED, retentionTime, PageRequest.of(0, limit));
        logger.info("Found {} data records of completed processes for cleanup. Retention time: {}", dataIds.size(), retentionTime);

        if (dataIds.isEmpty()) {
            return;
        }

        setProcessCleanupTime(dataIds);
        cleanDocumentData(dataIds);
        cleanSelfieData(dataIds);

        logger.info("Personal data records of completed processes deleted");
    }

    private void setProcessCleanupTime(final List<OnboardingProcessPersonalDataIdsProjection> personalDataIds) {
        final var cleanupTime = new Date();

        final var processIds = personalDataIds.stream()
                .map(OnboardingProcessPersonalDataIdsProjection::getProcessId)
                .distinct()
                .toList();

        logger.debug("Setting cleanup time {} for {} processes", cleanupTime, processIds.size());

        for (final var processIdsBatch : ListUtils.partition(processIds, BATCH_SIZE)) {
            onboardingProcessRepository.updateIdentityDataCleanup(processIdsBatch, cleanupTime);
        }

        logger.debug("Cleanup time set for processes");
    }

    private void cleanDocumentData(final List<OnboardingProcessPersonalDataIdsProjection> personalDataIds) {
        final var ids = personalDataIds.stream()
                .map(OnboardingProcessPersonalDataIdsProjection::getDocumentVerificationId)
                .filter(Objects::nonNull)
                .toList();

        logger.debug("Deleting document data for {} document verifications", ids.size());

        for (final var idsBatch : ListUtils.partition(ids, BATCH_SIZE)) {
            processedDocumentDataRepository.deleteAllByDocumentVerificationIds(idsBatch);
            documentDataRepository.deleteAllByDocumentVerificationIds(idsBatch);
        }

        logger.debug("Document data deleted");
    }

    private void cleanSelfieData(final List<OnboardingProcessPersonalDataIdsProjection> personalDataIds) {
        final var ids = personalDataIds.stream()
                .map(OnboardingProcessPersonalDataIdsProjection::getIdentityVerificationId)
                .distinct()
                .toList();

        logger.debug("Deleting selfie data for {} identity verifications", ids.size());

        for (final var idsBatch : ListUtils.partition(ids, BATCH_SIZE)) {
            selfieRepository.deleteAllByIdentityVerificationIds(idsBatch);
        }

        logger.debug("Selfie data deleted");
    }

    private Date getVerificationExpirationTime() {
        return DateUtil.convertExpirationToCreatedDate(identityVerificationConfig.getVerificationExpirationTime());
    }

    private void terminateProcessesAndRelatedEntities(final List<String> processIds, final String errorDetail) {
        if (processIds.isEmpty()) {
            logger.debug("No process to terminate");
            return;
        }

        final Date now = new Date();
        final ErrorOrigin errorOrigin = ErrorOrigin.PROCESS_LIMIT_CHECK;

        for (List<String> processIdChunk : ListUtils.partition(processIds, BATCH_SIZE)) {
            logger.info("Terminating {} processes", processIdChunk.size());
            terminateAndAuditProcesses(processIdChunk, now, errorDetail, errorOrigin);

            final List<String> identityVerificationIds = identityVerificationRepository.findNotCompletedIdentityVerifications(processIdChunk);
            logger.info("Terminating {} identity verifications", identityVerificationIds.size());
            terminateAndAuditIdentityVerifications(identityVerificationIds, now, errorDetail, errorOrigin);

            final List<String> documentVerificationIds = documentVerificationRepository.findDocumentVerifications(identityVerificationIds, DocumentStatus.ALL_NOT_FINISHED);
            logger.info("Terminating {} document verifications", documentVerificationIds.size());
            terminateAndAuditDocuments(documentVerificationIds, now, errorDetail, errorOrigin);
        }
    }

    private void terminateAndAuditProcesses(final List<String> processIds, final Date now, final String errorDetail, final ErrorOrigin errorOrigin) {
        onboardingProcessRepository.terminate(processIds, now, errorDetail, errorOrigin);
        processIds.forEach(processId ->
            onboardingProcessRepository.findById(processId).ifPresent(process ->
                    auditService.audit(process, "Expired process for user: {}, {}", process.getUserId(), errorDetail)));
    }

    private void terminateAndAuditIdentityVerifications(final List<String> identityVerificationIds, final Date now, final String errorDetail, final ErrorOrigin errorOrigin) {
        identityVerificationRepository.terminate(identityVerificationIds, now, errorDetail, errorOrigin);
        identityVerificationIds.forEach(identityVerificationId ->
                identityVerificationRepository.findById(identityVerificationId).ifPresent(identityVerification ->
                        auditService.audit(identityVerification, "Expired identity verification for user: {}, {}", identityVerification.getUserId(), errorDetail)));
    }

    private void terminateAndAuditOtps(final List<String> otpIds, final Date now) {
        onboardingOtpRepository.terminate(otpIds, now);
        otpIds.forEach(otpId ->
                onboardingOtpRepository.findById(otpId).ifPresent(otp ->
                        auditService.audit(otp, "Expired OTP for user: {}", otp.getProcess().getUserId())));
    }

    private void terminateAndAuditDocuments(final List<String> documentVerificationIds, final Date now, final String errorDetail, final ErrorOrigin errorOrigin) {
        documentVerificationRepository.terminate(documentVerificationIds, now, errorDetail, errorOrigin);
        documentResultRepository.clean(documentVerificationIds);

        final var documentVerifications = documentVerificationRepository.findAllById(documentVerificationIds);
        for (final var documentVerification : documentVerifications) {
            if (documentVerification != null) {
                auditService.audit(documentVerification, "Expired Document verification for user: {}, {}", documentVerification.getIdentityVerification().getUserId(), errorDetail);
            }
        }
    }

    protected static final class ListUtils {

        private ListUtils() {
            throw new IllegalStateException("Utility class");
        }

        public static Collection<List<String>> partition(final List<String> source, final int partitionSize) {
            if (source.size() <= partitionSize) {
                return List.of(source);
            }
            return IntStream.range(0, source.size())
                    .boxed()
                    .collect(Collectors.groupingBy(partition -> (partition / partitionSize), Collectors.mapping(source::get, Collectors.toList())))
                    .values();
        }
    }
}
