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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.ProcessedDocumentDataEntity;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
@AllArgsConstructor
@Slf4j
public class ClientEvaluationService {

    private static final String ERROR_VERIFICATION_ID = "unableToGetDocumentVerificationId";

    private final OnboardingProvider onboardingProvider;

    private final IdentityVerificationConfig config;

    private final IdentityVerificationService identityVerificationService;

    private final AuditService auditService;

    private final CommonOnboardingService onboardingService;

    private final ProcessedDocumentDataRepository processedDocumentDataRepository;

    public static final String RESULT_KEY = "EVALUATION_RESULT";

    /**
     * Checks whether the client evaluation phase is enabled in the onboarding process.
     *
     * @param processId the onboarding process ID
     * @return whether the client evaluation phase is enabled
     * @throws OnboardingProcessException if the onboarding process configuration is not found
     */
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
    public EvaluateClientResponse.EvaluationResult processClientEvaluation(
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
                    .score(10)
                    .documentCheckResult(documentCheckResult)
                    .build();

            // TODO: Implement retry
            final var response = onboardingProvider.evaluateClient(request);
            return response.getEvaluationResult();
        } catch (final OnboardingProviderException | OnboardingProcessException e) {
            return EvaluateClientResponse.EvaluationResult.NOK;
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
            final var documentData = buildDocumentData(documentResult);

            final var processedDocument = processedDocumentByPhotoId.getOrDefault(documentVerification.getPhotoId(), null);
            final var images = List.of(
                    EvaluateClientRequest.Image.builder()
                            .type(processedDocument.getDataType())
                            .data(processedDocument.getData())
                            .build()
            );

            final var document = EvaluateClientRequest.Document.builder()
                    .type(documentVerification.getType())
                    .status( EvaluateClientRequest.Status.SUCCESS)
                    .data(documentData)
                    .images(images)
                    .rawData(documentResult.getVerificationResult())
                    .build();

            documents.add(document);
        }

        return new EvaluateClientRequest.DocumentCheckResult(documents);
    }

    private static EvaluateClientRequest.DocumentCheckResult buildDocumentCheckResultWithoutExtractedData(
            final Set<DocumentVerificationEntity> documentsVerification
    ) {
        final var documents = new ArrayList<EvaluateClientRequest.Document>(documentsVerification.size());

        for (final DocumentVerificationEntity documentVerification : documentsVerification) {
            final var document = EvaluateClientRequest.Document.builder()
                    .type(documentVerification.getType())
                    .status( EvaluateClientRequest.Status.SUCCESS)
                    .images(new ArrayList<>())
                    .build();

            documents.add(document);
        }

        return new EvaluateClientRequest.DocumentCheckResult(documents);
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

    private static EvaluateClientRequest.DocumentData buildDocumentData(final  DocumentResultEntity documentResult) {
        return EvaluateClientRequest.DocumentData.builder()
                // TODO
                .build();
    }

    private static List<EvaluateClientRequest.Image> buildImages(final List<ProcessedDocumentDataEntity> processedDocuments) {
        return processedDocuments.stream()
                .map(i -> new EvaluateClientRequest.Image(i.getDataType(), i.getData()))
                .toList();
    }

    private Map<String, ProcessedDocumentDataEntity> fetchProcessedDocuments(final Set<String> ids) {
        return StreamSupport.stream(processedDocumentDataRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(ProcessedDocumentDataEntity::getId, Function.identity()));
    }
}
