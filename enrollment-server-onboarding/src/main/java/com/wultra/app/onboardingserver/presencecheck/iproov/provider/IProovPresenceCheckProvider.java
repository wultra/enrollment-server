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
package com.wultra.app.onboardingserver.presencecheck.iproov.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.presencecheck.iproov.model.api.*;
import com.wultra.app.onboardingserver.presencecheck.iproov.service.IProovRestApiService;
import com.wultra.app.onboardingserver.provider.PresenceCheckProvider;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Calendar;

/**
 * Implementation of the {@link PresenceCheckProvider} with <a href="https://www.iproov.com/">iProov</a>.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "iproov")
@Component
@Slf4j
public class IProovPresenceCheckProvider implements PresenceCheckProvider {

    /**
     * Session parameter name of the verification token
     */
    private static final String VERIFICATION_TOKEN = "iProovVerificationToken";

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
    public void initPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException, RemoteCommunicationException {
        ResponseEntity<String> responseEntityToken = callGenerateEnrolToken(id);
        // FIXME temporary solution of repeated presence check initialization
        // Deleting the iProov enrollment properly is not implemented yet, use random suffix to the current userId
        if (responseEntityToken.getBody() != null && responseEntityToken.getBody().contains("already_enrolled")) {
            logger.warn("Retrying the iProov enrollment with adapted userId");
            final OwnerId adaptedId = new OwnerId();
            adaptedId.setUserId(id.getUserId() + Calendar.getInstance().toInstant().getEpochSecond());
            adaptedId.setActivationId(id.getActivationId());
            adaptedId.setTimestamp(id.getTimestamp());
            id = adaptedId;
            responseEntityToken = callGenerateEnrolToken(id);
        }

        if (responseEntityToken.getBody() == null) {
            throw new RemoteCommunicationException("Missing response body when generating an enrol token in iProov, " + id);
        }

        if (!responseEntityToken.getStatusCode().is2xxSuccessful()) {
            throw new PresenceCheckException(
                    String.format("Failed to generate an enrol token, statusCode=%s, responseBody='%s', %s",
                            responseEntityToken.getStatusCode(), responseEntityToken.getBody(), id));
        }

        final ClaimResponse claimResponse = parseResponse(responseEntityToken.getBody(), ClaimResponse.class);
        final String token = claimResponse.getToken();

        final ResponseEntity<String> responseEntityEnrol;
        try {
            responseEntityEnrol = iProovRestApiService.enrolUserImageForToken(token, photo);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed to enrol a user image to iProov, statusCode=%s, responseBody='%s', %s",
                            responseEntityToken.getStatusCode(), responseEntityToken.getBody(), id),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when enrolling a user image to iProov, " + id, e);
        }

        if (responseEntityEnrol.getBody() == null) {
            throw new RemoteCommunicationException("Missing response body when enrolling a user image to iProov, " + id);
        }

        final EnrolResponse enrolResponse = parseResponse(responseEntityEnrol.getBody(), EnrolResponse.class);
        if (!enrolResponse.isSuccess()) {
            throw new PresenceCheckException("Not successful enrol of a user image to iProov, " + id);
        }
    }

    @Override
    public SessionInfo startPresenceCheck(OwnerId id) throws PresenceCheckException, RemoteCommunicationException {
        final ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.generateVerificationToken(id);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed to generate a verification token, statusCode=%s, responseBody='%s', %s",
                            e.getStatusCode(), e.getResponse(), id),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when generating a verification token in iProov, " + id, e);
        }

        if (responseEntity.getBody() == null) {
            throw new RemoteCommunicationException("Missing response body when generating a verification token in iProov, " + id);
        }

        final ClaimResponse claimResponse = parseResponse(responseEntity.getBody(), ClaimResponse.class);
        final String token = claimResponse.getToken();

        final SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.getSessionAttributes().put(VERIFICATION_TOKEN, token);

        return sessionInfo;
    }

    @Override
    public PresenceCheckResult getResult(OwnerId id, SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException {
        final String token = (String) sessionInfo.getSessionAttributes().get(VERIFICATION_TOKEN);
        if (Strings.isNullOrEmpty(token)) {
            throw new PresenceCheckException("Missing a token value for verification validation in iProov, " + id);
        }

        final ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.validateVerification(id, token);
        } catch (RestClientException e) {
            logger.warn("Failed REST call to validate a verification in iProov, statusCode={}, responseBody='{}', {}, {}",
                    e.getStatusCode(), e.getResponse(), id, e.getMessage());
            logger.debug("Failed REST call to validate a verification in iProov, statusCode={}, responseBody='{}', {}",
                    e.getStatusCode(), e.getResponse(), id, e);
            final PresenceCheckResult result = new PresenceCheckResult();
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                final ClientErrorResponse clientErrorResponse = parseResponse(e.getResponse(), ClientErrorResponse.class);
                if (ClientErrorResponse.ErrorEnum.INVALID_TOKEN.equals(clientErrorResponse.getError())) {
                    logger.warn("Invalid iProov token - reused token or validation called before verification, {}", id);
                }
            }
            result.setStatus(PresenceCheckStatus.FAILED);
            result.setErrorDetail(e.getResponse());
            return result;
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when validating a verification in iProov, " + id, e);
        }

        if (responseEntity == null || responseEntity.getBody() == null) {
            throw new RemoteCommunicationException("Missing response body when validating a verification in iProov, " + id);
        }

        final PresenceCheckResult result = new PresenceCheckResult();

        final ClaimValidateResponse response = parseResponse(responseEntity.getBody(), ClaimValidateResponse.class);
        if (response.isPassed()) {
            result.setStatus(PresenceCheckStatus.ACCEPTED);

            String frameJpeg = response.getFrame();
            frameJpeg = unescapeSlashes(frameJpeg);
            byte[] photoData = Base64.getDecoder().decode(frameJpeg);

            final Image photo = new Image();
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
    public void cleanupIdentityData(OwnerId id) throws PresenceCheckException, RemoteCommunicationException {
        final ResponseEntity<String> responseEntity;
        try {
            responseEntity = iProovRestApiService.deleteUserPersona(id);
        } catch (RestClientException e) {
            throw new RemoteCommunicationException(
                    String.format("Failed REST call to delete a user persona from iProov, statusCode=%s, responseBody='%s', %s",
                            e.getStatusCode(), e.getResponse(), id),
                    e);
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when deleting a user persona in iProov, " + id, e);
        }

        if (responseEntity.getBody() == null) {
            throw new RemoteCommunicationException("Missing response body when validating a verification in iProov, " + id);
        }

        UserResponse userResponse = parseResponse(responseEntity.getBody(), UserResponse.class);
        logger.info("Deleted a user persona in iProov, status={}, {}", userResponse.getStatus(), id);
    }

    private ResponseEntity<String> callGenerateEnrolToken(OwnerId id) throws RemoteCommunicationException {
        try {
            return iProovRestApiService.generateEnrolToken(id);
        } catch (RestClientException e) {
            if (e.getStatusCode().is4xxClientError()) {
                return new ResponseEntity<>(e.getResponse(), e.getStatusCode());
            } else {
                throw new RemoteCommunicationException("Failed REST call to generate an enrol token in iProov, " + id, e);
            }
        } catch (Exception e) {
            throw new RemoteCommunicationException("Unexpected error when generating an enrol token in iProov, " + id, e);
        }
    }

    private <T> T parseResponse(String body, Class<T> cls) throws PresenceCheckException {
        try {
            return objectMapper.readValue(body, cls);
        } catch (JsonProcessingException e) {
            throw new PresenceCheckException(String.format("Unable to parse JSON response %s to %s", body, cls), e);
        }
    }

    private String unescapeSlashes(String value) {
        return value.replace("\\", "");
    }

}
