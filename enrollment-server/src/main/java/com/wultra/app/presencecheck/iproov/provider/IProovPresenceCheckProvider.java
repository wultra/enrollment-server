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
package com.wultra.app.presencecheck.iproov.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.enrollmentserver.provider.PresenceCheckProvider;
import com.wultra.app.presencecheck.iproov.IProovConst;
import com.wultra.app.presencecheck.iproov.model.api.*;
import com.wultra.app.presencecheck.iproov.service.IProovRestApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Base64;

/**
 * Implementation of the {@link PresenceCheckProvider} with iProov (https://www.iproov.com/)
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.presence-check.provider", havingValue = "iproov")
@Component
public class IProovPresenceCheckProvider implements PresenceCheckProvider {

    private static final Logger logger = LoggerFactory.getLogger(IProovPresenceCheckProvider.class);

    private final ObjectMapper objectMapper;

    private final IProovRestApiService iProovRestApiService;

    /**
     * Service constructor.
     * @param objectMapper Object mapper.
     * @param iProovRestApiService REST API service for iProov calls.
     */
    @Autowired
    public IProovPresenceCheckProvider(
            ObjectMapper objectMapper,
            IProovRestApiService iProovRestApiService) {
        this.objectMapper = objectMapper;
        this.iProovRestApiService = iProovRestApiService;
    }

    @Override
    public void initPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException {
        ResponseEntity<String> responseEntityToken;
        try {
            responseEntityToken = iProovRestApiService.generateEnrolToken(id);
        } catch (HttpClientErrorException e) {
            responseEntityToken = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            logger.warn("Failed REST call to generate an enrol token in iProov, " + id, e);
            throw new PresenceCheckException("Unable to init a presence check due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when generating an enrol token in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when initializing a presence check");
        }

        if (responseEntityToken.getBody() == null) {
            logger.error("Missing response body when generating an enrol token in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when generating an enrol token in iProov, " + id);
        }

        if (!HttpStatus.OK.equals(responseEntityToken.getStatusCode())) {
            logger.error("Failed to generate an enrol token, statusCode={}, responseBody='{}', {}",
                    responseEntityToken.getStatusCode(), responseEntityToken.getBody(), id);
            throw new PresenceCheckException("Unable to init a presence check due to a service error");
        }

        ClaimResponse claimResponse = parseResponse(responseEntityToken.getBody(), ClaimResponse.class);
        String token = claimResponse.getToken();

        ResponseEntity<String> responseEntityEnrol;
        try {
            responseEntityEnrol = iProovRestApiService.enrolUserImageForToken(token, photo);
        } catch (HttpClientErrorException e) {
            responseEntityEnrol = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            logger.warn("Failed REST call to enrol a user image for the enrol token to iProov, " + id, e);
            throw new PresenceCheckException("Unable to init a presence check due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when enrolling a user image to iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when initializing a presence check");
        }

        if (responseEntityEnrol.getBody() == null) {
            logger.error("Missing response body when enrolling a user image to iProov, " + id);
            throw new PresenceCheckException("Unexpected error when initializing a presence check");
        }

        if (!HttpStatus.OK.equals(responseEntityToken.getStatusCode())) {
            logger.error("Failed to enrol a user image to iProov, statusCode={}, responseBody='{}', {}",
                    responseEntityToken.getStatusCode(), responseEntityToken.getBody(), id);
            throw new PresenceCheckException("Unable to init a presence check due to a service error");
        }

        EnrolResponse enrolResponse = parseResponse(responseEntityEnrol.getBody(), EnrolResponse.class);
        if (!enrolResponse.isSuccess()) {
            logger.error("Not successful enrol of a user image to iProov, " + id);
            throw new PresenceCheckException("Unable to init a presence check");
        }
    }

    @Override
    public SessionInfo startPresenceCheck(OwnerId id) throws PresenceCheckException {
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.generateVerificationToken(id);
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            logger.warn("Failed REST call to generate a verification token in iProov, " + id, e);
            throw new PresenceCheckException("Unable to start a presence check due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when generating a verification token in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when starting a presence check");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when generating a verification token in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when generating a verification token in iProov, " + id);
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            logger.error("Failed to generate a verification token, statusCode={}, responseBody='{}', {}",
                    responseEntity.getStatusCode(), responseEntity.getBody(), id);
            throw new PresenceCheckException("Unable to start a presence check due to a service error");
        }

        ClaimResponse claimResponse = parseResponse(responseEntity.getBody(), ClaimResponse.class);
        String token = claimResponse.getToken();

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.getSessionAttributes().put(IProovConst.VERIFICATION_TOKEN, token);

        return sessionInfo;
    }

    @Override
    public PresenceCheckResult getResult(OwnerId id, SessionInfo sessionInfo) throws PresenceCheckException {
        String token = (String) sessionInfo.getSessionAttributes().get(IProovConst.VERIFICATION_TOKEN);
        if (Strings.isNullOrEmpty(token)) {
            throw new PresenceCheckException("Missing a token value for verification validation in iProov, " + id);
        }

        ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.validateVerification(id, token);
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            logger.warn("Failed REST call to validate a verification in iProov, " + id, e);
            throw new PresenceCheckException("Unable to get a presence check result due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when validating a verification in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when getting a presence check result");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when validating a verification in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when getting a presence check result");
        }

        boolean failed = false;
        PresenceCheckResult result = new PresenceCheckResult();
        if (HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            ClaimValidateResponse response = parseResponse(responseEntity.getBody(), ClaimValidateResponse.class);

            if (response.isPassed()) {
                result.setStatus(PresenceCheckStatus.ACCEPTED);

                String frameJpeg = response.getFrameJpeg();
                byte[] photoData = Base64.getDecoder().decode(frameJpeg);

                Image photo = new Image();
                photo.setFilename("person_photo_from_id.jpg");
                photo.setData(photoData);

                result.setPhoto(photo);
            } else {
                result.setStatus(PresenceCheckStatus.REJECTED);
                result.setRejectReason(response.getReason());
            }
        } else if (HttpStatus.BAD_REQUEST.equals(responseEntity.getStatusCode())) {
            ClientErrorResponse clientErrorResponse = parseResponse(responseEntity.getBody(), ClientErrorResponse.class);
            if (ClientErrorResponse.ErrorEnum.INVALID_TOKEN.equals(clientErrorResponse.getError())) {
                // TODO same response when validating the verification using same token repeatedly
                result.setStatus(PresenceCheckStatus.IN_PROGRESS);
            } else {
                failed = true;
            }
        } else {
            failed = true;
        }

        if (failed) {
            logger.error("Failed to validate a verification in iProov, statusCode={}, responseBody='{}', {}",
                    responseEntity.getStatusCode(), responseEntity.getBody(), id);
            result.setStatus(PresenceCheckStatus.FAILED);
            result.setErrorDetail(responseEntity.getBody());
        }

        return result;
    }

    @Override
    public void cleanupIdentityData(OwnerId id) throws PresenceCheckException {
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.deleteUserPersona(id);
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            logger.warn("Failed REST call to delete a user persona from iProov, " + id, e);
            throw new PresenceCheckException("Unable to cleanup identity data due to a REST call failure");
        } catch (Exception e) {
            logger.error("Unexpected error when deleting a user persona in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when cleaning up identity data");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when validating a verification in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when cleaning up identity data");
        }

        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            logger.error("Failed to delete a user persona in iProov, statusCode={}, responseBody='{}', {}",
                    responseEntity.getStatusCode(), responseEntity.getBody(), id);
            throw new PresenceCheckException("Unable to cleanup identity data");
        }

        UserResponse userResponse = parseResponse(responseEntity.getBody(), UserResponse.class);
        logger.info("Deleted a user persona in iProov, status={}, {}", userResponse.getStatus(), id);
    }

    private <T> T parseResponse(String body, Class<T> cls) throws PresenceCheckException {
        try {
            return objectMapper.readValue(body, cls);
        } catch (JsonProcessingException e) {
            logger.error("Unable to parse JSON response {} to {}", body, cls);
            throw new PresenceCheckException("Unable to process a response from the REST service");
        }
    }

}
