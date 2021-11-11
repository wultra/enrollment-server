/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.app.presencecheck.iproov.service;

import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.presencecheck.iproov.config.IProovConfigProps;
import com.wultra.app.presencecheck.iproov.model.api.ClaimValidateRequest;
import com.wultra.app.presencecheck.iproov.model.api.EnrolImageBody;
import com.wultra.app.presencecheck.iproov.model.api.ServerClaimRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of the REST service to iProov (https://www.iproov.com/)
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.presence-check.provider", havingValue = "iproov")
@Service
public class IProovRestApiService {

    public static final String I_PROOV_RESOURCE = "presence_check";

    private final IProovConfigProps configProps;

    private final RestTemplate restTemplate;

    @Autowired
    public IProovRestApiService(
            IProovConfigProps configProps,
            @Qualifier("restTemplateIProov") RestTemplate restTemplate) {
        this.configProps = configProps;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> generateEnrolToken(OwnerId id) {
        ServerClaimRequest request = createServerClaimRequest(id);

        HttpEntity<ServerClaimRequest> requestEntity = createDefaultRequestEntity(request);

        return restTemplate.postForEntity("/claim/enrol/token", requestEntity, String.class);
    }

    public ResponseEntity<String> enrolUserImageForToken(String token, Image photo) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("api_key", configProps.getApiKey());
        body.add("secret", configProps.getApiSecret());
        body.add("rotation", 0);
        body.add("source", EnrolImageBody.SourceEnum.SELFIE.toString());
        body.add("image", new ByteArrayResource(photo.getData()) {

            @Override
            public String getFilename() {
                return photo.getFilename();
            }

        });
        body.add("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForEntity("/claim/enrol/image", requestEntity, String.class);
    }

    public ResponseEntity<String> generateVerificationToken(OwnerId id) {
        ServerClaimRequest request = createServerClaimRequest(id);

        HttpEntity<ServerClaimRequest> requestEntity = createDefaultRequestEntity(request);

        return restTemplate.postForEntity("/claim/verify/token", requestEntity, String.class);
    }

    public ResponseEntity<String> validateVerification(OwnerId id, String token) {
        ClaimValidateRequest request = new ClaimValidateRequest();
        request.setApiKey(configProps.getApiKey());
        request.setSecret(configProps.getApiSecret());
        request.setClient("Wultra Enrollment Server, activationId: " + id.getActivationId()); // TODO value from the device
        request.setIp("192.168.1.1"); // TODO deprecated but still required
        request.setRiskProfile(configProps.getRiskProfile());
        request.setToken(token);
        request.setUserId(id.getActivationId());

        HttpEntity<ClaimValidateRequest> requestEntity = createDefaultRequestEntity(request);

        return restTemplate.postForEntity("/claim/verify/validate", requestEntity, String.class);
    }

    public ResponseEntity<String> deleteUserPersona(OwnerId id) {
        // TODO implement with oauth call on DELETE /users/{activationId}
        throw new IllegalStateException("Not implemented yet");
    }

    private ServerClaimRequest createServerClaimRequest(OwnerId id) {
        ServerClaimRequest request = new ServerClaimRequest();
        request.setApiKey(configProps.getApiKey());
        request.setSecret(configProps.getApiSecret());
        request.setAssuranceType(configProps.getAssuranceType());
        request.setResource(I_PROOV_RESOURCE);
        request.setRiskProfile(configProps.getRiskProfile());
        request.setUserId(id.getActivationId());
        return request;
    }

    private <T> HttpEntity<T> createDefaultRequestEntity(T entity) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(entity, headers);
    }

}
