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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the REST service to ZenID (https://zenid.trask.cz/)
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.document-verification.provider", havingValue = "zenid")
@Service
public class ZenidRestApiService {

    /**
     * Configuration properties.
     */
    private final ZenidConfigProps configProps;

    /**
     * REST template for ZenID calls.
     */
    private final RestTemplate restTemplate;

    /**
     * Service constructor.
     *
     * @param configProps Configuration properties.
     * @param restTemplate REST template for ZenID calls.
     */
    @Autowired
    public ZenidRestApiService(
            ZenidConfigProps configProps,
            @Qualifier("restTemplateZenid") RestTemplate restTemplate) {
        this.configProps = configProps;
        this.restTemplate = restTemplate;
    }

    /**
     * Uploads photo data as a sample DocumentPicture
     *
     * @param ownerId Owner identification.
     * @param sessionId Session id which allows to link several uploads together.
     * @param document Submitted document.
     * @return Response entity with the upload result
     */
    public ResponseEntity<ZenidWebUploadSampleResponse> uploadSample(OwnerId ownerId, String sessionId, SubmittedDocument document) {
        Preconditions.checkNotNull(document.getPhoto(), "Missing photo in " + document);

        String apiPath = buildApiUploadPath(ownerId, sessionId, document);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(document.getPhoto().getData()) {
            @Override
            public String getFilename() {
                return document.getPhoto().getFilename();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity(apiPath, requestEntity, ZenidWebUploadSampleResponse.class);
    }

    /**
     * Synchronizes submitted document result of a previous upload.
     *
     * @param ownerId Owner identification.
     * @param documentId Submitted document id.
     * @return Response entity with the upload result
     */
    public ResponseEntity<ZenidWebUploadSampleResponse> syncSample(OwnerId ownerId, String documentId) {
        String apiPath = "/api/sample/" + documentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(apiPath, HttpMethod.GET, requestEntity, ZenidWebUploadSampleResponse.class);
    }

    /**
     * Investigates uploaded samples
     *
     * @param sampleIds Ids of previously uploaded samples.
     * @return Response entity with the investigation result
     */
    public ResponseEntity<ZenidWebInvestigateResponse> investigateSamples(List<String> sampleIds) {
        Preconditions.checkArgument(sampleIds.size() > 0, "Missing sample ids for investigation");

        String apiPath = "/api/investigateSamples";

        String querySampleIds = sampleIds.stream()
                .map(sampleId -> "sampleIDs=" + sampleId)
                .collect(Collectors.joining("&"));
        apiPath += "?" + querySampleIds;

        apiPath += "&async=" + String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase();

        HttpEntity<Void> entity = createDefaultRequestEntity();
        return restTemplate.exchange(apiPath, HttpMethod.GET, entity, ZenidWebInvestigateResponse.class);
    }

    /**
     * Deletes an uploaded sample
     *
     * @param sampleId Id of previously uploaded sample.
     * @return Response entity with the deletion result
     */
    public ResponseEntity<ZenidWebDeleteSampleResponse> deleteSample(String sampleId) {
        String apiPath = String.format("/api/deleteSample?sampleId=%s", sampleId);
        HttpEntity<Void> entity = createDefaultRequestEntity();
        return restTemplate.exchange(apiPath, HttpMethod.GET, entity, ZenidWebDeleteSampleResponse.class);
    }

    /**
     * Gets image data belonging to the specified hash
     * @param imageHash Image hash
     * @return Response entity with the image data
     */
    public ResponseEntity<byte[]> getImage(String imageHash) {
        String apiPath = String.format("/History/Image/%s", imageHash);
        HttpEntity<Void> entity = createAcceptOctetStreamRequestEntity();
        return restTemplate.exchange(apiPath, HttpMethod.GET, entity, byte[].class);
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
    public ResponseEntity<ZenidWebInvestigateResponse> getInvestigation(String investigationId) {
        String apiPath = String.format("/api/investigation/%s", investigationId);
        HttpEntity<Void> entity = createDefaultRequestEntity();
        return restTemplate.exchange(apiPath, HttpMethod.GET, entity, ZenidWebInvestigateResponse.class);
    }

    /**
     * Initializes the SDK of ZenID
     *
     * @param token Initialization token
     * @return Response entity with the SDK initialization result
     */
    public ResponseEntity<ZenidWebInitSdkResponse> initSdk(String token) {
        String apiPath = String.format("/api/initSdk?token=%s", token);
        HttpEntity<Void> entity = createDefaultRequestEntity();
        return restTemplate.exchange(apiPath, HttpMethod.GET, entity, ZenidWebInitSdkResponse.class);
    }

    private String buildApiUploadPath(OwnerId ownerId, String sessionId, SubmittedDocument document) {
        StringBuilder apiPathBuilder = new StringBuilder("/api/sample")
                .append("?")
                .append("async=").append(String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase())
                .append("&expectedSampleType=").append(toSampleType(document.getType()))
                .append("&customData=").append(ownerId.getActivationId())
                .append("&uploadSessionID=").append(sessionId);

        apiPathBuilder.append("&country=").append(configProps.getDocumentCountry());

        ZenidSharedMineAllResult.DocumentCodeEnum documentCode = toDocumentCode(document.getType());
        if (documentCode != null) {
            apiPathBuilder.append("&documentCode=").append(documentCode);
        }

        if (document.getSide() != null) {
            apiPathBuilder.append("&pageCode=").append(toPageCodeEnum(document.getSide()));
        }

        ZenidSharedMineAllResult.DocumentRoleEnum documentRole = toDocumentRole(document.getType());
        if (documentRole != null) {
            apiPathBuilder.append("&role=").append(documentRole);
        }

        return apiPathBuilder.toString();
    }

    private HttpEntity<Void> createDefaultRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Void> createAcceptOctetStreamRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return new HttpEntity<>(headers);
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

    @Nullable
    private ZenidSharedMineAllResult.PageCodeEnum toPageCodeEnum(@Nullable CardSide cardSide) {
        if (cardSide == null) {
            return null;
        }
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
