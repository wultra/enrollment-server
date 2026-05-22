/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
import com.wultra.app.onboardingserver.provider.model.request.EvaluateClientRequest;
import com.wultra.app.onboardingserver.provider.model.request.EventType;
import com.wultra.app.onboardingserver.provider.model.request.ProcessEventRequest;
import com.wultra.app.onboardingserver.provider.model.request.ProcessFinishedEventData;
import com.wultra.app.onboardingserver.provider.model.request.EventStatus;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;


/**
 * Unit tests for {@link RestOnboardingProvider}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class RestOnboardingProviderTest {

    @Mock
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<ClientEvaluateRequestDto> clientEvaluateRequestDtoArgumentCaptor;

    @Captor
    private ArgumentCaptor<ProcessEventRequestDto> processEventRequestDtoArgumentCaptor;

    private RestOnboardingProvider tested;

    @BeforeEach
    void setUp() {
        final var config = new RestOnboardingProviderConfigProperties();

        tested = new RestOnboardingProvider(restClient, config);
    }

    @Test
    void testEvaluateClient_withExtractedData_correctRequestBody() throws OnboardingProviderException, RestClientException {
        // given
        final var request = buildRequestWithExtractedData();

        final var responseDto = new ClientEvaluateResponseDto();
        responseDto.setResult(ClientEvaluateResponseDto.ResultEnum.OK);

        when(restClient.post(
                eq("/client/evaluate"),
                clientEvaluateRequestDtoArgumentCaptor.capture(),
                isNull(),
                ArgumentMatchers.any(),
                ArgumentMatchers.<ParameterizedTypeReference<ClientEvaluateResponseDto>>any()
        )).thenReturn(ResponseEntity.ok(responseDto));

        // when
        tested.evaluateClient(request);

        // then
        assertRequestDtoWithExtractedData(clientEvaluateRequestDtoArgumentCaptor.getValue());
    }

    @Test
    void testEvaluateClient_withoutExtractedData_correctRequestBody() throws OnboardingProviderException, RestClientException {
        // given
        final var request = buildRequestWithoutExtractedData();

        final var responseDto = new ClientEvaluateResponseDto();
        responseDto.setResult(ClientEvaluateResponseDto.ResultEnum.OK);

        when(restClient.post(
                eq("/client/evaluate"),
                clientEvaluateRequestDtoArgumentCaptor.capture(),
                isNull(),
                ArgumentMatchers.any(),
                ArgumentMatchers.<ParameterizedTypeReference<ClientEvaluateResponseDto>>any()
        )).thenReturn(ResponseEntity.ok(responseDto));

        // when
        tested.evaluateClient(request);

        // then
        assertRequestDtoWithoutExtractedData(clientEvaluateRequestDtoArgumentCaptor.getValue());
    }

    @Test
    void testProcessEvent_processFinished_correctRequestBody() throws OnboardingProviderException, RestClientException {
        final var request = ProcessEventRequest.builder()
                .processId("dummyProcessId")
                .processType("dummyProcessType")
                .userId("dummyUserId")
                .externalUserId("dummyExternalUserId")
                .identityVerificationId("dummyIdentityVerificationId")
                .type(EventType.PROCESS_FINISHED)
                .eventData(ProcessFinishedEventData.builder()
                        .status(EventStatus.FINISHED)
                        .deviceData(ProcessFinishedEventData.DeviceData.builder()
                                .locale(Locale.ENGLISH)
                                .ipAddress("127.0.0.1")
                                .httpUserAgent("Mozilla/5.0")
                                .fdsData(Map.of("fdsIdentifier", "42"))
                                .build())
                        .build())
                .build();

        when(restClient.post(
                eq("/process/event"),
                processEventRequestDtoArgumentCaptor.capture(),
                isNull(),
                ArgumentMatchers.any(),
                ArgumentMatchers.<ParameterizedTypeReference<ProcessEventResponseDto>>any()
        )).thenReturn(ResponseEntity.ok(new ProcessEventResponseDto()));

        tested.processEvent(request);

        final ProcessEventRequestDto requestDto = processEventRequestDtoArgumentCaptor.getValue();
        assertEquals("dummyProcessId", requestDto.getProcessId());
        assertEquals("dummyProcessType", requestDto.getProcessType());
        assertEquals("dummyUserId", requestDto.getUserId());
        assertEquals("dummyExternalUserId", requestDto.getExternalUserId());
        assertEquals("dummyIdentityVerificationId", requestDto.getIdentityVerificationId());
        assertEquals(EventTypeDto.PROCESS_FINISHED, requestDto.getType());

        final var expected = ProcessFinishedEventDataDto.builder()
                .process(ProcessFinishedEventDataDto.Process.builder()
                        .status("FINISHED")
                        .errorDetail(null)
                        .deviceData(ProcessFinishedEventDataDto.DeviceData.builder()
                                .locale("EN")
                                .ipAddress("127.0.0.1")
                                .httpUserAgent("Mozilla/5.0")
                                .fdsData(Map.of("fdsIdentifier", "42"))
                                .build())
                        .build())
                .build();
        assertEquals(expected, requestDto.getEventData());
    }

    private static EvaluateClientRequest buildRequestWithExtractedData() {
        return EvaluateClientRequest.builder()
                .processId("dummyProcessId")
                .processType("dummyProcessType")
                .userId("dummyUserId")
                .identityVerificationId("dummyIdentityVerificationId")
                .verificationId("dummyVerificationId")
                .provider("dummyProvider")
                .status(EvaluateClientRequest.Status.SUCCESS)
                .documentCheckResult(EvaluateClientRequest.DocumentCheckResult.builder()
                        .person(EvaluateClientRequest.Person.builder()
                                .givenNames("John")
                                .surname("Doe")
                                .dateOfBirth(LocalDate.parse("1990-01-01"))
                                .build())
                        .documents(List.of(
                                EvaluateClientRequest.Document.builder()
                                        .type(DocumentType.ID_CARD)
                                        .country("CZE")
                                        .status(EvaluateClientRequest.Status.SUCCESS)
                                        .score(10)
                                        .data(EvaluateClientRequest.DocumentData.builder()
                                                .givenNames("John")
                                                .surname("Doe")
                                                .dateOfBirth(LocalDate.parse("1990-01-01"))
                                                .placeOfBirth("Czechia")
                                                .sex("M")
                                                .nationality("Czech")
                                                .personalNumber("123")
                                                .documentNumber("456")
                                                .dateOfIssue(LocalDate.parse("2020-01-01"))
                                                .dateOfExpiry(LocalDate.parse("2030-01-01"))
                                                .authority("ABC")
                                                .build())
                                        .images(List.of(
                                                EvaluateClientRequest.Image.builder()
                                                        .type(ProcessedDocumentDataType.FACE_IMAGE)
                                                        .data(new byte[] { 1 })
                                                        .build(),
                                                EvaluateClientRequest.Image.builder()
                                                        .type(ProcessedDocumentDataType.DOCUMENT_FRONT_SIDE)
                                                        .data(new byte[] { 2 })
                                                        .build(),
                                                EvaluateClientRequest.Image.builder()
                                                        .type(ProcessedDocumentDataType.DOCUMENT_BACK_SIDE)
                                                        .data(new byte[] { 3 })
                                                        .build()
                                        ))
                                        .rawData("{}")
                                        .build()
                        ))
                        .build())
                .build();
    }

    private static EvaluateClientRequest buildRequestWithoutExtractedData() {
        return EvaluateClientRequest.builder()
                .processId("dummyProcessId")
                .processType("dummyProcessType")
                .userId("dummyUserId")
                .identityVerificationId("dummyIdentityVerificationId")
                .verificationId("dummyVerificationId")
                .provider("dummyProvider")
                .status(EvaluateClientRequest.Status.SUCCESS)
                .documentCheckResult(EvaluateClientRequest.DocumentCheckResult.builder()
                        .documents(List.of(
                                EvaluateClientRequest.Document.builder()
                                        .type(DocumentType.ID_CARD)
                                        .country("CZE")
                                        .status(EvaluateClientRequest.Status.SUCCESS)
                                        .score(10)
                                        .build()
                        ))
                        .build())
                .build();
    }

    private static void assertRequestDtoWithExtractedData(final ClientEvaluateRequestDto requestDto) {
        final var expectedDocumentCheckResult = ClientEvaluateRequestDto.DocumentCheckResult.builder()
                        .person(ClientEvaluateRequestDto.Person.builder()
                                .givenNames("John")
                                .surname("Doe")
                                .dateOfBirth(LocalDate.parse("1990-01-01"))
                                .build())
                        .documents(List.of(
                                ClientEvaluateRequestDto.Document.builder()
                                        .type(ClientEvaluateRequestDto.DocumentType.ID_CARD)
                                        .country("CZE")
                                        .status(ClientEvaluateRequestDto.Status.SUCCESS)
                                        .score(10)
                                        .data(ClientEvaluateRequestDto.DocumentData.builder()
                                                .givenNames("John")
                                                .surname("Doe")
                                                .dateOfBirth(LocalDate.parse("1990-01-01"))
                                                .placeOfBirth("Czechia")
                                                .sex("M")
                                                .nationality("Czech")
                                                .personalNumber("123")
                                                .documentNumber("456")
                                                .dateOfIssue(LocalDate.parse("2020-01-01"))
                                                .dateOfExpiry(LocalDate.parse("2030-01-01"))
                                                .authority("ABC")
                                                .build())
                                        .images(List.of(
                                                ClientEvaluateRequestDto.Image.builder()
                                                        .type(ClientEvaluateRequestDto.ImageType.FACE)
                                                        .data(new byte[] { 1 })
                                                        .build(),
                                                ClientEvaluateRequestDto.Image.builder()
                                                        .type(ClientEvaluateRequestDto.ImageType.DOCUMENT_FRONT_SIDE)
                                                        .data(new byte[] { 2 })
                                                        .build(),
                                                ClientEvaluateRequestDto.Image.builder()
                                                        .type(ClientEvaluateRequestDto.ImageType.DOCUMENT_BACK_SIDE)
                                                        .data(new byte[] { 3 })
                                                        .build()
                                        ))
                                        .rawData("{}")
                                        .build()
                        ))
                        .build();

        assertEquals("dummyProcessId", requestDto.getProcessId());
        assertEquals("dummyProcessType", requestDto.getProcessType());
        assertEquals("dummyIdentityVerificationId", requestDto.getIdentityVerificationId());
        assertEquals("dummyUserId", requestDto.getUserId());
        assertEquals("dummyVerificationId", requestDto.getVerificationId());
        assertEquals("dummyProvider", requestDto.getProvider());
        assertEquals(ClientEvaluateRequestDto.Status.SUCCESS, requestDto.getStatus());
        assertEquals(expectedDocumentCheckResult, requestDto.getDocumentCheckResult());
    }

    private static void assertRequestDtoWithoutExtractedData(final ClientEvaluateRequestDto requestDto) {
        final var expectedDocumentCheckResult = ClientEvaluateRequestDto.DocumentCheckResult.builder()
                .documents(List.of(
                        ClientEvaluateRequestDto.Document.builder()
                                .type(ClientEvaluateRequestDto.DocumentType.ID_CARD)
                                .country("CZE")
                                .status(ClientEvaluateRequestDto.Status.SUCCESS)
                                .score(10)
                                .images(List.of())
                                .build()
                ))
                .build();

        assertEquals("dummyProcessId", requestDto.getProcessId());
        assertEquals("dummyProcessType", requestDto.getProcessType());
        assertEquals("dummyIdentityVerificationId", requestDto.getIdentityVerificationId());
        assertEquals("dummyUserId", requestDto.getUserId());
        assertEquals("dummyVerificationId", requestDto.getVerificationId());
        assertEquals("dummyProvider", requestDto.getProvider());
        assertEquals(ClientEvaluateRequestDto.Status.SUCCESS, requestDto.getStatus());
        assertEquals(expectedDocumentCheckResult, requestDto.getDocumentCheckResult());
    }
}
