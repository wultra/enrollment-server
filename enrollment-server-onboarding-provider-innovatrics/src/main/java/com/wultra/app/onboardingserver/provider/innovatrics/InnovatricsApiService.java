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
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CustomerInspectResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.EvaluateCustomerLivenessRequest;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.EvaluateCustomerLivenessResponse;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the REST service to<a href="https://www.innovatrics.com/">Innovatrics</a>.
 * <p>
 * It is not possible to combine Innovatrics with other providers such as iProov or ZenID.
 * Both providers, document verifier and presence check, must be configured to {@code innovatrics}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
        """)
@Service
@Slf4j
class InnovatricsApiService {

    private static final MultiValueMap<String, String> EMPTY_QUERY_PARAMS = new LinkedMultiValueMap<>();

    /**
     * REST client for Innovatrics calls.
     */
    private final RestClient restClient;

    /**
     * Configuration properties.
     */
    private final InnovatricsConfigProps configProps;

    /**
     * Service constructor.
     *
     * @param restClient REST template for Innovatrics calls.
     */
    @Autowired
    public InnovatricsApiService(@Qualifier("restClientInnovatrics") final RestClient restClient,
                                 InnovatricsConfigProps configProps) {
        this.restClient = restClient;
        this.configProps = configProps;
    }

    public EvaluateCustomerLivenessResponse evaluateLiveness(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final EvaluateCustomerLivenessRequest request = new EvaluateCustomerLivenessRequest();
        request.setType(EvaluateCustomerLivenessRequest.TypeEnum.MAGNIFEYE_LIVENESS);

        final String apiPath = "/api/v1/customers/%s/liveness/evaluation".formatted(customerId);

        try {
            logger.info("Calling liveness/evaluation, {}", ownerId);
            logger.debug("Calling {}, {}", apiPath, request);
            final ResponseEntity<EvaluateCustomerLivenessResponse> response = restClient.post(apiPath, request, new ParameterizedTypeReference<>() {});
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
            final ResponseEntity<CustomerInspectResponse> response = restClient.post(apiPath, null, new ParameterizedTypeReference<>() {});
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

    public void createLiveness(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/liveness".formatted(customerId);

        try {
            logger.info("Calling liveness creation, {}", ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<CreateCustomerLivenessResponse> response = restClient.put(apiPath, null, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for liveness creation, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to liveness creation for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when creating liveness for customerId=" + customerId, e);
        }
    }

    public CreateCustomerLivenessRecordResponse createLivenessRecord(final String customerId, final byte[] requestData, final OwnerId ownerId) throws RemoteCommunicationException{
        final String apiPath = "/api/v1/customers/%s/liveness/records".formatted(customerId);

        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        try {
            logger.info("Calling liveness record creation, {}", ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<CreateCustomerLivenessRecordResponse> response = restClient.post(apiPath, requestData, EMPTY_QUERY_PARAMS, httpHeaders, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for liveness record creation, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to liveness record creation for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when creating liveness record for customerId=" + customerId, e);
        }
    }

    public CreateSelfieResponse createSelfie(final String customerId, final String livenessSelfieLink, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/selfie".formatted(customerId);

        final CreateSelfieRequest request = new CreateSelfieRequest().selfieOrigin(new LivenessSelfieOrigin().link(livenessSelfieLink));

        try {
            logger.info("Calling selfie creation, {}", ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<CreateSelfieResponse> response = restClient.put(apiPath, request, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for selfie creation, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to selfie creation for customerId=%s, statusCode=%s, responseBody='%s'", customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when creating selfie for customerId=" + customerId, e);
        }
    }

    /**
     * Create a new customer resource.
     * @param ownerId owner identification.
     * @return optional of CreateCustomerResponse with a customerId.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public CreateCustomerResponse createCustomer(final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers";

        try {
            logger.info("Creating customer, {}", ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<CreateCustomerResponse> response = restClient.post(apiPath, null, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for creating customer, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed when creating a new customer resource, statusCode=%s, responseBody='%s'".formatted(e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when creating a new customer resource", e);
        }
    }

    /**
     * Create a new document resource assigned to a customer. This resource is used for documents only, not for selfies.
     * @param customerId id of the customer to assign the resource to.
     * @param documentType type of document that will be uploaded later.
     * @param ownerId owner identification.
     * @return optional of CreateDocumentResponse. Does not contain important details.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public CreateDocumentResponse createDocument(final String customerId, final DocumentType documentType, final OwnerId ownerId) throws RemoteCommunicationException, DocumentVerificationException {
        final String apiPath = "/api/v1/customers/%s/document".formatted(customerId);

        final DocumentClassificationAdvice classificationAdvice = new DocumentClassificationAdvice();
        classificationAdvice.setTypes(List.of(convertType(documentType)));
        classificationAdvice.setCountries(configProps.getDocumentVerificationConfiguration().getDocumentCountries());
        final DocumentAdvice advice = new DocumentAdvice();
        advice.setClassification(classificationAdvice);
        final CreateDocumentRequest request = new CreateDocumentRequest();
        request.setAdvice(advice);

        try {
            logger.info("Creating new document of type {} for customer {}, {}", documentType, customerId, ownerId);
            logger.debug("Calling {}, {}", apiPath, request);
            final ResponseEntity<CreateDocumentResponse> response = restClient.put(apiPath, request, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for creating document, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed when creating a new document resource for customerId=%s, statusCode=%s, responseBody='%s'".formatted(customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when creating a new document resource for customerId=%s".formatted(customerId), e);
        }
    }

    /**
     * Provide photo of a document page. A document resource must be already assigned to the customer.
     * @param customerId id of the customer to whom the document should be provided.
     * @param side specifies side of the document.
     * @param imageBytes image of the page encoded in base64.
     * @param ownerId owner identification.
     * @return optional of CreateDocumentPageResponse with details extracted from the page.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public CreateDocumentPageResponse provideDocumentPage(final String customerId, final CardSide side, final byte[] imageBytes, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/document/pages".formatted(customerId);

        final DocumentPageClassificationAdvice classificationAdvice = new DocumentPageClassificationAdvice();
        classificationAdvice.setPageTypes(List.of(convertSide(side)));
        final DocumentPageAdvice advice = new DocumentPageAdvice();
        advice.setClassification(classificationAdvice);

        final Image image = new Image();
        image.setData(imageBytes);

        final CreateDocumentPageRequest request = new CreateDocumentPageRequest();
        request.setAdvice(advice);
        request.setImage(image);

        try {
            logger.info("Providing {} side document page for customer {}, {}", convertSide(side), customerId, ownerId);
            logger.debug("Calling {}, {}", apiPath, request);
            final ResponseEntity<CreateDocumentPageResponse> response = restClient.put(apiPath, request, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for providing document page, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed when providing a document page for customerId=%s, statusCode=%s, responseBody='%s'".formatted(customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when providing a document page for customerId=%s".formatted(customerId), e);
        }
    }

    /**
     * Get details gathered about the customer.
     * @param customerId id of the customer.
     * @param ownerId owner identification.
     * @return optional of GetCustomerResponse with details about the customer.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public GetCustomerResponse getCustomer(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s".formatted(customerId);

        try {
            logger.info("Getting details about customer {}, {}", customerId, ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<GetCustomerResponse> response = restClient.get(apiPath, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for getting details about customer, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed when getting details of customerId=%s, statusCode=%s, responseBody='%s'".formatted(customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when getting details of customerId=%s".formatted(customerId), e);
        }
    }

    /**
     * Get document portrait of the customer.
     * @param customerId id of the customer.
     * @param ownerId owner identification.
     * @return successful Response contains a base64 in the JPG format.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public Optional<ImageCrop> getDocumentPortrait(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/document/portrait".formatted(customerId);

        try {
            logger.info("Getting document portrait of customer {}, {}", customerId, ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<ImageCrop> response = restClient.get(apiPath, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for getting document portrait, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                // API returns 404 in case of missing portrait photo.
                logger.debug("Missing portrait photo for customer {}, {}", customerId, ownerId);
                return Optional.empty();
            }
            throw new RemoteCommunicationException("REST API call failed when getting customer portrait, statusCode=%s, responseBody='%s'".formatted(e.getStatusCode(), e.getResponse()), e);
        }
    }

    /**
     * Inspect consistency of data of the submitted document provided for a customer.
     * @param customerId id of the customer whose document to inspect.
     * @param ownerId owner identification.
     * @return optional of DocumentInspectResponse with details about consistency of the document.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public DocumentInspectResponse inspectDocument(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/document/inspect".formatted(customerId);

        try {
            logger.info("Getting document inspect of customer {}, {}", customerId, ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<DocumentInspectResponse> response = restClient.post(apiPath, null, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for getting document inspect, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed while getting document inspection for customerId=%s, statusCode=%s, responseBody='%s'".formatted(customerId, e.getStatusCode(), e.getResponse()), e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when getting document inspection for customerId=%s".formatted(customerId), e);
        }
    }

    /**
     * Delete customer.
     * @param customerId id of the customer.
     * @param ownerId owner identification.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public void deleteCustomer(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s".formatted(customerId);

        try {
            logger.info("Deleting customer {}, {}", customerId, ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<Void> response = restClient.delete(apiPath, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for deleting customer, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed when deleting customer, statusCode=%s, responseBody='%s'".formatted(e.getStatusCode(), e.getResponse()), e);
        }
    }

    /**
     * Delete customer's document.
     * @param customerId id of the customer.
     * @param ownerId owner identification.
     * @throws RemoteCommunicationException in case of 4xx or 5xx response status code.
     */
    public void deleteDocument(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        final String apiPath = "/api/v1/customers/%s/document".formatted(customerId);

        try {
            logger.info("Deleting document of customer {}, {}", customerId, ownerId);
            logger.debug("Calling {}", apiPath);
            final ResponseEntity<Void> response = restClient.delete(apiPath, new ParameterizedTypeReference<>() {});
            logger.info("Got {} for deleting customer's document, {}", response.getStatusCode(), ownerId);
            logger.debug("{} response status code: {}", apiPath, response.getStatusCode());
            logger.trace("{} response: {}", apiPath, response);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException("REST API call failed when deleting customer's document, statusCode=%s, responseBody='%s'".formatted(e.getStatusCode(), e.getResponse()), e);
        }
    }

    /**
     * Converts internal DocumentType enum to string value used by Innovatrics.
     * @param type represents type of document.
     * @return document type as a string value.
     */
    private static String convertType(DocumentType type) throws DocumentVerificationException {
        return switch (type) {
            case ID_CARD -> "identity-card";
            case PASSPORT -> "passport";
            case DRIVING_LICENSE -> "drivers-licence";
            default -> throw new DocumentVerificationException("Unsupported documentType " + type);
        };
    }

    /**
     * Converts internal CardSide enum to string value used by Innovatrics.
     * @param side represents side of a card.
     * @return side of a card as a string value.
     */
     private static String convertSide(CardSide side) {
        return switch (side) {
            case FRONT -> "front";
            case BACK -> "back";
        };
    }

}
