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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.common.database.IdentityVerificationRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CreateCustomerLivenessRecordResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CreateSelfieResponse;
import io.getlime.security.powerauth.rest.api.spring.encryption.EncryptionContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service providing Innovatrics business features beyond {@link InnovatricsPresenceCheckProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@AllArgsConstructor
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and ${enrollment-server-onboarding.onboarding-process.enabled} == true
        """)
class InnovatricsLivenessService {

    private final InnovatricsApiService innovatricsApiService;

    private final IdentityVerificationRepository identityVerificationRepository;

    private AuditService auditService;

    public void upload(final byte[] requestData, final EncryptionContext encryptionContext) throws IdentityVerificationException, RemoteCommunicationException {
        final String activationId = encryptionContext.getActivationId();
        final IdentityVerificationEntity identityVerification = identityVerificationRepository.findFirstByActivationIdOrderByTimestampCreatedDesc(activationId).orElseThrow(() ->
                new IdentityVerificationException("No identity verification entity found for Activation ID: " + activationId));

        final OwnerId ownerId = extractOwnerId(identityVerification);
        final String customerId = fetchCustomerId(ownerId, identityVerification);

        createLiveness(customerId, ownerId);
        final CreateCustomerLivenessRecordResponse livenessRecordResponse = createLivenessRecord(requestData, customerId, ownerId);
        createSelfie(livenessRecordResponse, customerId, ownerId);

        auditService.auditPresenceCheckProvider(identityVerification, "Uploaded presence check data for user: {}", ownerId.getUserId());
        logger.info("Liveness record successfully uploaded, {}", ownerId);
    }

    private void createSelfie(final CreateCustomerLivenessRecordResponse livenessRecordResponse, final String customerId, final OwnerId ownerId) throws IdentityVerificationException, RemoteCommunicationException {
        final String livenessSelfieLink = fetchSelfieLink(livenessRecordResponse);
        final CreateSelfieResponse createSelfieResponse = innovatricsApiService.createSelfie(customerId, livenessSelfieLink, ownerId);
        if (createSelfieResponse.getErrorCode() != null) {
            logger.warn("Customer selfie error: {}, {}", createSelfieResponse.getErrorCode(), ownerId);
        }
        if (createSelfieResponse.getWarnings() != null) {
            for (CreateSelfieResponse.WarningsEnum warning : createSelfieResponse.getWarnings()) {
                logger.warn("Customer selfie warning: {}, {}", warning.getValue(), ownerId);
            }
        }
        logger.debug("Selfie created, {}", ownerId);
    }

    private CreateCustomerLivenessRecordResponse createLivenessRecord(final byte[] requestData, final String customerId, final OwnerId ownerId) throws RemoteCommunicationException, IdentityVerificationException {
        final CreateCustomerLivenessRecordResponse livenessRecordResponse = innovatricsApiService.createLivenessRecord(customerId, requestData, ownerId);
        if (livenessRecordResponse.getErrorCode() != null) {
            throw new IdentityVerificationException("Unable to create liveness record: " + livenessRecordResponse.getErrorCode());
        }
        logger.debug("Liveness record created, {}", ownerId);
        return livenessRecordResponse;
    }

    private void createLiveness(final String customerId, final OwnerId ownerId) throws RemoteCommunicationException {
        innovatricsApiService.createLiveness(customerId, ownerId);
        logger.debug("Liveness created, {}", ownerId);
    }

    private static String fetchSelfieLink(final CreateCustomerLivenessRecordResponse livenessRecordResponse) throws IdentityVerificationException {
        if (livenessRecordResponse.getLinks() == null) {
            throw new IdentityVerificationException("Unable to get selfie link");
        }
        return livenessRecordResponse.getLinks().getSelfie();
    }

    private static OwnerId extractOwnerId(final IdentityVerificationEntity identityVerification) {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId(identityVerification.getActivationId());
        ownerId.setUserId(identityVerification.getUserId());
        return ownerId;
    }

    private static String fetchCustomerId(final OwnerId id, final IdentityVerificationEntity identityVerification) throws IdentityVerificationException {
        final String sessionInfoString = StringUtils.defaultIfEmpty(identityVerification.getSessionInfo(), "{}");
        final SessionInfo sessionInfo;
        try {
            sessionInfo = new ObjectMapper().readValue(sessionInfoString, SessionInfo.class);
        } catch (JsonProcessingException e) {
            throw new IdentityVerificationException("Unable to deserialize session info", e);
        }

        final String customerId = (String) sessionInfo.getSessionAttributes().get(SessionInfo.ATTRIBUTE_PRIMARY_DOCUMENT_REFERENCE);
        if (StringUtils.isBlank(customerId)) {
            throw new IdentityVerificationException("Missing a customer ID value for calling Innovatrics, " + id);
        }
        return customerId;
    }
}
