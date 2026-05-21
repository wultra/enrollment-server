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
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.*;
import com.wultra.app.onboardingserver.provider.model.response.ProcessEventResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

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
    private final ProcessedDocumentDataRepository processedDocumentDataRepository;
    private final ObjectMapper objectMapper;

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
     * Publish a {@link EventType#FINAL_DOCUMENT_VERIFICATION_FINISHED} event with status
     * {@link EventStatus#ACCEPTED}.
     *
     * @param identityVerification Identity verification entity.
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishFinalDocumentVerificationAccepted(
            final IdentityVerificationEntity identityVerification,
            final OwnerId ownerId) {

        publishFinalDocumentVerification(identityVerification, EventStatus.ACCEPTED, null, null, ownerId);
    }

    /**
     * Publish a {@link EventType#FINAL_DOCUMENT_VERIFICATION_FINISHED} event with status
     * {@link EventStatus#REJECTED}.
     *
     * @param identityVerification Identity verification entity.
     * @param rejectReason Reason why the documents were rejected.
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishFinalDocumentVerificationRejected(
            final IdentityVerificationEntity identityVerification,
            final String rejectReason,
            final OwnerId ownerId) {

        publishFinalDocumentVerification(identityVerification, EventStatus.REJECTED, rejectReason, null, ownerId);
    }

    /**
     * Publish a {@link EventType#FINAL_DOCUMENT_VERIFICATION_FINISHED} event with status
     * {@link EventStatus#FAILED}.
     *
     * @param identityVerification Identity verification entity.
     * @param errorDetail Error detail describing the failure.
     * @param ownerId Owner identification (used only for logging).
     */
    public void publishFinalDocumentVerificationFailed(
            final IdentityVerificationEntity identityVerification,
            final String errorDetail,
            final OwnerId ownerId) {

        publishFinalDocumentVerification(identityVerification, EventStatus.FAILED, null, errorDetail, ownerId);
    }

    private void publishFinalDocumentVerification(
            final IdentityVerificationEntity identityVerification,
            final EventStatus status,
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
                .status(convert(process.getStatus()))
                .errorDetail(process.getErrorDetail())
                .deviceData(ProcessFinishedEventData.DeviceData.builder()
                        .locale(processWrapper.getLocale())
                        .ipAddress(processWrapper.getIpAddress())
                        .httpUserAgent(processWrapper.getUserAgent())
                        .fdsData(processWrapper.getFdsData())
                        .build())
                .build();
    }

    private static EventStatus convert(final OnboardingStatus source) {
        return switch (source) {
            case FINISHED -> EventStatus.FINISHED;
            case FAILED -> EventStatus.FAILED;
            default -> throw new IllegalArgumentException("Unsupported onboarding status: " + source);
        };
    }

    private EventData createDocumentVerificationFinishedEventData(final DocumentVerificationEntity document) {
        final DocumentStatus documentStatus = document.getStatus();
        final boolean detailsApplicable = documentStatus == DocumentStatus.ACCEPTED || documentStatus == DocumentStatus.REJECTED;

        final DocumentResultEntity latestResult = document.getResults().stream()
                .findFirst()
                .orElse(null);

        final DocumentVerificationFinishedEventData.DocumentVerificationResult result = detailsApplicable
                ? DocumentVerificationFinishedEventData.DocumentVerificationResult.builder()
                        .type(document.getType() == null ? null : document.getType().name())
                        .country(document.getCountry())
                        .data(buildDocumentData(latestResult))
                        .images(buildImages(document))
                        .rawData(latestResult == null ? null : latestResult.getVerificationResult())
                        .build()
                : null;

        return DocumentVerificationFinishedEventData.builder()
                .documentVerificationId(document.getId())
                .documentId(document.getUploadId() != null ? document.getUploadId() : document.getId())
                .status(convert(documentStatus))
                .rejectReason(document.getRejectReason())
                .errorDetail(document.getErrorDetail())
                .provider(identityVerificationConfig.getDocumentVerificationProvider())
                .score(document.getVerificationScore() != null ? document.getVerificationScore() : 0)
                .documentVerificationResult(result)
                .build();
    }

    private static EventStatus convert(final DocumentStatus source) {
        return switch (source) {
            case ACCEPTED -> EventStatus.ACCEPTED;
            case REJECTED -> EventStatus.REJECTED;
            case FAILED -> EventStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown document status: " + source);
        };
    }

    private DocumentVerificationFinishedEventData.DocumentData buildDocumentData(final DocumentResultEntity result) {
        if (result == null || result.getExtractedData() == null) {
            return null;
        }
        final DocumentExtractedDataValue value;
        try {
            value = objectMapper.readValue(result.getExtractedData(), DocumentExtractedDataValue.class);
        } catch (JacksonException e) {
            logger.warn("Unable to parse extracted data for documentResultId={}: {}", result.getId(), e.getMessage());
            return null;
        }
        return DocumentVerificationFinishedEventData.DocumentData.builder()
                .surname(value.surname())
                .givenNames(value.givenNames())
                .dateOfBirth(formatDate(value.dateOfBirth()))
                .placeOfBirth(value.placeOfBirth())
                .sex(value.sex())
                .nationality(value.nationality())
                .personalNumber(value.personalNumber())
                .documentNumber(value.documentNumber())
                .dateOfIssue(formatDate(value.dateOfIssue()))
                .dateOfExpiry(formatDate(value.dateOfExpiry()))
                .authority(value.authority())
                .build();
    }

    private List<DocumentVerificationFinishedEventData.Image> buildImages(final DocumentVerificationEntity doc) {
        final List<ProcessedDocumentDataEntity> entities =
                processedDocumentDataRepository.findAllByDocumentVerificationIds(Set.of(doc.getId()));
        if (entities.isEmpty()) {
            return null;
        }
        return entities.stream()
                .map(e -> DocumentVerificationFinishedEventData.Image.builder()
                        .type(e.getDataType().name())
                        .data(Base64.getEncoder().encodeToString(e.getData()))
                        .build())
                .toList();
    }

    private static String formatDate(final LocalDate date) {
        return date == null ? null : date.toString();
    }

    private EventData createFinalDocumentVerificationFinishedEventData(
            final IdentityVerificationEntity identityVerification,
            final EventStatus status,
            final String rejectReason,
            final String errorDetail) {

        final List<String> documentIds = identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .map(doc -> doc.getUploadId() != null ? doc.getUploadId() : doc.getId())
                .toList();

        return FinalDocumentVerificationFinishedEventData.builder()
                .documentVerificationId(identityVerification.getId())
                .status(status)
                .rejectReason(rejectReason)
                .errorDetail(errorDetail)
                .provider(identityVerificationConfig.getDocumentVerificationProvider())
                .documentIds(documentIds)
                .build();
    }

    private EventData createPresenceCheckFinishedEventData(final PresenceCheckResult result) {
        final Image photo = result.getPhoto();
        final PresenceCheckFinishedEventData.PresenceCheckResult presenceCheckResult = (photo == null || photo.getData() == null)
                ? null
                : PresenceCheckFinishedEventData.PresenceCheckResult.builder()
                        .frame(Base64.getEncoder().encodeToString(photo.getData()))
                        .build();
        return PresenceCheckFinishedEventData.builder()
                .status(convert(result.getStatus()))
                .rejectReason(result.getRejectReason())
                .errorDetail(result.getErrorDetail())
                .provider(identityVerificationConfig.getPresenceCheckProvider())
                .score(10) // so far sending constant 10 as 100 percent confidence, possible future extension point
                .presenceCheckResult(presenceCheckResult)
                .build();
    }

    private static EventStatus convert(final PresenceCheckStatus source) {
        return switch (source) {
            case ACCEPTED -> EventStatus.ACCEPTED;
            case FAILED -> EventStatus.FAILED;
            case REJECTED -> EventStatus.REJECTED;
        };
    }
}
