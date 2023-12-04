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

import com.google.common.base.Strings;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.api.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.api.provider.PresenceCheckProvider;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CustomerInspectResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.EvaluateCustomerLivenessResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.SelfieSimilarityWith;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of the {@link PresenceCheckProvider} with <a href="https://www.innovatrics.com/">Innovatrics</a>.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "innovatrics")
@Component
@Slf4j
@AllArgsConstructor
class InnovatricsPresenceCheckProvider implements PresenceCheckProvider {

    private static final String INNOVATRICS_CUSTOMER_ID = "InnovatricsCustomerId";

    private final InnovatricsApiService innovatricsApiService;

    private final InnovatricsConfigProps configuration;

    @Override
    public void initPresenceCheck(final OwnerId id, final Image photo) {
        logger.debug("#initPresenceCheck does nothing for Innovatrics, {}", id);
    }

    @Override
    public boolean shouldProvideTrustedPhoto() {
        return false;
    }

    @Override
    public SessionInfo startPresenceCheck(final OwnerId id) {
        logger.debug("#startPresenceCheck does nothing for Innovatrics, {}", id);
        return new SessionInfo();
    }

    @Override
    public PresenceCheckResult getResult(final OwnerId id, final SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException {
        final String customerId = fetchCustomerId(id, sessionInfo);

        final Optional<PresenceCheckError> evaluateLivenessError = evaluateLiveness(customerId, id);
        if (evaluateLivenessError.isPresent()) {
            return convert(evaluateLivenessError.get());
        }
        logger.debug("Liveness passed, {}", id);

        // do not be afraid of the timing attack, the action is invoked by the state machine, not by the user
        final Optional<PresenceCheckError> inspectCustomerError = inspectCustomer(customerId, id);
        if (inspectCustomerError.isPresent()) {
            return convert(inspectCustomerError.get());
        }
        logger.debug("Customer inspection passed, {}", id);

        return accepted();
    }

    private static PresenceCheckResult accepted() {
        final PresenceCheckResult result = new PresenceCheckResult();
        result.setStatus(PresenceCheckStatus.ACCEPTED);
        return result;
    }

    private static PresenceCheckResult convert(final PresenceCheckError source) {
        final PresenceCheckResult target = new PresenceCheckResult();
        target.setStatus(source.status());
        target.setErrorDetail(source.errorDetail());
        target.setRejectReason(source.rejectReason());
        return target;
    }

    private Optional<PresenceCheckError> evaluateLiveness(final String customerId, final OwnerId id) throws RemoteCommunicationException {
        final EvaluateCustomerLivenessResponse livenessResponse = innovatricsApiService.evaluateLiveness(customerId, id);
        final Double score = livenessResponse.getScore();
        final EvaluateCustomerLivenessResponse.ErrorCodeEnum errorCode = livenessResponse.getErrorCode();
        logger.debug("Presence check score: {}, errorCode: {}, {}", score, errorCode, id);
        final double scoreThreshold = configuration.getPresenceCheckConfiguration().getScore();

        if (score == null) {
            return fail(errorCode == null ? "Score is null" : errorCode.getValue());
        } else if (score < scoreThreshold) {
            return reject(String.format(Locale.ENGLISH, "Score %.3f is bellow the threshold %.3f", score, scoreThreshold));
        } else {
            return success();
        }
    }

    private Optional<PresenceCheckError> inspectCustomer(final String customerId, final OwnerId id) throws RemoteCommunicationException{
        final CustomerInspectResponse customerInspectResponse = innovatricsApiService.inspectCustomer(customerId, id);

        if (customerInspectResponse.getSelfieInspection() == null || customerInspectResponse.getSelfieInspection().getSimilarityWith() == null) {
            return fail("Missing selfie inspection payload");
        }

        final SelfieSimilarityWith similarityWith = customerInspectResponse.getSelfieInspection().getSimilarityWith();

        if (!Boolean.TRUE.equals(similarityWith.getLivenessSelfies())) {
            return reject("The person in the selfie does not match a person in each liveness selfie");
        } else if (!Boolean.TRUE.equals(similarityWith.getDocumentPortrait())) {
            return reject("The person in the selfie does not match a person in the document portrait");
        } else {
            return success();
        }
    }

    private static Optional<PresenceCheckError> success() {
        return Optional.empty();
    }

    private static Optional<PresenceCheckError> reject(final String rejectReason) {
        return Optional.of(new PresenceCheckError(PresenceCheckStatus.REJECTED, rejectReason, null));
    }

    private static Optional<PresenceCheckError> fail(final String errorDetail) {
        return Optional.of(new PresenceCheckError(PresenceCheckStatus.FAILED, null, errorDetail));
    }

    private static String fetchCustomerId(final OwnerId id, final SessionInfo sessionInfo) throws PresenceCheckException {
        // TODO (racansky, 2023-11-28) discuss the format with Jan Pesek
        final String customerId = (String) sessionInfo.getSessionAttributes().get(INNOVATRICS_CUSTOMER_ID);
        if (Strings.isNullOrEmpty(customerId)) {
            throw new PresenceCheckException("Missing a customer ID value for calling Innovatrics, " + id);
        }
        return customerId;
    }

    @Override
    public void cleanupIdentityData(final OwnerId id, final SessionInfo sessionInfo) throws PresenceCheckException, RemoteCommunicationException {
        logger.info("Invoked cleanupIdentityData, {}", id);
        final String customerId = fetchCustomerId(id, sessionInfo);

        innovatricsApiService.deleteLiveness(customerId, id);
        logger.debug("Deleted liveness, {}", id);

        innovatricsApiService.deleteSelfie(customerId, id);
        logger.debug("Deleted selfie, {}", id);
    }

    record PresenceCheckError(PresenceCheckStatus status, String rejectReason, String errorDetail){}
}
