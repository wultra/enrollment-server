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
package com.wultra.app.docverify.zenid.service;

import com.google.common.base.Preconditions;
import com.wultra.app.docverify.zenid.config.ZenidConfigProps;
import com.wultra.app.docverify.zenid.model.api.*;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SubmittedDocument;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Implementation of the REST service to ZenID (https://zenid.trask.cz/)
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.document-verification.provider", havingValue = "zenid")
@Service
public class ZenidRestApiService {

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
     * @param sessionId Session id which allows to link several uploads together.
     * @param document Submitted document.
     * @return Response entity with the upload result
     */
    public ResponseEntity<ZenidWebUploadSampleResponse> uploadSample(OwnerId ownerId, String sessionId, SubmittedDocument document)
            throws RestClientException {
        Preconditions.checkNotNull(document.getPhoto(), "Missing photo in " + document);

        MultiValueMap<String, String> queryParams = buildQueryParams(ownerId, sessionId, document);

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

        return restClient.post("/api/sample", bodyBuilder.build(), queryParams, httpHeaders, RESPONSE_TYPE_REFERENCE_UPLOAD_SAMPLE);
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
        return restClient.get(apiPath, RESPONSE_TYPE_REFERENCE_UPLOAD_SAMPLE);
    }

    /**
     * Investigates uploaded samples
     *
     * @param sampleIds Ids of previously uploaded samples.
     * @return Response entity with the investigation result
     */
    public ResponseEntity<ZenidWebInvestigateResponse> investigateSamples(List<String> sampleIds)
            throws RestClientException {
        Preconditions.checkArgument(sampleIds.size() > 0, "Missing sample ids for investigation");

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        sampleIds.forEach(sampleId -> queryParams.add("sampleIDs", sampleId));
        queryParams.add("async", String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase());

        return restClient.get("/api/investigateSamples", queryParams, EMPTY_ADDITIONAL_HEADERS, RESPONSE_TYPE_REFERENCE_INVESTIGATE);
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
        return restClient.get("/api/deleteSample", queryParams, EMPTY_ADDITIONAL_HEADERS, RESPONSE_TYPE_REFERENCE_DELETE);
    }

    /**
     * Gets image data belonging to the specified hash
     * @param imageHash Image hash
     * @return Response entity with the image data
     */
    public ResponseEntity<byte[]> getImage(String imageHash) throws RestClientException {
        String apiPath = String.format("/History/Image/%s", imageHash);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
        return restClient.get(apiPath, EMPTY_QUERY_PARAMS, httpHeaders, RESPONSE_TYPE_BYTE_ARRAY);
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
        return restClient.get(apiPath, RESPONSE_TYPE_REFERENCE_INVESTIGATE);
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

        return restClient.get("/api/initSdk", queryParams, EMPTY_ADDITIONAL_HEADERS, RESPONSE_TYPE_REFERENCE_INIT_SDK);
    }

    private MultiValueMap<String, String> buildQueryParams(OwnerId ownerId, String sessionId, SubmittedDocument document) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        queryParams.add("async", String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase());
        queryParams.add("expectedSampleType", toSampleType(document.getType()).toString());
        queryParams.add("customData", ownerId.getActivationId());
        queryParams.add("uploadSessionID", sessionId);
        queryParams.add("country", configProps.getDocumentCountry().toString());

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
        switch (documentType) {
            case DRIVING_LICENSE:
                return ZenidSharedMineAllResult.DocumentCodeEnum.DRV;
            case PASSPORT:
                return ZenidSharedMineAllResult.DocumentCodeEnum.PAS;
//            case ID_CARD:
//                // Not supported more than one version of a document
//                return List.of(ZenidSharedMineAllResult.DocumentCodeEnum.IDC1, ZenidSharedMineAllResult.DocumentCodeEnum.IDC2);
            default:
                return null;
        }
    }

    private @Nullable ZenidSharedMineAllResult.DocumentRoleEnum toDocumentRole(DocumentType documentType) {
        switch (documentType) {
            case DRIVING_LICENSE:
                return ZenidSharedMineAllResult.DocumentRoleEnum.DRV;
            case ID_CARD:
                return ZenidSharedMineAllResult.DocumentRoleEnum.IDC;
            case PASSPORT:
                return ZenidSharedMineAllResult.DocumentRoleEnum.PAS;
            default:
                return null;
        }
    }

    private ZenidSharedMineAllResult.PageCodeEnum toPageCodeEnum(CardSide cardSide) {
        switch (cardSide) {
            case FRONT:
                return ZenidSharedMineAllResult.PageCodeEnum.F;
            case BACK:
                return ZenidSharedMineAllResult.PageCodeEnum.B;
            default:
                throw new IllegalStateException("Unexpected card side value: " + cardSide);
        }
    }

    private ZenidWebUploadSampleResponse.SampleTypeEnum toSampleType(DocumentType type) {
        switch (type) {
            case ID_CARD:
            case DRIVING_LICENSE:
            case PASSPORT:
                return ZenidWebUploadSampleResponse.SampleTypeEnum.DOCUMENTPICTURE;
            case SELFIE_PHOTO:
                return ZenidWebUploadSampleResponse.SampleTypeEnum.SELFIE;
            default:
                throw new IllegalStateException("Not supported documentType: " + type);
        }
    }

}
