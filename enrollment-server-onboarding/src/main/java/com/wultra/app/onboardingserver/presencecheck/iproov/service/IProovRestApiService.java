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
package com.wultra.app.onboardingserver.presencecheck.iproov.service;

import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.presencecheck.iproov.config.IProovConfigProps;
import com.wultra.app.onboardingserver.presencecheck.iproov.model.api.ClaimValidateRequest;
import com.wultra.app.onboardingserver.presencecheck.iproov.model.api.EnrolImageBody;
import com.wultra.app.onboardingserver.presencecheck.iproov.model.api.ServerClaimRequest;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
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

import java.util.regex.Pattern;

/**
 * Implementation of the REST service to <a href="https://www.iproov.com/">iProov</a>.
 *
 * <p>
 *     The userId is filled with a secured user identification
 * </p>
 * <ul>
 *     <li>optimizes API costs which are based on unique users</li>
 *     <li>hides potentially sensitive data from leaking at the external provider side</li>
 * </ul>
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "iproov")
@Service
@Slf4j
public class IProovRestApiService {

    private static final MultiValueMap<String, String> EMPTY_QUERY_PARAMS = new LinkedMultiValueMap<>();

    private static final ParameterizedTypeReference<String> STRING_TYPE_REFERENCE = new ParameterizedTypeReference<>() { };

    /**
     * Max length of the user id value defined by iProov
     */
    protected static final int USER_ID_MAX_LENGTH = 256;

    /**
     * Regex used by iProov on user id values
     */
    private static final Pattern USER_ID_REGEX_PATTERN = Pattern.compile("[a-zA-Z0-9'+_@.-]{1,256}");

    private static final String I_PROOV_RESOURCE_CONTEXT = "presence_check/";

    /**
     * Configuration properties.
     */
    private final IProovConfigProps configProps;

    /**
     * REST client for iProov calls.
     */
    private final RestClient restClient;

    /**
     * Service constructor.
     *
     * @param configProps Configuration properties.
     * @param restClient REST template for IProov calls.
     */
    @Autowired
    public IProovRestApiService(
            IProovConfigProps configProps,
            @Qualifier("restClientIProov") RestClient restClient) {
        this.configProps = configProps;
        this.restClient = restClient;
    }

    /**
     * Generates an enrolment token for a new user to enrol the service
     *
     * @param id Owner identification.
     * @return Response entity with the result json
     */
    public ResponseEntity<String> generateEnrolToken(OwnerId id) throws RestClientException {
        final ServerClaimRequest request = createServerClaimRequest(id);

        logger.debug("Calling /claim/enrol/token userId={}, {}", request.getUserId(), id);
        return restClient.post("/claim/enrol/token", request, STRING_TYPE_REFERENCE);
    }

    /**
     * Enrols a user through a photo that is trusted.
     *
     * @param token An enrolment token value
     * @param photo Trusted photo of a person
     * @param id Owner identification.
     * @return Response entity with the result json
     */
    public ResponseEntity<String> enrolUserImageForToken(String token, Image photo, OwnerId id) throws RestClientException {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("api_key", configProps.getApiKey());
        bodyBuilder.part("secret", configProps.getApiSecret());
        bodyBuilder.part("rotation", 0);
        bodyBuilder.part("source", EnrolImageBody.SourceEnum.OID.toString());
        bodyBuilder.part("image", new ByteArrayResource(photo.getData()) {

            @Override
            public String getFilename() {
                return photo.getFilename();
            }

        });
        bodyBuilder.part("token", token);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        logger.debug("Calling /claim/enroll/image token={}, {}", token, id);
        return restClient.post("/claim/enrol/image", bodyBuilder.build(), EMPTY_QUERY_PARAMS, httpHeaders, STRING_TYPE_REFERENCE);
    }

    /**
     * Generates a token for initializing a person verification process
     *
     * @param id Owner identification.
     * @return Response entity with the result json
     */
    public ResponseEntity<String> generateVerificationToken(OwnerId id) throws RestClientException {
        final ServerClaimRequest request = createServerClaimRequest(id);

        logger.debug("Calling /claim/verify/token userId={}, {}", request.getUserId(), id);
        return restClient.post("/claim/verify/token", request, STRING_TYPE_REFERENCE);
    }

    /**
     * Validates the result of a person verification process
     *
     * @param id Owner identification.
     * @param token Token value used for initializing the person verification process
     * @return Response entity with the result json
     */
    public ResponseEntity<String> validateVerification(OwnerId id, String token) throws RestClientException {
        final ClaimValidateRequest request = new ClaimValidateRequest();
        request.setApiKey(configProps.getApiKey());
        request.setSecret(configProps.getApiSecret());
        request.setClient("Wultra Enrollment Server, activationId: " + id.getActivationId()); // TODO value from the device
        request.setIp("192.168.1.1"); // TODO deprecated but still required
        request.setRiskProfile(configProps.getRiskProfile());
        request.setToken(token);

        final String userId = getUserId(id);
        request.setUserId(userId);

        logger.debug("Calling /claim/verify/validate userId={}, token={}, {}", userId, token, id);
        return restClient.post("/claim/verify/validate", request, STRING_TYPE_REFERENCE);
    }

    private ServerClaimRequest createServerClaimRequest(OwnerId id) {
        final ServerClaimRequest request = new ServerClaimRequest();
        request.setApiKey(configProps.getApiKey());
        request.setSecret(configProps.getApiSecret());
        request.setAssuranceType(configProps.getAssuranceType());
        request.setResource(I_PROOV_RESOURCE_CONTEXT + id.getActivationId());
        request.setRiskProfile(configProps.getRiskProfile());

        final String userId = getUserId(id);
        request.setUserId(userId);

        return request;
    }

    public static String ensureValidUserIdValue(String value) {
        if (value.length() > USER_ID_MAX_LENGTH) {
            value = value.substring(0, USER_ID_MAX_LENGTH);
            logger.warn("The userId value: '{}', was too long for iProov, shortened to {} characters", value, USER_ID_MAX_LENGTH);
        }
        if (!USER_ID_REGEX_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(String.format("The userId value: '%s', does not match the iProov regex pattern", value));
        }
        return value;
    }

    private String getUserId(OwnerId id) {
        if (configProps.isEnsureUserIdValueEnabled()) {
            return ensureValidUserIdValue(id.getUserIdSecured());
        } else {
            return id.getUserIdSecured();
        }
    }

}
