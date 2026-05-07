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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntityWrapper;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.DocumentVerificationFinishedEventData;
import com.wultra.app.onboardingserver.provider.model.request.EventData;
import com.wultra.app.onboardingserver.provider.model.request.EventType;
import com.wultra.app.onboardingserver.provider.model.request.FinalDocumentVerificationFinishedEventData;
import com.wultra.app.onboardingserver.provider.model.request.PresenceCheckFinishedEventData;
import com.wultra.app.onboardingserver.provider.model.request.ProcessEventRequest;
import com.wultra.app.onboardingserver.provider.model.request.ProcessFinishedEventData;
import com.wultra.app.onboardingserver.provider.model.response.ProcessEventResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that publishes lifecycle events of the identity verification process to the configured
 * {@link OnboardingProvider}.
 * <p>
 * Failures during event publishing never abort the underlying process; they are only logged.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
@AllArgsConstructor
public class OnboardingEventService {

    private final OnboardingProvider onboardingProvider;
    private final IdentityVerificationConfig identityVerificationConfig;
    private final CommonOnboardingService commonOnboardingService;

    /**
     * Publish a {@link EventType#PROCESS_FINISHED} event.
     *
     * @param process Onboarding process that has just finished.
     * @param identityVerification Related identity verification entity.
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishProcessFinished(
            final OnboardingProcessEntity process,
            final IdentityVerificationEntity identityVerification,
            final OwnerId ownerId) {

        final ProcessEventRequest request = baseRequestBuilder(process, identityVerification)
                .type(EventType.PROCESS_FINISHED)
                .eventData(createProcessFinishedEventData(process))
                .build();
        sendEvent(request, ownerId, "process finished");
    }

    /**
     * Publish a {@link EventType#DOCUMENT_VERIFICATION_FINISHED} event for a single document.
     *
     * @param documentVerification Document verification entity whose verification has finished.
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishDocumentVerificationFinished(
            final DocumentVerificationEntity documentVerification,
            final OwnerId ownerId) {

        final IdentityVerificationEntity identityVerification = documentVerification.getIdentityVerification();
        final OnboardingProcessEntity process = findProcessSafely(identityVerification, EventType.DOCUMENT_VERIFICATION_FINISHED);
        if (process == null) {
            return;
        }

        final ProcessEventRequest request = baseRequestBuilder(process, identityVerification)
                .type(EventType.DOCUMENT_VERIFICATION_FINISHED)
                .eventData(createDocumentVerificationFinishedEventData(documentVerification))
                .build();
        sendEvent(request, ownerId, "document verification finished");
    }

    /**
     * Publish a {@link EventType#FINAL_DOCUMENT_VERIFICATION_FINISHED} event with the overall
     * outcome of the document checks (cross-check, type, country).
     *
     * @param identityVerification Identity verification entity.
     * @param status Resulting identity verification status after the final document verification
     *               (typically {@code ACCEPTED}, {@code REJECTED} or {@code FAILED}).
     * @param rejectReason Reject reason when {@code status} is {@code REJECTED}, otherwise {@code null}.
     * @param errorDetail Error detail when {@code status} is {@code FAILED}, otherwise {@code null}.
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishFinalDocumentVerificationFinished(
            final IdentityVerificationEntity identityVerification,
            final IdentityVerificationStatus status,
            final String rejectReason,
            final String errorDetail,
            final OwnerId ownerId) {

        final OnboardingProcessEntity process = findProcessSafely(identityVerification, EventType.FINAL_DOCUMENT_VERIFICATION_FINISHED);
        if (process == null) {
            return;
        }

        final ProcessEventRequest request = baseRequestBuilder(process, identityVerification)
                .type(EventType.FINAL_DOCUMENT_VERIFICATION_FINISHED)
                .eventData(createFinalDocumentVerificationFinishedEventData(identityVerification, status, rejectReason, errorDetail))
                .build();
        sendEvent(request, ownerId, "final document verification finished");
    }

    /**
     * Publish a {@link EventType#PRESENCE_CHECK_FINISHED} event with the result of the presence
     * check verification provider.
     *
     * @param identityVerification Identity verification entity.
     * @param result Presence check result returned by the provider (terminal state expected).
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishPresenceCheckFinished(
            final IdentityVerificationEntity identityVerification,
            final PresenceCheckResult result,
            final OwnerId ownerId) {

        final OnboardingProcessEntity process = findProcessSafely(identityVerification, EventType.PRESENCE_CHECK_FINISHED);
        if (process == null) {
            return;
        }

        final ProcessEventRequest request = baseRequestBuilder(process, identityVerification)
                .type(EventType.PRESENCE_CHECK_FINISHED)
                .eventData(createPresenceCheckFinishedEventData(result))
                .build();
        sendEvent(request, ownerId, "presence check finished");
    }

    private ProcessEventRequest.ProcessEventRequestBuilder baseRequestBuilder(
            final OnboardingProcessEntity process,
            final IdentityVerificationEntity identityVerification) {

        return ProcessEventRequest.builder()
                .processId(process.getId())
                .processType(process.getProcessConfiguration().getProcessType())
                .userId(identityVerification.getUserId())
                .externalUserId(process.getUserId()) // TODO Lubos store and get iProov userId
                .identityVerificationId(identityVerification.getId());
    }

    private OnboardingProcessEntity findProcessSafely(final IdentityVerificationEntity identityVerification, final EventType eventType) {
        try {
            return commonOnboardingService.findProcess(identityVerification.getProcessId());
        } catch (OnboardingProcessException e) {
            logger.warn("Unable to publish {} event - onboarding process not found, identityVerificationId={}, processId={}: {}",
                    eventType, identityVerification.getId(), identityVerification.getProcessId(), e.getMessage());
            return null;
        }
    }

    private void sendEvent(final ProcessEventRequest request, final OwnerId ownerId, final String eventLabel) {
        try {
            logger.info("Publishing {} event, type={}, processId={}, {}", eventLabel, request.getType(), request.getProcessId(), ownerId);
            final ProcessEventResponse response = onboardingProvider.processEvent(request);
            logger.debug("Got {} for processId={}", response, request.getProcessId());
            if (response.isErrorOccurred()) {
                logger.info("{} event failed to be published: {}, {}", eventLabel, response.getErrorDetail(), ownerId);
            } else {
                logger.info("{} event published, {}", eventLabel, ownerId);
            }
        } catch (OnboardingProviderException e) {
            // unsuccessful event publishing does not stop the process
            logger.info("Unable to publish {} event to the onboarding adapter: {}", eventLabel, e.getMessage());
            logger.debug("Unable to publish {} event to the onboarding adapter", eventLabel, e);
        }
    }

    private static EventData createProcessFinishedEventData(final OnboardingProcessEntity process) {
        final OnboardingProcessEntityWrapper processWrapper = new OnboardingProcessEntityWrapper(process);
        return ProcessFinishedEventData.builder()
                .status(process.getStatus().name())
                .errorDetail(process.getErrorDetail())
                .mobileData(ProcessFinishedEventData.MobileData.builder()
                        .locale(processWrapper.getLocale())
                        .clientIPAddress(processWrapper.getIpAddress())
                        .httpUserAgent(processWrapper.getUserAgent())
                        .fdsData(processWrapper.getFdsData())
                        .build())
                .build();
    }

    private EventData createDocumentVerificationFinishedEventData(final DocumentVerificationEntity doc) {
        final DocumentStatus status = doc.getStatus();
        final boolean detailsApplicable = status == DocumentStatus.ACCEPTED || status == DocumentStatus.REJECTED;

        final DocumentVerificationFinishedEventData.DocumentVerificationResult result = detailsApplicable
                ? DocumentVerificationFinishedEventData.DocumentVerificationResult.builder()
                        .type(doc.getType() == null ? null : doc.getType().name())
                        .country(doc.getCountry())
                        .rawData(doc.getResults().stream()
                                .findFirst()
                                .map(DocumentResultEntity::getVerificationResult)
                                .orElse(null))
                        .build()
                : null;

        return DocumentVerificationFinishedEventData.builder()
                .documentVerificationId(doc.getId())
                .documentId(doc.getUploadId() != null ? doc.getUploadId() : doc.getId())
                .status(status.name())
                .rejectReason(doc.getRejectReason())
                .errorDetail(doc.getErrorDetail())
                .provider(identityVerificationConfig.getDocumentVerificationProvider())
                .score(doc.getVerificationScore() != null ? doc.getVerificationScore() : 0)
                .documentVerificationResult(result)
                .build();
    }

    private EventData createFinalDocumentVerificationFinishedEventData(
            final IdentityVerificationEntity identityVerification,
            final IdentityVerificationStatus status,
            final String rejectReason,
            final String errorDetail) {

        final List<String> documentIds = identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .map(doc -> doc.getUploadId() != null ? doc.getUploadId() : doc.getId())
                .toList();

        return FinalDocumentVerificationFinishedEventData.builder()
                .documentVerificationId(identityVerification.getId())
                .status(status.name())
                .rejectReason(rejectReason)
                .errorDetail(errorDetail)
                .provider(identityVerificationConfig.getDocumentVerificationProvider())
                .documentIds(documentIds)
                .build();
    }

    private EventData createPresenceCheckFinishedEventData(final PresenceCheckResult result) {
        return PresenceCheckFinishedEventData.builder()
                .status(result.getStatus().name())
                .rejectReason(result.getRejectReason())
                .errorDetail(result.getErrorDetail())
                .provider(identityVerificationConfig.getPresenceCheckProvider())
                .score(0) // TODO Lubos fill score
                .build();
    }
}
