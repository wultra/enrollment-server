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
package com.wultra.app.onboardingserver.impl.service.verification;

import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.database.DocumentResultRepository;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.DocumentsVerificationResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.provider.DocumentVerificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service implementing verification processing features.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class VerificationProcessingBatchService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationProcessingBatchService.class);

    private final DocumentResultRepository documentResultRepository;

    private final IdentityVerificationRepository identityVerificationRepository;

    private final DocumentVerificationProvider documentVerificationProvider;

    private final IdentityVerificationService identityVerificationService;

    private final VerificationProcessingService verificationProcessingService;

    private final AuditService auditService;

    /**
     * Service constructor.
     * @param documentResultRepository Document verification result repository.
     * @param identityVerificationRepository Identity verification repository.
     * @param documentVerificationProvider Document verification provider.
     * @param identityVerificationService Identity verification service.
     * @param verificationProcessingService Verification processing service.
     * @param auditService Audit service.
     */
    @Autowired
    public VerificationProcessingBatchService(
            final DocumentResultRepository documentResultRepository,
            final DocumentVerificationProvider documentVerificationProvider,
            final IdentityVerificationRepository identityVerificationRepository,
            final IdentityVerificationService identityVerificationService,
            final VerificationProcessingService verificationProcessingService,
            final AuditService auditService) {

        this.documentResultRepository = documentResultRepository;
        this.identityVerificationRepository = identityVerificationRepository;
        this.documentVerificationProvider = documentVerificationProvider;
        this.identityVerificationService = identityVerificationService;
        this.verificationProcessingService = verificationProcessingService;
        this.auditService = auditService;
    }

    /**
     * Checks document submit verifications
     */
    @Transactional
    public void checkDocumentSubmitVerifications() {
        AtomicInteger countFinished = new AtomicInteger(0);
        try (Stream<DocumentResultEntity> stream = documentResultRepository.streamAllInProgressDocumentSubmitVerifications()) {
            stream.forEach(docResult -> {
                DocumentVerificationEntity docVerification = docResult.getDocumentVerification();

                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(docVerification.getActivationId());
                ownerId.setUserId("server-task-doc-submit-verifications");

                DocumentsVerificationResult docVerificationResult;
                try {
                    docVerificationResult = documentVerificationProvider.getVerificationResult(ownerId, docVerification.getVerificationId());
                    final IdentityVerificationEntity identityVerification = docVerification.getIdentityVerification();
                    auditService.auditDocumentVerificationProvider(identityVerification, "Result verified: {} for user: {}", docVerificationResult.getStatus(), ownerId.getUserId());
                } catch (DocumentVerificationException e) {
                    logger.error("Checking document submit verification failed, {}", ownerId, e);
                    return;
                }
                verificationProcessingService.processVerificationResult(ownerId, List.of(docVerification), docVerificationResult);

                if (!DocumentStatus.UPLOAD_IN_PROGRESS.equals(docVerification.getStatus())) {
                    logger.debug("Finished verification of {} during submit at the provider, {}", docVerification, ownerId);
                    countFinished.incrementAndGet();
                }
            });
        }
        if (countFinished.get() > 0) {
            logger.debug("Finished {} documents verifications during submit", countFinished.get());
        }
    }

    /**
     * Checks pending documents verifications
     */
    @Transactional
    public void checkDocumentsVerifications() {
        AtomicInteger countFinished = new AtomicInteger(0);
        try (Stream<IdentityVerificationEntity> stream = identityVerificationRepository.streamAllInProgressDocumentsVerifications()) {
            stream.forEach(idVerification -> {
                final OwnerId ownerId = new OwnerId();
                ownerId.setActivationId(idVerification.getActivationId());
                ownerId.setUserId("server-task-docs-verifications");

                try {
                    identityVerificationService.checkVerificationResult(IdentityVerificationPhase.DOCUMENT_VERIFICATION, ownerId, idVerification);
                    if (!IdentityVerificationStatus.IN_PROGRESS.equals(idVerification.getStatus())) {
                        countFinished.incrementAndGet();
                    }
                } catch (DocumentVerificationException | OnboardingProcessException e) {
                    logger.error("Checking identity verification result failed, {}", ownerId, e);
                }
            });
        }
        if (countFinished.get() > 0) {
            logger.debug("Finished {} documents verifications", countFinished.get());
        }
    }

}
