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
package com.wultra.app.presencecheck.iproov.service;

import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.presencecheck.iproov.config.IProovConfigProps;
import com.wultra.app.presencecheck.iproov.model.api.ClaimValidateRequest;
import com.wultra.app.presencecheck.iproov.model.api.EnrolImageBody;
import com.wultra.app.presencecheck.iproov.model.api.ServerClaimRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>
 *     The userId is filled with a secured user identification
 *     <ul>
 *         <li>optimizes API costs which are based on unique users</li>
 *         <li>hides potentially sensitive data from leaking at the external provider side</li>
 *     </ul>
 * </p>
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.presence-check.provider", havingValue = "iproov")
@Service
public class IProovRestApiService {

    private static final Logger logger = LoggerFactory.getLogger(IProovRestApiService.class);

    public static final String I_PROOV_RESOURCE_CONTEXT = "presence_check/";

    /**
     * Configuration properties.
     */
    private final IProovConfigProps configProps;

    /**
     * REST template for iProov calls.
     */
    private final RestTemplate restTemplate;

    /**
     * Service constructor.
     *
     * @param configProps Configuration properties.
     * @param restTemplate REST template for IProov calls.
     */
    @Autowired
    public IProovRestApiService(
            IProovConfigProps configProps,
            @Qualifier("restTemplateIProov") RestTemplate restTemplate) {
        this.configProps = configProps;
        this.restTemplate = restTemplate;
    }

    /**
     * Generates an enrolment token for a new user to enrol the service
     *
     * @param id Owner identification.
     * @return Response entity with the result json
     */
    public ResponseEntity<String> generateEnrolToken(OwnerId id) {
        ServerClaimRequest request = createServerClaimRequest(id);

        HttpEntity<ServerClaimRequest> requestEntity = createDefaultRequestEntity(request);

        return restTemplate.postForEntity("/claim/enrol/token", requestEntity, String.class);
    }

    /**
     * Enrols a user through a photo that is trusted.
     *
     * @param token An enrolment token value
     * @param photo Trusted photo of a person
     * @return Response entity with the result json
     */
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

    /**
     * Generates a token for initializing a person verification process
     *
     * @param id Owner identification.
     * @return Response entity with the result json
     */
    public ResponseEntity<String> generateVerificationToken(OwnerId id) {
        ServerClaimRequest request = createServerClaimRequest(id);

        HttpEntity<ServerClaimRequest> requestEntity = createDefaultRequestEntity(request);

        return restTemplate.postForEntity("/claim/verify/token", requestEntity, String.class);
    }

    /**
     * Validates the result of a person verification process
     *
     * @param id Owner identification.
     * @param token Token value used for initializing the person verification process
     * @return Response entity with the result json
     */
    public ResponseEntity<String> validateVerification(OwnerId id, String token) {
        ClaimValidateRequest request = new ClaimValidateRequest();
        request.setApiKey(configProps.getApiKey());
        request.setSecret(configProps.getApiSecret());
        request.setClient("Wultra Enrollment Server, activationId: " + id.getActivationId()); // TODO value from the device
        request.setIp("192.168.1.1"); // TODO deprecated but still required
        request.setRiskProfile(configProps.getRiskProfile());
        request.setToken(token);
        request.setUserId(id.getUserIdSecured());

        HttpEntity<ClaimValidateRequest> requestEntity = createDefaultRequestEntity(request);

        return restTemplate.postForEntity("/claim/verify/validate", requestEntity, String.class);
    }

    /**
     * Deletes user person data
     *
     * @param id Owner identification.
     * @return Response entity with the result json
     */
    public ResponseEntity<String> deleteUserPersona(OwnerId id) {
        // TODO implement this, oauth call on DELETE /users/activationId
        logger.warn("Not deleting user in iProov (not implemented yet), {}", id);
        return ResponseEntity.ok("{\n" +
                "  \"user_id\": \"" + id.getUserIdSecured() + "\",\n" +
                "  \"name\": \"user name\",\n" +
                "  \"status\": \"Deleted\"\n" +
                "}");
    }

    private ServerClaimRequest createServerClaimRequest(OwnerId id) {
        ServerClaimRequest request = new ServerClaimRequest();
        request.setApiKey(configProps.getApiKey());
        request.setSecret(configProps.getApiSecret());
        request.setAssuranceType(configProps.getAssuranceType());
        request.setResource(I_PROOV_RESOURCE_CONTEXT + id.getActivationId());
        request.setRiskProfile(configProps.getRiskProfile());
        request.setUserId(id.getUserIdSecured());
        return request;
    }

    private <T> HttpEntity<T> createDefaultRequestEntity(T entity) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(entity, headers);
    }

}
