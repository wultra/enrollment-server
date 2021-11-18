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
import com.wultra.app.docverify.zenid.model.api.ZenidWebDeleteSampleResponse;
import com.wultra.app.docverify.zenid.model.api.ZenidWebInvestigateResponse;
import com.wultra.app.docverify.zenid.model.api.ZenidWebUploadSampleResponse;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
     * @param photo Photo image.
     * @return Response entity with the upload result
     */
    public ResponseEntity<ZenidWebUploadSampleResponse> uploadSample(OwnerId ownerId, String sessionId, Image photo) {
        String apiPath = String.format("/api/sample?" +
                "expectedSampleType=DocumentPicture" +
                "&customData=%s" +
                "&uploadSessionID=%s", ownerId.getActivationId(), sessionId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(photo.getData()) {
            @Override
            public String getFilename() {
                return photo.getFilename();
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity(apiPath, requestEntity, ZenidWebUploadSampleResponse.class);
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

        apiPath += "?async=" + String.valueOf(configProps.isAsyncProcessingEnabled()).toLowerCase();

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
     * Provides result of an investigation
     *
     * @param investigationId Id of a previously run investigation
     * @return Response entity with the investigation result
     */
    public ResponseEntity<ZenidWebInvestigateResponse> getInvestigation(String investigationId) {
        String apiPath = String.format("/api/investigation/%s", investigationId);
        HttpEntity<Void> entity = createDefaultRequestEntity();
        return restTemplate.exchange(apiPath, HttpMethod.GET, entity, ZenidWebInvestigateResponse.class);
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

}
