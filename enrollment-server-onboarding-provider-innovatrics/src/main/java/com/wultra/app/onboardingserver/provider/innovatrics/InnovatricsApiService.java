/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2023 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.innovatrics;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CustomerInspectResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.EvaluateCustomerLivenessRequest;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.EvaluateCustomerLivenessResponse;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Implementation of the REST service to<a href="https://www.innovatrics.com/">Innovatrics</a>.
 * <p>
 * It is not possible to combine Innovatrics with other providers such as iProov or ZenID.
 * Both providers, document verifier and presence check, must be configured to {@code innovatrics}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
        """)
@Service
@Slf4j
class InnovatricsApiService {

    private static final MultiValueMap<String, String> EMPTY_ADDITIONAL_HEADERS = new LinkedMultiValueMap<>();

    private static final MultiValueMap<String, String> EMPTY_QUERY_PARAMS = new LinkedMultiValueMap<>();

    /**
     * REST client for Innovatrics calls.
     */
    private final RestClient restClient;

    /**
     * Service constructor.
     *
     * @param restClient REST template for Innovatrics calls.
     */
    @Autowired
    public InnovatricsApiService(@Qualifier("restClientInnovatrics") final RestClient restClient) {
        this.restClient = restClient;
    }

    public EvaluateCustomerLivenessResponse evaluateLiveness(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final EvaluateCustomerLivenessRequest request = new EvaluateCustomerLivenessRequest();
        request.setType(EvaluateCustomerLivenessRequest.TypeEnum.MAGNIFEYE_LIVENESS);

        final String apiPath = "/api/v1/customers/%s/liveness/evaluation".formatted(customerId);

        try {
            logger.info("Calling liveness/evaluation, {}", ownerId);
            logger.debug("Calling {}, {}", apiPath, request);
            final ResponseEntity<EvaluateCustomerLivenessResponse> response = restClient.post(apiPath, request, EMPTY_QUERY_PARAMS, EMPTY_ADDITIONAL_HEADERS, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for liveness/evaluation, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to evaluate liveness for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when evaluating liveness for customerId=" + customerId, e);
        }
    }

    public CustomerInspectResponse inspectCustomer(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/inspect".formatted(customerId);

        try {
            logger.info("Calling /inspect, {}", ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<CustomerInspectResponse> response = restClient.post(apiPath, null, EMPTY_QUERY_PARAMS, EMPTY_ADDITIONAL_HEADERS, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for /inspect, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to evaluate inspect for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when evaluating inspect for customerId=" + customerId, e);
        }
    }

    public void deleteLiveness(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/liveness".formatted(customerId);

        try {
            logger.info("Deleting liveness, {}", ownerId);
            logger.debug("Deleting {}", apiPath);
            final ResponseEntity<Void> response = restClient.delete(apiPath, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for liveness delete, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to delete liveness for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when deleting liveness for customerId=" + customerId, e);
        }
    }

    public void deleteSelfie(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/selfie".formatted(customerId);

        try {
            logger.info("Deleting selfie, {}", ownerId);
            logger.debug("Deleting {}", apiPath);
            final ResponseEntity<Void> response = restClient.delete(apiPath, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for selfie delete, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to delete selfie for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when deleting selfie for customerId=" + customerId, e);
        }
    }

}
