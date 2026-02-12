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
package com.wultra.app.onboardingserver.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toSet;

/**
 * Service for client evaluation features.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Slf4j
public class ClientEvaluationService {

    private static final String ERROR_VERIFICATION_ID = "unableToGetDocumentVerificationId";

    private final OnboardingProvider onboardingProvider;

    private final IdentityVerificationConfig config;

    private final AuditService auditService;

    private final CommonOnboardingService onboardingService;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    private final RetryTemplate retryTemplate;

    private final ObjectMapper objectMapper;

    public ClientEvaluationService(OnboardingProvider onboardingProvider,
                                   IdentityVerificationConfig config,
                                   AuditService auditService,
                                   CommonOnboardingService onboardingService,
                                   ProcessedDocumentDataRepository processedDocumentDataRepository) {
        this.onboardingProvider = onboardingProvider;
        this.config = config;
        this.auditService = auditService;
        this.onboardingService = onboardingService;
        this.processedDocumentDataRepository = processedDocumentDataRepository;

        this.retryTemplate = RetryTemplate.builder()
                .maxAttempts(config.getClientEvaluationMaxFailedAttempts())
                .exponentialBackoff(200, 2.0, 2_000)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks whether the client evaluation phase is enabled in the onboarding process.
     *
     * @param processId the onboarding process ID
     * @return whether the client evaluation phase is enabled
     * @throws OnboardingProcessException if the onboarding process configuration is not found
     */
    @Transactional(readOnly = true)
    public boolean isClientEvaluationEnabled(final String processId) throws OnboardingProcessException {
        return onboardingService.findProcess(processId)
                .getProcessConfiguration()
                .getConfiguration()
                .clientEvaluationEnabled();
    }

    /**
     * Process client evaluation of the given identity verification.
     *
     * @param identityVerification identity verification to process
     * @param ownerId Owner identification.
     */
    public @Nullable EvaluateClientResponse.EvaluationResult processClientEvaluation(
            final IdentityVerificationEntity identityVerification,
            final OwnerId ownerId
    ) {
        try {
            final var process = onboardingService.findProcess(identityVerification.getProcessId());
            final var acceptedDocuments = selectAcceptedDocuments(identityVerification);
            final var documentCheckResult = config.isSendingExtractedDataEnabled() ?
                    buildDocumentCheckResultWithExtractedData(acceptedDocuments) :
                    buildDocumentCheckResultWithoutExtractedData(acceptedDocuments);
            final var verificationId = fetchVerificationId(identityVerification, acceptedDocuments);

            final var request = EvaluateClientRequest.builder()
                    .processId(process.getId())
                    .processType(process.getProcessConfiguration().getProcessType())
                    .userId(identityVerification.getUserId())
                    .identityVerificationId(identityVerification.getId())
                    .verificationId(verificationId)
                    .provider(config.getDocumentVerificationProvider())
                    .status(EvaluateClientRequest.Status.SUCCESS)
                    .documentCheckResult(documentCheckResult)
                    .build();

            final var response = retryTemplate.execute(context -> callEvaluateClient(request, context));
            processEvaluationResponse(identityVerification, ownerId, response);
            return response.getEvaluationResult();
        } catch (final RetryException e) {
            processTooManyEvaluationError(identityVerification, ownerId);
            return null;
        } catch (final OnboardingProviderException | OnboardingProcessException | RuntimeException e) {
            processVerificationIdError(identityVerification, ownerId, e);
            return null;
        }
    }

    private EvaluateClientResponse callEvaluateClient(final EvaluateClientRequest request, final RetryContext context) throws OnboardingProviderException {
        final var maxAttempts = config.getClientEvaluationMaxFailedAttempts();
        final int attempt = context.getRetryCount() + 1;

        final Throwable lastThrowable = context.getLastThrowable();
        if (lastThrowable != null) {
            logger.info("action: callEvaluateClient, state: initiated, attempt {}/{}, previous failure: {}", attempt, maxAttempts, lastThrowable.getMessage());
        } else {
            logger.info("action: callEvaluateClient, state: initiated, attempt {}/{}", attempt, maxAttempts);
        }

        try {
            final var response = onboardingProvider.evaluateClient(request);
            logger.info("action: callEvaluateClient, state: succeeded");
            return response;
        } catch (final Exception e) {
            if (attempt >= maxAttempts) {
                logger.info("action: callEvaluateClient, state: failed, exceptionMessage: {}", e.getMessage());
                throw new RetryException("Evaluate client call reached retry limit", e);
            }

            throw e;
        }
    }

    private EvaluateClientRequest.DocumentCheckResult buildDocumentCheckResultWithExtractedData(
            final Set<DocumentVerificationEntity> documentsVerification
    ) {
        final var photoIds = documentsVerification.stream()
                .map(DocumentVerificationEntity::getPhotoId)
                .filter(Objects::nonNull)
                .collect(toSet());

        final var processedDocumentByPhotoId = fetchProcessedDocuments(photoIds);

        final var documents = new ArrayList<EvaluateClientRequest.Document>(documentsVerification.size());

        for (final DocumentVerificationEntity documentVerification : documentsVerification) {
            final var documentResult = selectLatestDocumentResult(documentVerification);
            final var extractedData = parseExtractedData(documentResult);
            final var documentData = buildDocumentData(extractedData);

            if (documentVerification.getSide() == CardSide.FRONT) {

                final var country = Optional.ofNullable(extractedData)
                        .map(DocumentExtractedDataValue::country)
                        .orElse(null);

                final var processedDocument = processedDocumentByPhotoId.getOrDefault(documentVerification.getPhotoId(), null);
                final var images = buildImages(processedDocument);

                final var document = EvaluateClientRequest.Document.builder()
                        .type(documentVerification.getType())
                        .country(country)
                        .status(EvaluateClientRequest.Status.SUCCESS) // so far the request is sent only in case of success
                        .score(10) // so far sending constant 10 as 100 percent confidence, possible future extension point
                        .data(documentData)
                        .images(images)
                        .rawData(documentResult.getVerificationResult())
                        .build();

                documents.add(document);
            }
        }

        final var person = buildPerson(documents);

        return new EvaluateClientRequest.DocumentCheckResult(documents, person);
    }

    private static EvaluateClientRequest.Person buildPerson(final List<EvaluateClientRequest.Document> documents) {
        String surname = null;
        String givenNames = null;
        LocalDate dateOfBirth = null;

        for (final var document : documents) {
            final var documentData = document.data();
            if (documentData == null) {
                return null;
            }

            if (surname == null) {
                surname = documentData.surname();
            }

            if (givenNames == null) {
                givenNames = documentData.givenNames();
            }

            if (dateOfBirth == null) {
                dateOfBirth = documentData.dateOfBirth();
            }
        }

        return EvaluateClientRequest.Person.builder()
                .surname(surname)
                .givenNames(givenNames)
                .dateOfBirth(dateOfBirth)
                .build();
    }

    private static List<EvaluateClientRequest.Image> buildImages(final ProcessedDocumentDataEntity processedDocumentData) {
        if (processedDocumentData == null) {
            return List.of();
        }

        return List.of(
                EvaluateClientRequest.Image.builder()
                        .type(processedDocumentData.getDataType())
                        .data(processedDocumentData.getData())
                        .build()
        );
    }

    private static EvaluateClientRequest.DocumentCheckResult buildDocumentCheckResultWithoutExtractedData(
            final Set<DocumentVerificationEntity> documentsVerification
    ) {
        final var documents = new ArrayList<EvaluateClientRequest.Document>(documentsVerification.size());

        for (final DocumentVerificationEntity documentVerification : documentsVerification) {
            final var document = EvaluateClientRequest.Document.builder()
                    .type(documentVerification.getType())
                    .status(EvaluateClientRequest.Status.SUCCESS)
                    .images(new ArrayList<>())
                    .build();

            documents.add(document);
        }

        return new EvaluateClientRequest.DocumentCheckResult(documents, null);
    }

    private static Set<DocumentVerificationEntity> selectAcceptedDocuments(final IdentityVerificationEntity identityVerification) {
        return identityVerification.getDocumentVerifications().stream()
                .filter(DocumentVerificationEntity::isUsedForVerification)
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(toSet());
    }

    private static String fetchVerificationId(final IdentityVerificationEntity identityVerification, final Set<DocumentVerificationEntity> documents) {
        final Set<String> verificationIds = documents.stream()
                .map(DocumentVerificationEntity::getVerificationId)
                .collect(toSet());

        if (verificationIds.size() == 1) {
            return verificationIds.iterator().next();
        } else {
            throw new IllegalStateException(
                    String.format("Expected just one document verificationId for %s but got %s", identityVerification, verificationIds));
        }
    }

    private static DocumentResultEntity selectLatestDocumentResult(final DocumentVerificationEntity documentVerificationEntity) {
        return documentVerificationEntity.getResults().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing document result for %s".formatted(documentVerificationEntity)));
    }

    private static LocalDate convertDate(final String date) {
        if (date == null) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(date,formatter);
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("DD.MM.YYYY");
                return LocalDate.parse(date,formatter);
            } catch (DateTimeParseException  ex) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("DD MM YYYY");
                    return LocalDate.parse(date, formatter);
                } catch (DateTimeParseException  ex2) {
                    return null;
                }
            }
        }
    }

    private static EvaluateClientRequest.DocumentData buildDocumentData(final DocumentExtractedDataValue extractedData) {
        if (extractedData == null) {
            return null;
        }

        return EvaluateClientRequest.DocumentData.builder()
                .givenNames(extractedData.givenNames())
                .surname(extractedData.surname())
                .dateOfBirth(convertDate(extractedData.dateOfBirth()))
                .placeOfBirth(extractedData.placeOfBirth())
                .sex(extractedData.sex())
                .nationality(extractedData.nationality())
                .personalNumber(extractedData.personalNumber())
                .documentNumber(extractedData.documentNumber())
                .dateOfIssue(convertDate(extractedData.dateOfIssue()))
                .dateOfExpiry(convertDate(extractedData.dateOfExpiry()))
                .authority(extractedData.authority())
                .build();
    }

    private DocumentExtractedDataValue parseExtractedData(final DocumentResultEntity documentResult) {
        try {
            return objectMapper.readValue(documentResult.getExtractedData(), DocumentExtractedDataValue.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse extracted data for document result id {}", documentResult.getId(), e);
            return null;
        }
    }

    private Map<String, ProcessedDocumentDataEntity> fetchProcessedDocuments(final Set<String> ids) {
        return StreamSupport.stream(processedDocumentDataRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(ProcessedDocumentDataEntity::getId, Function.identity()));
    }

    private void processTooManyEvaluationError(final IdentityVerificationEntity identityVerification, final OwnerId ownerId) {
        logger.warn("Client evaluation too many attempts for {} - {}", identityVerification, ownerId);
        identityVerification.setErrorDetail(IdentityVerificationEntity.ERROR_MAX_FAILED_ATTEMPTS_CLIENT_EVALUATION);
        identityVerification.setErrorOrigin(ErrorOrigin.PROCESS_LIMIT_CHECK);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
    }

    private void processVerificationIdError(final IdentityVerificationEntity identityVerification, final OwnerId ownerId, final Exception e) {
        logger.warn("Client evaluation failed to get verificationId for {}, {} - {}", identityVerification, ownerId, e.getMessage());
        logger.debug("Client evaluation failed to get verificationId for {}, {}", identityVerification, ownerId, e);
        identityVerification.setErrorDetail(ERROR_VERIFICATION_ID);
        identityVerification.setErrorOrigin(ErrorOrigin.CLIENT_EVALUATION);
        identityVerification.setTimestampFailed(ownerId.getTimestamp());
    }

    private void processEvaluationResponse(final IdentityVerificationEntity identityVerification, final OwnerId ownerId, final EvaluateClientResponse response) {
        auditService.auditOnboardingProvider(identityVerification, "Client evaluated for user: {}", ownerId.getUserId());
        final var identityVerificationId = identityVerification.getId();

        if (EvaluateClientResponse.EvaluationResult.OK == response.getEvaluationResult()) {
            logger.info("Client evaluation accepted for identity verification id: {}", identityVerificationId);
        } else if (EvaluateClientResponse.EvaluationResult.NOK == response.getEvaluationResult()) {
            logger.info("Client evaluation rejected for identity verification id: {}", identityVerificationId);
            identityVerification.getDocumentVerifications()
                    .forEach(document -> {
                        document.setStatus(DocumentStatus.REJECTED);
                        auditService.audit(document, "Document rejected because of client evaluation for user: {}", identityVerification.getUserId());
                    });
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
        } else { // WAIT
            logger.info("Client evaluation waiting for identity verification id: {}", identityVerificationId);
        }
    }
}
