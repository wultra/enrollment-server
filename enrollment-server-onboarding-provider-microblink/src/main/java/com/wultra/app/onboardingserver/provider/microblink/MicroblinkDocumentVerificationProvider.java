/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.microblink;

import com.github.benmanes.caffeine.cache.Cache;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.microblink.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.microblink.com/">Microblink</a>.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "microblink")
@Component
public class MicroblinkDocumentVerificationProvider implements DocumentVerificationProvider {

    private final Cache<UUID, SubmittedDocument> documentCache;
    private final RestClient restClient;

    @Autowired
    public MicroblinkDocumentVerificationProvider(
            @Qualifier("microblinkDocumentCache") Cache<UUID, SubmittedDocument> documentCache,
            @Qualifier("microblinkRestClient") RestClient restClient
    ) {
        this.documentCache = documentCache;
        this.restClient = restClient;
    }

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) {
        throw new UnsupportedOperationException("Method checkDocumentUpload is not supported by Microblink provider.");
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) {
        final var results = new ArrayList<DocumentSubmitResult>();

        for (SubmittedDocument document : documents) {
            var uploadUuid = UUID.randomUUID();
            documentCache.put(uploadUuid, document);

            final var documentResult = new DocumentSubmitResult();
            documentResult.setDocumentId(document.getDocumentId());
            documentResult.setUploadId(uploadUuid.toString());
            results.add(documentResult);
            // TODO: set 'extractedPhotoId'
        }

        final var result = new DocumentsSubmitResult();
        result.setResults(results);
        return result;
    }

    @Override
    public boolean shouldStoreSelfie() {
        return false;
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        final var documentUuids = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        final var documents = documentCache.getAllPresent(documentUuids);
        final var frontDocument = findDocument(documents, CardSide.FRONT);
        final var backDocument = findDocument(documents, CardSide.BACK);

        final var request = buildRequest(frontDocument, backDocument);

        try {
            final var response = restClient.post("/api/v2/docver", request, new ParameterizedTypeReference<DocumentVerificationResponse>() {});
            final var responseBody = Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new DocumentVerificationException("Response body is empty"));

            final var responseRuntime = responseBody.getRuntime();
            final var responseVerification = responseBody.getVerification();

            final var result = new DocumentsVerificationResult();
            result.setVerificationId(responseRuntime.getTraceId());
            result.setStatus(responseVerification.getResult() == CheckResult.PASS ? DocumentVerificationStatus.ACCEPTED : DocumentVerificationStatus.REJECTED);

            return result;
        } catch (final RestClientException e) {
            throw new RemoteCommunicationException(
                    "Failed REST call to verify documents %s in Microblink, statusCode=%s, responseBody='%s', %s".formatted(
                            String.join(",", uploadIds),
                            e.getStatusCode(),
                            e.getResponse(),
                            id
                    ),
                    e);
        }
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) {
        throw new UnsupportedOperationException("Method getVerificationResult is not supported by Microblink provider.");
    }

    @Override
    public Image getPhoto(String photoId) {
        throw new UnsupportedOperationException("Method getPhoto is not implemented by Microblink provider.");
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) {
        final var uploadUuid = uploadIds.stream()
                .map(UUID::fromString)
                .toList();

        documentCache.invalidateAll(uploadUuid);
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        // TODO
        throw new NotImplementedException();
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) {
        return new VerificationSdkInfo();
    }

    private static DocumentVerificationImageSource buildImageSource(final Image image) {
        final var imageBase64 = Base64.getEncoder().encodeToString(image.getData());

        final var imageSource = new DocumentVerificationImageSource();
        imageSource.setBase64(imageBase64);
        return imageSource;
    }

    private static DocumentVerificationRequest buildRequest(final SubmittedDocument frontDocument, final SubmittedDocument backDocument) {
        final var frontImageSource = buildImageSource(frontDocument.getPhoto());
        final var backImageSource = buildImageSource(backDocument.getPhoto());

        final var useCase = new DocumentVerificationUseCaseOptions();

        final var request = new DocumentVerificationRequest();
        request.setImageFront(frontImageSource);
        request.setImageBack(backImageSource);
        request.setUseCase(useCase);
        return request;
    }

    private static SubmittedDocument findDocument(final Map<UUID, SubmittedDocument> documents, final CardSide side) throws DocumentVerificationException {
        return documents.values()
                .stream()
                .filter(v -> v.getSide() == side)
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException("Document site %s is missing".formatted(side)));
    }
}
