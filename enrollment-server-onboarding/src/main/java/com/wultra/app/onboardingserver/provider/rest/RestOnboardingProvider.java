/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.rest;

import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.*;
import com.wultra.app.onboardingserver.provider.model.request.*;
import com.wultra.app.onboardingserver.provider.model.response.ApproveConsentResponse;
import com.wultra.app.onboardingserver.provider.model.response.EvaluateClientResponse;
import com.wultra.app.onboardingserver.provider.model.response.LookupUserResponse;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Rest specialization of {@link OnboardingProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
class RestOnboardingProvider implements OnboardingProvider {

    private final String correlationHeaderName;

    private final String requestIdHeaderName;

    private final RestClient restClient;

    public RestOnboardingProvider(final RestClient restClient, final RestOnboardingProviderConfiguration configuration) {
        this.restClient = restClient;
        this.correlationHeaderName = configuration.getCorrelationHeader().getName();
        this.requestIdHeaderName = configuration.getRequestIdHeader().getName();
    }

    @Override
    public LookupUserResponse lookupUser(final LookupUserRequest request) throws OnboardingProviderException {
        logger.debug("Looking up user for {}", request);
        final UserLookupRequestDto requestDto = convert(request);

        final ParameterizedTypeReference<UserLookupResponseDto> responseType = ParameterizedTypeReference.forType(UserLookupResponseDto.class);
        final UserLookupResponseDto response;

        try {
            response = restClient.post("/user/lookup", requestDto, null, createHeaders(), responseType).getBody();
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to lookup user for " + request, e);
        }

        if (response == null) {
            throw new OnboardingProviderException("Unable to lookup user for " + request + ", response was null");
        }
        logger.debug("Looked up {} for {}", response, request);
        return LookupUserResponse.builder()
                .userId(response.getUserId())
                .build();
    }

    @Override
    public void sendOtpCode(final SendOtpCodeRequest request) throws OnboardingProviderException {
        logger.debug("Sending otp for {}", request);
        final OtpSendRequestDto requestDto = convert(request);

        final ParameterizedTypeReference<OtpSendResponseDto> responseType = ParameterizedTypeReference.forType(OtpSendResponseDto.class);
        final OtpSendResponseDto response;

        try {
            response = restClient.post("/otp/send", requestDto, null, createHeaders(), responseType).getBody();
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to send otp for " + request, e);
        }

        if (response == null) {
            throw new OnboardingProviderException("Unable to send otp for " + request + ", response was null");
        }
        logger.debug("Sent otp {} for {}", response, request);
        if (!response.isOtpSent()) {
            throw new OnboardingProviderException("Otp has not been sent for " + request);
        }
    }

    @Override
    public String fetchConsent(final ConsentTextRequest request) throws OnboardingProviderException {
        logger.debug("Fetching consent for {}", request);
        final ConsentTextRequestDto requestDto = convert(request);

        final ParameterizedTypeReference<ConsentTextResponseDto> responseType = ParameterizedTypeReference.forType(ConsentTextResponseDto.class);
        final ConsentTextResponseDto response;

        try {
            response = restClient.post("/consent/text", requestDto, null, createHeaders(), responseType).getBody();
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to fetch consent for " + request, e);
        }

        if (response == null) {
            throw new OnboardingProviderException("Unable to fetch consent for " + request + ", response was null");
        }
        logger.debug("Fetched consent {} for {}", StringUtils.truncate(response.getConsentText(), 100), request);
        return response.getConsentText();
    }

    @Override
    public ApproveConsentResponse approveConsent(final ApproveConsentRequest request) throws OnboardingProviderException {
        logger.debug("Approving consent for {}", request);
        final ConsentStorageRequestDto requestDto = convert(request);

        try {
            restClient.post("/consent/storage", requestDto, null, createHeaders(), ParameterizedTypeReference.forType(Object.class));
            logger.debug("Approved consent for {}", request);
            return new ApproveConsentResponse();
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to approve consent for " + request, e);
        }
    }

    @Override
    public EvaluateClientResponse evaluateClient(final EvaluateClientRequest request) throws OnboardingProviderException {
        logger.debug("Evaluating client for {}", request);
        // TODO (racansky, 2022-07-20) suboptimal, not sending extracted data yet; adapter must retrieve data based on investigationId itself
        final ClientEvaluateRequestDto requestDto = convert(request);

        try {
            final ParameterizedTypeReference<ClientEvaluateResponseDto> type = ParameterizedTypeReference.forType(ClientEvaluateResponseDto.class);
            ResponseEntity<ClientEvaluateResponseDto> response = restClient.post("/client/evaluate", requestDto, null, createHeaders(), type);
            final boolean accepted = response.getBody() != null && response.getBody().getResult() == ClientEvaluateResponseDto.ResultEnum.OK;
            return EvaluateClientResponse.builder()
                    .accepted(accepted)
                    .build();

        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to evaluate client for " + request, e);
        }
    }

    private MultiValueMap<String, String> createHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(correlationHeaderName, UUID.randomUUID().toString());
        headers.add(requestIdHeaderName, UUID.randomUUID().toString());
        return headers;
    }

    private static UserLookupRequestDto convert(final LookupUserRequest source) {
        final UserLookupRequestDto target = new UserLookupRequestDto();
        target.setIdentification(source.getIdentification());
        target.setProcessId(source.getProcessId());
        return target;
    }

    private static OtpSendRequestDto convert(final SendOtpCodeRequest source) throws OnboardingProviderException {
        final OtpSendRequestDto target = new OtpSendRequestDto();
        target.setProcessId(source.getProcessId());
        target.setUserId(source.getUserId());
        target.setResend(source.isResend());
        target.setOtpCode(source.getOtpCode());
        target.setLanguage(source.getLocale().getLanguage());
        target.setOtpType(convert(source.getOtpType()));
        return target;
    }

    private static OtpSendRequestDto.OtpTypeEnum convert(SendOtpCodeRequest.OtpType source) throws OnboardingProviderException {
        switch (source) {
            case ACTIVATION:
                return OtpSendRequestDto.OtpTypeEnum.ACTIVATION;
            case USER_VERIFICATION:
                return OtpSendRequestDto.OtpTypeEnum.USER_VERIFICATION;
            default:
                throw new OnboardingProviderException("No mapping for " + source);
        }
    }

    private static ConsentTextRequestDto convert(final ConsentTextRequest source) {
        final ConsentTextRequestDto target = new ConsentTextRequestDto();
        target.setProcessId(source.getProcessId());
        target.setUserId(source.getUserId());
        target.setLanguage(source.getLocale().getLanguage());
        target.setConsentType(source.getConsentType());
        return target;
    }

    private static ConsentStorageRequestDto convert(final ApproveConsentRequest source) {
        final ConsentStorageRequestDto target = new ConsentStorageRequestDto();
        target.setProcessId(source.getProcessId());
        target.setUserId(source.getUserId());
        target.setConsentType(source.getConsentType());
        target.setApproved(source.isApproved());
        return target;
    }

    private static ClientEvaluateRequestDto convert(final EvaluateClientRequest source) {
        final ClientEvaluateRequestDto target = new ClientEvaluateRequestDto();
        target.setProcessId(source.getProcessId());
        target.setIdentityVerificationId(source.getIdentityVerificationId());
        target.setUserId(source.getUserId());
        target.setVerificationId(source.getVerificationId());
        return target;
    }
}
