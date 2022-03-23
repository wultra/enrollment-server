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
import com.wultra.core.rest.client.base.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Calendar;

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
            @Qualifier("objectMapperIproov")
            ObjectMapper objectMapper,
            IProovRestApiService iProovRestApiService) {
        this.objectMapper = objectMapper;
        this.iProovRestApiService = iProovRestApiService;
    }

    @Override
    public void initPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException {
        ResponseEntity<String> responseEntityToken = callGenerateEnrolToken(id);
        // FIXME temporary solution of repeated presence check initialization
        // Deleting the iProov enrollment properly is not implemented yet, use random suffix to the current userId
        if (responseEntityToken.getBody() != null && responseEntityToken.getBody().contains("already_enrolled")) {
            logger.warn("Retrying the iProov enrollment with adapted userId");
            OwnerId adaptedId = new OwnerId();
            adaptedId.setUserId(id.getUserId() + Calendar.getInstance().toInstant().getEpochSecond());
            adaptedId.setActivationId(id.getActivationId());
            adaptedId.setTimestamp(id.getTimestamp());
            id = adaptedId;
            responseEntityToken = callGenerateEnrolToken(id);
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
        } catch (RestClientException e) {
            logger.error("Failed to enrol a user image to iProov, statusCode={}, responseBody='{}', {}",
                    responseEntityToken.getStatusCode(), responseEntityToken.getBody(), id);
            throw new PresenceCheckException("Unable to init a presence check due to a service error");
        } catch (Exception e) {
            logger.error("Unexpected error when enrolling a user image to iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when initializing a presence check");
        }

        if (responseEntityEnrol.getBody() == null) {
            logger.error("Missing response body when enrolling a user image to iProov, " + id);
            throw new PresenceCheckException("Unexpected error when initializing a presence check");
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
        } catch (RestClientException e) {
            logger.error("Failed to generate a verification token, statusCode={}, responseBody='{}', {}",
                    e.getStatusCode(), e.getResponse(), id);
            throw new PresenceCheckException("Unable to start a presence check due to a service error");
        } catch (Exception e) {
            logger.error("Unexpected error when generating a verification token in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when starting a presence check");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when generating a verification token in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when generating a verification token in iProov, " + id);
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

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = iProovRestApiService.validateVerification(id, token);
        } catch (RestClientException e) {
            logger.warn(
                    String.format("Failed REST call to validate a verification in iProov, statusCode=%s, responseBody='%s', %s",
                            e.getStatusCode(),
                            e.getResponse(),
                            id),
                    e
            );
            PresenceCheckResult result = new PresenceCheckResult();
            if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
                ClientErrorResponse clientErrorResponse = parseResponse(e.getResponse(), ClientErrorResponse.class);
                if (ClientErrorResponse.ErrorEnum.INVALID_TOKEN.equals(clientErrorResponse.getError())) {
                    // TODO same response when validating the verification using same token repeatedly
                    result.setStatus(PresenceCheckStatus.IN_PROGRESS);
                } else {
                    result.setStatus(PresenceCheckStatus.FAILED);
                    result.setErrorDetail(e.getResponse());
                }
            } else {
                result.setStatus(PresenceCheckStatus.FAILED);
                result.setErrorDetail(e.getResponse());
            }
            return result;
        } catch (Exception e) {
            logger.error("Unexpected error when validating a verification in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when getting a presence check result");
        }

        if (responseEntity == null || responseEntity.getBody() == null) {
            logger.error("Missing response body when validating a verification in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when getting a presence check result");
        }

        PresenceCheckResult result = new PresenceCheckResult();

        ClaimValidateResponse response = parseResponse(responseEntity.getBody(), ClaimValidateResponse.class);
        if (response.isPassed()) {
            result.setStatus(PresenceCheckStatus.ACCEPTED);

            String frameJpeg = response.getFrame();
            frameJpeg = unescapeSlashes(frameJpeg);
            byte[] photoData = Base64.getDecoder().decode(frameJpeg);

            Image photo = new Image();
            photo.setFilename("person_photo_from_id.jpg");
            photo.setData(photoData);

            result.setPhoto(photo);
        } else {
            result.setStatus(PresenceCheckStatus.REJECTED);
            result.setRejectReason(response.getReason());
        }
        return result;
    }

    @Override
    public void cleanupIdentityData(OwnerId id) throws PresenceCheckException {
        ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.deleteUserPersona(id);
        } catch (RestClientException e) {
            logger.warn(
                    String.format("Failed REST call to delete a user persona from iProov, statusCode=%s, responseBody='%s', %s",
                            e.getStatusCode(),
                            e.getResponse(),
                            id),
                    e
            );
            throw new PresenceCheckException("Unable to cleanup identity data");
        } catch (Exception e) {
            logger.error("Unexpected error when deleting a user persona in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when cleaning up identity data");
        }

        if (responseEntity.getBody() == null) {
            logger.error("Missing response body when validating a verification in iProov, " + id);
            throw new PresenceCheckException("Unexpected error when cleaning up identity data");
        }

        UserResponse userResponse = parseResponse(responseEntity.getBody(), UserResponse.class);
        logger.info("Deleted a user persona in iProov, status={}, {}", userResponse.getStatus(), id);
    }

    private ResponseEntity<String> callGenerateEnrolToken(OwnerId id) throws PresenceCheckException {
        ResponseEntity<String> responseEntityToken;
        try {
            responseEntityToken = iProovRestApiService.generateEnrolToken(id);
        } catch (RestClientException e) {
            if (e.getStatusCode().is4xxClientError()) {
                responseEntityToken = new ResponseEntity<>(e.getResponse(), e.getStatusCode());
            } else {
                logger.warn("Failed REST call to generate an enrol token in iProov, " + id, e);
                throw new PresenceCheckException("Unable to init a presence check due to a REST call failure");
            }
        } catch (Exception e) {
            logger.error("Unexpected error when generating an enrol token in iProov, " + id, e);
            throw new PresenceCheckException("Unexpected error when initializing a presence check");
        }
        return responseEntityToken;
    }

    private <T> T parseResponse(String body, Class<T> cls) throws PresenceCheckException {
        try {
            return objectMapper.readValue(body, cls);
        } catch (JsonProcessingException e) {
            logger.error("Unable to parse JSON response {} to {}", body, cls);
            throw new PresenceCheckException("Unable to process a response from the REST service");
        }
    }

    private String unescapeSlashes(String value) {
        return value.replace("\\", "");
    }

}
