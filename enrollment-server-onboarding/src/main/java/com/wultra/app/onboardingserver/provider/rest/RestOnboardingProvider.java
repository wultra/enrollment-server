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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.*;
import com.wultra.app.onboardingserver.provider.model.response.*;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rest specialization of {@link OnboardingProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
public class RestOnboardingProvider implements OnboardingProvider {

    private final String correlationHeaderName;

    private final String requestIdHeaderName;

    private final RestClient restClient;

    public RestOnboardingProvider(final RestClient restClient, final RestOnboardingProviderConfigProperties configuration) {
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
            throw new OnboardingProviderException("Unable to lookup user for " + request.getProcessId(), e);
        }

        if (response == null) {
            throw new OnboardingProviderException("Unable to lookup user for " + request.getProcessId() + ", response was null");
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
            throw new OnboardingProviderException("Unable to send otp for " + request.getProcessId(), e);
        }

        if (response == null) {
            throw new OnboardingProviderException("Unable to send otp for " + request.getProcessId() + ", response was null");
        }
        logger.debug("Sent otp {} for {}", response, request);
        if (!response.isOtpSent()) {
            throw new OnboardingProviderException("Otp has not been sent for " + request.getProcessId());
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
            throw new OnboardingProviderException("Unable to fetch consent for " + request.getProcessId(), e);
        }

        if (response == null) {
            throw new OnboardingProviderException("Unable to fetch consent for " + request.getProcessId() + ", response was null");
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

        final var requestDto = convert(request);

        try {
            final ParameterizedTypeReference<ClientEvaluateResponseDto> type = ParameterizedTypeReference.forType(ClientEvaluateResponseDto.class);
            ResponseEntity<ClientEvaluateResponseDto> response = restClient.post("/client/evaluate", requestDto, null, createHeaders(), type);
            logger.debug("Got evaluating client response: {}", response);

            final var body = Optional.ofNullable(response)
                    .map(ResponseEntity::getBody)
                    .orElseThrow(() -> new OnboardingProviderException("Unable to fetch client evaluation, response was null"));

            return convert(body);
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to evaluate client for " + request.getProcessId() + "message:"+ e.getMessage(), e);
        }
    }

    @Override
    public ProcessEventResponse processEvent(final ProcessEventRequest request) throws OnboardingProviderException {
        logger.debug("Processing event for {}", request);
        final ProcessEventRequestDto requestDto = convert(request);

        try {
            final ParameterizedTypeReference<ProcessEventResponseDto> type = ParameterizedTypeReference.forType(ProcessEventResponseDto.class);
            final ResponseEntity<ProcessEventResponseDto> response = restClient.post("/process/event", requestDto, null, createHeaders(), type);
            logger.debug("Got processing event response: {}", response);
            return ProcessEventResponse.builder().build();
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to process event for " + request.getProcessId(), e);
        }
    }

    @Override
    public ApproveClientResponse approveClient(final ApproveClientRequest request) throws OnboardingProviderException {
        logger.debug("Approving client for {}", request);
        final ApproveClientRequestDto requestDto = convert(request);

        try {
            final ParameterizedTypeReference<ApproveClientResponseDto> type = ParameterizedTypeReference.forType(ApproveClientResponseDto.class);
            final ResponseEntity<ApproveClientResponseDto> response = restClient.post("/client/approve", requestDto, null, createHeaders(), type);
            logger.debug("Got approval client response: {}", response);
            if (response.getBody() == null) {
                throw new OnboardingProviderException("Client approval response is null");
            }
            return convert(response.getBody());
        } catch (RestClientException e) {
            throw new OnboardingProviderException("Unable to approve client for " + request.processId()+ "message:"+ e.getMessage(), e);
        }
    }

    private static ApproveClientResponse convert(final ApproveClientResponseDto source) {
        return ApproveClientResponse.builder()
                .result(convert(source.result()))
                .resultReason(source.resultReason())
                .build();
    }

    private static ApproveClientResponse.ApprovalResult convert(final ApproveClientResponseDto.EvaluationResult source) {
        return switch (source) {
            case OK -> ApproveClientResponse.ApprovalResult.OK;
            case NOK -> ApproveClientResponse.ApprovalResult.NOK;
            case WAIT -> ApproveClientResponse.ApprovalResult.WAIT;
        };
    }

    private static ApproveClientRequestDto convert(final ApproveClientRequest source) {
        return ApproveClientRequestDto.builder()
                .processId(source.processId())
                .processType(source.processType())
                .userId(source.userId())
                .identityVerificationId(source.identityVerificationId())
                .provider(source.provider())
                .status(convert(source.status()))
                .score(source.score())
                .presenceCheckResult(new ApproveClientRequestDto.PresenceCheckResult(source.image()))
                .build();
    }

    private static ApproveClientRequestDto.Status convert(final ApproveClientRequest.Status source) {
        return switch (source) {
            case SUCCESS -> ApproveClientRequestDto.Status.SUCCESS;
            case FAILURE -> ApproveClientRequestDto.Status.FAILURE;
        };
    }

    private static ProcessEventRequestDto convert(final ProcessEventRequest source) throws OnboardingProviderException {
        final ProcessEventRequestDto target = new ProcessEventRequestDto();
        target.setProcessId(source.getProcessId());
        target.setProcessType(source.getProcessType());
        target.setIdentityVerificationId(source.getIdentityVerificationId());
        target.setUserId(source.getUserId());
        target.setType(convert(source.getType()));
        target.setData(source.getEventData().asMap());
        return target;
    }

    private static ProcessEventRequestDto.EventType convert(ProcessEventRequest.EventType source) throws OnboardingProviderException {
        if (source == ProcessEventRequest.EventType.FINISHED) {
            return ProcessEventRequestDto.EventType.FINISHED;
        } else {
            throw new OnboardingProviderException("No mapping for " + source);
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
        target.setProcessType(source.getProcessType());
        return target;
    }

    private static OtpSendRequestDto convert(final SendOtpCodeRequest source) {
        final OtpSendRequestDto target = new OtpSendRequestDto();
        target.setProcessId(source.getProcessId());
        target.setProcessType(source.getProcessType());
        target.setUserId(source.getUserId());
        target.setResend(source.isResend());
        target.setOtpCode(source.getOtpCode());
        target.setLanguage(source.getLocale().getLanguage());
        target.setOtpType(convert(source.getOtpType()));
        return target;
    }

    private static OtpSendRequestDto.OtpTypeEnum convert(SendOtpCodeRequest.OtpType source) {
        return switch (source) {
            case ACTIVATION -> OtpSendRequestDto.OtpTypeEnum.ACTIVATION;
            case USER_VERIFICATION -> OtpSendRequestDto.OtpTypeEnum.USER_VERIFICATION;
        };
    }

    private static ConsentTextRequestDto convert(final ConsentTextRequest source) {
        final ConsentTextRequestDto target = new ConsentTextRequestDto();
        target.setProcessId(source.getProcessId());
        target.setProcessType(source.getProcessType());
        target.setUserId(source.getUserId());
        target.setLanguage(source.getLocale().getLanguage());
        target.setConsentType(source.getConsentType());
        return target;
    }

    private static ConsentStorageRequestDto convert(final ApproveConsentRequest source) {
        final ConsentStorageRequestDto target = new ConsentStorageRequestDto();
        target.setProcessId(source.getProcessId());
        target.setProcessType(source.getProcessType());
        target.setUserId(source.getUserId());
        target.setConsentType(source.getConsentType());
        target.setApproved(source.isApproved());
        return target;
    }

    private static ClientEvaluateRequestDto convert(final EvaluateClientRequest source) {
        final var documents = source.getDocumentCheckResult()
                .documents()
                .stream()
                .map(RestOnboardingProvider::convert)
                .toList();

        final var person = convert(source.getDocumentCheckResult().person());

        final ClientEvaluateRequestDto target = new ClientEvaluateRequestDto();
        target.setProcessId(source.getProcessId());
        target.setProcessType(source.getProcessType());
        target.setIdentityVerificationId(source.getIdentityVerificationId());
        target.setUserId(source.getUserId());
        target.setVerificationId(source.getVerificationId());
        target.setProvider(source.getProvider());
        target.setStatus(convert(source.getStatus()));
        target.setDocumentCheckResult(new ClientEvaluateRequestDto.DocumentCheckResult(documents, person));
        return target;
    }

    private static ClientEvaluateRequestDto.Person convert(final EvaluateClientRequest.Person source) {
        if (source == null) {
            return null;
        }

        return ClientEvaluateRequestDto.Person.builder()
                .surname(source.surname())
                .givenNames(source.givenNames())
                .dateOfBirth(source.dateOfBirth())
                .build();
    }

    private static EvaluateClientResponse convert(final ClientEvaluateResponseDto source) {

        final var result = switch (source.getResult()) {
            case OK -> EvaluateClientResponse.EvaluationResult.OK;
            case NOK ->  EvaluateClientResponse.EvaluationResult.NOK;
            case WAIT ->  EvaluateClientResponse.EvaluationResult.WAIT;
        };

        return EvaluateClientResponse.builder()
                .evaluationResult(result)
                .resultReason(source.getResultReason())
                .build();
    }

    private static ClientEvaluateRequestDto.Status convert(final EvaluateClientRequest.Status source) {
        return switch (source) {
            case SUCCESS -> ClientEvaluateRequestDto.Status.SUCCESS;
            case FAILURE -> ClientEvaluateRequestDto.Status.FAILURE;
        };
    }

    private static ClientEvaluateRequestDto.Document convert(final EvaluateClientRequest.Document source) {
        final var images = Optional.ofNullable(source.images())
                .orElse(List.of())
                .stream()
                .map(RestOnboardingProvider::convert)
                .toList();

        return ClientEvaluateRequestDto.Document.builder()
                .type(convert(source.type()))
                .country(source.country())
                .status(convert(source.status()))
                .score(source.score())
                .data(convert(source.data()))
                .images(images)
                .rawData(source.rawData())
                .build();
    }

    private static ClientEvaluateRequestDto.DocumentType convert(final DocumentType source) {
        return switch (source) {
            case ID_CARD -> ClientEvaluateRequestDto.DocumentType.ID_CARD;
            case DRIVING_LICENSE -> ClientEvaluateRequestDto.DocumentType.DRIVING_LICENCE;
            case PASSPORT -> ClientEvaluateRequestDto.DocumentType.PASSPORT;
            default -> throw new IllegalArgumentException("Unsupported document type: " + source);
        };
    }

    private static ClientEvaluateRequestDto.DocumentData convert(final EvaluateClientRequest.DocumentData source) {
        if (source == null) {
            return null;
        }

        return ClientEvaluateRequestDto.DocumentData.builder()
                .givenNames(source.givenNames())
                .surname(source.surname())
                .dateOfBirth(source.dateOfBirth())
                .placeOfBirth(source.placeOfBirth())
                .sex(source.sex())
                .nationality(source.nationality())
                .personalNumber(source.personalNumber())
                .documentNumber(source.documentNumber())
                .dateOfIssue(source.dateOfIssue())
                .dateOfExpiry(source.dateOfExpiry())
                .authority(source.authority())
                .build();
    }

    private static ClientEvaluateRequestDto.Image convert(final EvaluateClientRequest.Image source) {
        return ClientEvaluateRequestDto.Image.builder()
                .type(convert(source.type()))
                .data(source.data())
                .build();
    }

    private static ClientEvaluateRequestDto.ImageType convert(final ProcessedDocumentDataType source) {
        return switch (source) {
            case FACE_IMAGE -> ClientEvaluateRequestDto.ImageType.FACE;
        };
    }
}
