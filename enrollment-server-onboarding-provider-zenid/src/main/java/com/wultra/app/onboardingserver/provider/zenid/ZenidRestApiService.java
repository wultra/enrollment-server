/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.zenid;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SubmittedDocument;
import com.wultra.app.onboardingserver.provider.zenid.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * Implementation of the REST service to <a href="https://zenid.trask.cz/">ZenID</a>.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "zenid")
@Service
@Slf4j
class ZenidRestApiService {

    private static final MultiValueMap<String, String> EMPTY_ADDITIONAL_HEADERS = new LinkedMultiValueMap<>();

    private static final MultiValueMap<String, String> EMPTY_QUERY_PARAMS = new LinkedMultiValueMap<>();

    private static final ParameterizedTypeReference<byte[]> RESPONSE_TYPE_BYTE_ARRAY =
            new ParameterizedTypeReference<>() { };

    private static final ParameterizedTypeReference<ZenidWebDeleteSampleResponse> RESPONSE_TYPE_REFERENCE_DELETE =
            new ParameterizedTypeReference<>() { };

    private static final ParameterizedTypeReference<ZenidWebInitSdkResponse> RESPONSE_TYPE_REFERENCE_INIT_SDK =
            new ParameterizedTypeReference<>() { };

    private static final ParameterizedTypeReference<ZenidWebInvestigateResponse> RESPONSE_TYPE_REFERENCE_INVESTIGATE =
            new ParameterizedTypeReference<>() { };

    private static final ParameterizedTypeReference<ZenidWebUploadSampleResponse> RESPONSE_TYPE_REFERENCE_UPLOAD_SAMPLE =
            new ParameterizedTypeReference<>() { };

    /**
     * Configuration properties.
     */
    private final ZenidConfigProps configProps;

    /**
     * REST client for ZenID calls.
     */
    private final RestClient restClient;

    /**
     * Service constructor.
     *
     * @param configProps Configuration properties.
     * @param restClient REST client for ZenID calls.
     */
    @Autowired
    public ZenidRestApiService(
            ZenidConfigProps configProps,
            @Qualifier("restClientZenid") RestClient restClient) {
        this.configProps = configProps;
        this.restClient = restClient;
    }

    /**
     * Uploads photo data as a sample DocumentPicture
     *
     * @param ownerId Owner identification.
     * @param document Submitted document.
     * @return Response entity with the upload result
     */
    public ResponseEntity<ZenidWebUploadSampleResponse> uploadSample(OwnerId ownerId, SubmittedDocument document)
            throws RestClientException {
        Validate.notNull(document.getPhoto(), "Missing photo in " + document);

        final MultiValueMap<String, String> queryParams = buildQueryParams(ownerId, document);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ByteArrayResource(document.getPhoto().getData()) {
                    @Override
                    public String getFilename() {
                        return document.getPhoto().getFilename();
                    }
                }
        );

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        final ResponseEntity<ZenidWebUploadSampleResponse> response =
                restClient.post("/api/sample", bodyBuilder.build(), queryParams, httpHeaders, RESPONSE_TYPE_REFERENCE_UPLOAD_SAMPLE);
        logger.debug("/api/sample response status code: {}, {}", response.getStatusCode(), ownerId);
        logger.trace("/api/sample response: {}, {}", response, ownerId);
        return response;
    }

    /**
     * Synchronizes submitted document result of a previous upload.
     *
     * @param documentId Submitted document id.
     * @return Response entity with the upload result
     */
    public ResponseEntity<ZenidWebUploadSampleResponse> syncSample(String documentId)
            throws RestClientException {
        String apiPath = "/api/sample/" + documentId;
        final ResponseEntity<ZenidWebUploadSampleResponse> response = restClient.get(apiPath, RESPONSE_TYPE_REFERENCE_UPLOAD_SAMPLE);
        logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
        logger.trace("{} response {}", apiPath, response);
        return response;
    }

    /**
     * Investigates uploaded samples
     *
     * @param sampleIds Ids of previously uploaded samples.
     * @return Response entity with the investigation result
     */
    public ResponseEntity<ZenidWebInvestigateResponse> investigateSamples(List<String> sampleIds) throws RestClientException {
        Validate.notEmpty(sampleIds, "Missing sample ids for investigation");

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        sampleIds.forEach(sampleId -> queryParams.add("sampleIDs", sampleId));
        queryParams.add("async", String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase());

        configProps.getProfile().ifPresent(profile ->
                queryParams.add("profile", profile));

        final ResponseEntity<ZenidWebInvestigateResponse> response =
                restClient.get("/api/investigateSamples", queryParams, EMPTY_ADDITIONAL_HEADERS, RESPONSE_TYPE_REFERENCE_INVESTIGATE);
        logger.debug("/api/investigateSamples response status code: {} for IDs: {}", response.getStatusCode(), sampleIds);
        logger.trace("/api/investigateSamples response: {} for IDs: {}", response, sampleIds);
        return response;
    }

    /**
     * Deletes an uploaded sample
     *
     * @param sampleId Id of previously uploaded sample.
     * @return Response entity with the deletion result
     */
    public ResponseEntity<ZenidWebDeleteSampleResponse> deleteSample(String sampleId) throws RestClientException {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("sampleId", sampleId);
        final ResponseEntity<ZenidWebDeleteSampleResponse> response =
                restClient.get("/api/deleteSample", queryParams, EMPTY_ADDITIONAL_HEADERS, RESPONSE_TYPE_REFERENCE_DELETE);
        logger.debug("/api/deleteSample/{} response: {}", sampleId, response);
        return response;
    }

    /**
     * Gets image data belonging to the specified hash
     * @param imageHash Image hash
     * @return Response entity with the image data
     */
    public ResponseEntity<byte[]> getImage(String imageHash) throws RestClientException {
        final String apiPath = String.format("/History/Image/%s", imageHash);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        final ResponseEntity<byte[]> result = restClient.get(apiPath, EMPTY_QUERY_PARAMS, httpHeaders, RESPONSE_TYPE_BYTE_ARRAY);
        logger.debug("{} called", apiPath);
        return result;
    }

    /**
     * Provides result of an investigation.
     *
     * <p>
     *   Only failed validation results are returned. All document samples without a validation constraint
     *   are considered as passed.
     * </p>
     *
     * @param investigationId Id of a previously run investigation
     * @return Response entity with the investigation result
     */
    public ResponseEntity<ZenidWebInvestigateResponse> getInvestigation(String investigationId)
            throws RestClientException {
        String apiPath = String.format("/api/investigation/%s", investigationId);
        final ResponseEntity<ZenidWebInvestigateResponse> response = restClient.get(apiPath, RESPONSE_TYPE_REFERENCE_INVESTIGATE);
        logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
        logger.trace("{} response: {}", apiPath, response);
        return response;
    }

    /**
     * Initializes the SDK of ZenID
     *
     * @param token Initialization token
     * @return Response entity with the SDK initialization result
     */
    public ResponseEntity<ZenidWebInitSdkResponse> initSdk(String token) throws RestClientException {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("token", token);

        final ResponseEntity<ZenidWebInitSdkResponse> response =
                restClient.get("/api/initSdk", queryParams, EMPTY_ADDITIONAL_HEADERS, RESPONSE_TYPE_REFERENCE_INIT_SDK);
        logger.debug("/api/initSdk response: {}", response);
        return response;
    }

    private MultiValueMap<String, String> buildQueryParams(OwnerId ownerId, SubmittedDocument document) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        queryParams.add("async", String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase());
        queryParams.add("expectedSampleType", toSampleType(document.getType()).toString());
        queryParams.add("customData", ownerId.getActivationId());
        queryParams.add("country", configProps.getDocumentCountry().toString());

        configProps.getProfile().ifPresent(profile ->
                queryParams.add("profile", profile));

        ZenidSharedMineAllResult.DocumentCodeEnum documentCode = toDocumentCode(document.getType());
        if (documentCode != null) {
            queryParams.add("documentCode", documentCode.toString());
        }

        if (document.getSide() != null) {
            queryParams.add("pageCode", toPageCodeEnum(document.getSide()).toString());
        }

        ZenidSharedMineAllResult.DocumentRoleEnum documentRole = toDocumentRole(document.getType());
        if (documentRole != null) {
            queryParams.add("role", documentRole.toString());
        }

        return queryParams;
    }

    private @Nullable ZenidSharedMineAllResult.DocumentCodeEnum toDocumentCode(DocumentType documentType) {
        return switch (documentType) {
            case DRIVING_LICENSE -> ZenidSharedMineAllResult.DocumentCodeEnum.DRV;
            case PASSPORT -> ZenidSharedMineAllResult.DocumentCodeEnum.PAS;
//            case ID_CARD ->
//                // Not supported more than one version of a document
//                List.of(ZenidSharedMineAllResult.DocumentCodeEnum.IDC1, ZenidSharedMineAllResult.DocumentCodeEnum.IDC2);
            default -> null;
        };
    }

    private @Nullable ZenidSharedMineAllResult.DocumentRoleEnum toDocumentRole(DocumentType documentType) {
        return switch (documentType) {
            case DRIVING_LICENSE -> ZenidSharedMineAllResult.DocumentRoleEnum.DRV;
            case ID_CARD -> ZenidSharedMineAllResult.DocumentRoleEnum.IDC;
            case PASSPORT -> ZenidSharedMineAllResult.DocumentRoleEnum.PAS;
            default -> null;
        };
    }

    private ZenidSharedMineAllResult.PageCodeEnum toPageCodeEnum(CardSide cardSide) {
        return switch (cardSide) {
            case FRONT -> ZenidSharedMineAllResult.PageCodeEnum.F;
            case BACK -> ZenidSharedMineAllResult.PageCodeEnum.B;
        };
    }

    private ZenidWebUploadSampleResponse.SampleTypeEnum toSampleType(DocumentType type) {
        return switch (type) {
            case ID_CARD, DRIVING_LICENSE, PASSPORT -> ZenidWebUploadSampleResponse.SampleTypeEnum.DOCUMENTPICTURE;
            case SELFIE_PHOTO -> ZenidWebUploadSampleResponse.SampleTypeEnum.SELFIE;
            default -> throw new IllegalStateException("Not supported documentType: " + type);
        };
    }

}
