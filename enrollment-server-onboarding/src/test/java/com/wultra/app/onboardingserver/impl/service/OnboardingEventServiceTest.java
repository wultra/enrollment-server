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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.OnboardingStatus;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.onboardingserver.common.database.ProcessedDocumentDataRepository;
import com.wultra.app.onboardingserver.common.database.entity.*;
import com.wultra.app.onboardingserver.common.errorhandling.OnboardingProcessException;
import com.wultra.app.onboardingserver.common.service.CommonOnboardingService;
import com.wultra.app.onboardingserver.configuration.IdentityVerificationConfig;
import com.wultra.app.onboardingserver.configuration.OnboardingConfig;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.*;
import com.wultra.app.onboardingserver.provider.model.response.ProcessEventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for {@link OnboardingEventService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class OnboardingEventServiceTest {

    @Mock
    private OnboardingProvider onboardingProvider;

    @Mock
    private IdentityVerificationConfig identityVerificationConfig;

    @Mock
    private CommonOnboardingService commonOnboardingService;

    @Mock
    private ProcessedDocumentDataRepository processedDocumentDataRepository;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().build();

    @Mock
    private OnboardingConfig onboardingConfig;

    @Captor
    private ArgumentCaptor<ProcessEventRequest> requestCaptor;

    @InjectMocks
    private OnboardingEventService tested;

    @Test
    void testPublishProcessFinished_successful() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.PROCESS_FINISHED));
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        process.setCustomData("""
                {"locale": "cs", "ipAddress": "10.0.0.1", "userAgent": "TestAgent/1.0"}""");

        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.publishProcessFinished(process, identityVerification);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final ProcessEventRequest request = requestCaptor.getValue();

        assertEquals("p1", request.getProcessId());
        assertEquals("onboarding", request.getProcessType());
        assertEquals("u1", request.getUserId());
        assertEquals("ext-u1", request.getExternalUserId());
        assertEquals("iv1", request.getIdentityVerificationId());
        assertEquals(EventType.PROCESS_FINISHED, request.getType());

        final ProcessFinishedEventData eventData = (ProcessFinishedEventData) request.getEventData();
        assertEquals(EventStatus.FINISHED, eventData.status());
        assertNull(eventData.errorDetail());
        assertEquals("cs", eventData.deviceData().locale().getLanguage());
        assertEquals("10.0.0.1", eventData.deviceData().ipAddress());
        assertEquals("TestAgent/1.0", eventData.deviceData().httpUserAgent());
    }

    @Test
    void testPublishProcessFinished_failed() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.PROCESS_FINISHED));
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FAILED);
        process.setErrorDetail("someError");
        process.setCustomData("""
                {"locale": "en"}""");

        final IdentityVerificationEntity identityVerification = createIdentityVerification();

        tested.publishProcessFinished(process, identityVerification);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final ProcessFinishedEventData eventData = (ProcessFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.FAILED, eventData.status());
        assertEquals("someError", eventData.errorDetail());
    }

    @Test
    void testPublishProcessFinished_eventTypeNotEnabled() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of());

        tested.publishProcessFinished(createProcess(OnboardingStatus.FINISHED), createIdentityVerification());

        verify(onboardingProvider, never()).processEvent(any());
    }

    @Test
    void testPublishProcessFinished_providerException_doesNotPropagate() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.PROCESS_FINISHED));
        when(onboardingProvider.processEvent(any())).thenThrow(new OnboardingProviderException("connection failed"));

        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        process.setCustomData("""
                {"locale": "en"}""");

        assertDoesNotThrow(() -> tested.publishProcessFinished(process, createIdentityVerification()));
    }

    @Test
    void testPublishDocumentVerificationFinished_accepted() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.DOCUMENT_VERIFICATION_FINISHED));
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("zenid");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());
        when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any())).thenReturn(List.of());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final DocumentVerificationEntity docVerification = createDocumentVerification(identityVerification);
        docVerification.setStatus(DocumentStatus.ACCEPTED);
        docVerification.setVerificationScore(8);

        final DocumentResultEntity result = new DocumentResultEntity();
        result.setId(1L);
        result.setExtractedData("""
                {"givenNames": "Jan", "surname": "Novak", "dateOfBirth": "1990-05-15"}""");
        docVerification.setResults(new LinkedHashSet<>(Set.of(result)));

        tested.publishDocumentVerificationFinished(docVerification);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final ProcessEventRequest request = requestCaptor.getValue();
        assertEquals(EventType.DOCUMENT_VERIFICATION_FINISHED, request.getType());

        final DocumentVerificationFinishedEventData eventData = (DocumentVerificationFinishedEventData) request.getEventData();
        assertEquals(EventStatus.ACCEPTED, eventData.status());
        assertEquals("dv1", eventData.documentVerificationId());
        assertEquals("upload1", eventData.documentId());
        assertEquals("zenid", eventData.provider());
        assertEquals(8, eventData.score());
        assertNotNull(eventData.documentVerificationResult());
        assertEquals("ID_CARD", eventData.documentVerificationResult().type());
        assertEquals("CZE", eventData.documentVerificationResult().country());
        assertNotNull(eventData.documentVerificationResult().data());
        assertEquals("Jan", eventData.documentVerificationResult().data().givenNames());
        assertEquals("Novak", eventData.documentVerificationResult().data().surname());
        assertEquals("1990-05-15", eventData.documentVerificationResult().data().dateOfBirth());
    }

    @Test
    void testPublishDocumentVerificationFinished_rejected() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.DOCUMENT_VERIFICATION_FINISHED));
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("microblink");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());
        when(processedDocumentDataRepository.findAllByDocumentVerificationIds(any())).thenReturn(List.of());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        when(commonOnboardingService.findProcess("p1")).thenReturn(createProcess(OnboardingStatus.FINISHED));

        final DocumentVerificationEntity docVerification = createDocumentVerification(identityVerification);
        docVerification.setStatus(DocumentStatus.REJECTED);
        docVerification.setRejectReason("documentVerificationRejected");

        tested.publishDocumentVerificationFinished(docVerification);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final DocumentVerificationFinishedEventData eventData = (DocumentVerificationFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.REJECTED, eventData.status());
        assertEquals("upload1", eventData.documentId());
        assertEquals("documentVerificationRejected", eventData.rejectReason());
        assertEquals("microblink", eventData.provider());
        assertNotNull(eventData.documentVerificationResult());
    }

    @Test
    void testPublishDocumentVerificationFinished_failed_noResult() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.DOCUMENT_VERIFICATION_FINISHED));
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("zenid");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final DocumentVerificationEntity docVerification = createDocumentVerification(identityVerification);
        docVerification.setStatus(DocumentStatus.FAILED);
        docVerification.setErrorDetail("provider timeout");

        tested.publishDocumentVerificationFinished(docVerification);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final DocumentVerificationFinishedEventData eventData = (DocumentVerificationFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.FAILED, eventData.status());
        assertEquals("provider timeout", eventData.errorDetail());
        assertNull(eventData.documentVerificationResult());
    }

    @Test
    void testPublishDocumentVerificationFinished_processNotFound() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.DOCUMENT_VERIFICATION_FINISHED));
        when(commonOnboardingService.findProcess("p1")).thenThrow(new OnboardingProcessException("not found"));

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final DocumentVerificationEntity docVerification = createDocumentVerification(identityVerification);
        docVerification.setStatus(DocumentStatus.ACCEPTED);

        assertDoesNotThrow(() -> tested.publishDocumentVerificationFinished(docVerification));
        verify(onboardingProvider, never()).processEvent(any());
    }

    @Test
    void testPublishFinalDocumentVerificationAccepted() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.FINAL_DOCUMENT_VERIFICATION_FINISHED));
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("zenid");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final DocumentVerificationEntity doc1 = createDocumentVerification(identityVerification);
        doc1.setUsedForVerification(true);
        doc1.setUploadId("upload1");
        final DocumentVerificationEntity doc2 = new DocumentVerificationEntity();
        doc2.setId("dv2");
        doc2.setUploadId("upload2");
        doc2.setUsedForVerification(true);
        doc2.setIdentityVerification(identityVerification);
        doc2.setResults(new LinkedHashSet<>());
        identityVerification.setDocumentVerifications(Set.of(doc1, doc2));

        tested.publishFinalDocumentVerificationAccepted(identityVerification);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final FinalDocumentVerificationFinishedEventData eventData = (FinalDocumentVerificationFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.ACCEPTED, eventData.status());
        assertNull(eventData.rejectReason());
        assertNull(eventData.errorDetail());
        assertEquals("zenid", eventData.provider());
        assertTrue(eventData.documentIds().containsAll(List.of("upload1", "upload2")));
    }

    @Test
    void testPublishFinalDocumentVerificationRejected() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.FINAL_DOCUMENT_VERIFICATION_FINISHED));
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("zenid");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final DocumentVerificationEntity doc1 = createDocumentVerification(identityVerification);
        doc1.setUsedForVerification(true);
        identityVerification.setDocumentVerifications(Set.of(doc1));

        tested.publishFinalDocumentVerificationRejected(identityVerification, "documents do not match");

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final FinalDocumentVerificationFinishedEventData eventData = (FinalDocumentVerificationFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.REJECTED, eventData.status());
        assertEquals("documents do not match", eventData.rejectReason());
        assertNull(eventData.errorDetail());
    }

    @Test
    void testPublishFinalDocumentVerificationFailed() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.FINAL_DOCUMENT_VERIFICATION_FINISHED));
        when(identityVerificationConfig.getDocumentVerificationProvider()).thenReturn("zenid");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final DocumentVerificationEntity doc1 = createDocumentVerification(identityVerification);
        doc1.setUsedForVerification(true);
        identityVerification.setDocumentVerifications(Set.of(doc1));

        tested.publishFinalDocumentVerificationFailed(identityVerification, "crosscheck failed");

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final FinalDocumentVerificationFinishedEventData eventData = (FinalDocumentVerificationFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.FAILED, eventData.status());
        assertNull(eventData.rejectReason());
        assertEquals("crosscheck failed", eventData.errorDetail());
    }

    @Test
    void testPublishPresenceCheckFinished_accepted_withPhoto() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.PRESENCE_CHECK_FINISHED));
        when(identityVerificationConfig.getPresenceCheckProvider()).thenReturn("iproov");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final byte[] photoBytes = "fake-photo-data".getBytes();
        final Image photo = Image.builder()
                .filename("photo.jpg")
                .data(photoBytes)
                .build();

        final PresenceCheckResult result = new PresenceCheckResult();
        result.setStatus(PresenceCheckStatus.ACCEPTED);
        result.setPhoto(photo);

        tested.publishPresenceCheckFinished(identityVerification, result);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final PresenceCheckFinishedEventData eventData = (PresenceCheckFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.ACCEPTED, eventData.status());
        assertNull(eventData.rejectReason());
        assertNull(eventData.errorDetail());
        assertEquals("iproov", eventData.provider());
        assertEquals(10, eventData.score());
        assertNotNull(eventData.presenceCheckResult());
        assertEquals(Base64.getEncoder().encodeToString(photoBytes), eventData.presenceCheckResult().frame());
    }

    @Test
    void testPublishPresenceCheckFinished_rejected_noPhoto() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of(EventType.PRESENCE_CHECK_FINISHED));
        when(identityVerificationConfig.getPresenceCheckProvider()).thenReturn("iproov");
        when(onboardingProvider.processEvent(any())).thenReturn(ProcessEventResponse.builder().build());

        final IdentityVerificationEntity identityVerification = createIdentityVerification();
        final OnboardingProcessEntity process = createProcess(OnboardingStatus.FINISHED);
        when(commonOnboardingService.findProcess("p1")).thenReturn(process);

        final PresenceCheckResult result = new PresenceCheckResult();
        result.setStatus(PresenceCheckStatus.REJECTED);
        result.setRejectReason("liveness failed");

        tested.publishPresenceCheckFinished(identityVerification, result);

        verify(onboardingProvider).processEvent(requestCaptor.capture());
        final PresenceCheckFinishedEventData eventData = (PresenceCheckFinishedEventData) requestCaptor.getValue().getEventData();
        assertEquals(EventStatus.REJECTED, eventData.status());
        assertEquals("liveness failed", eventData.rejectReason());
        assertNull(eventData.presenceCheckResult());
    }

    @Test
    void testPublishPresenceCheckFinished_eventTypeNotEnabled() throws Exception {
        when(onboardingConfig.getEventTypes()).thenReturn(List.of());

        final PresenceCheckResult result = new PresenceCheckResult();
        result.setStatus(PresenceCheckStatus.ACCEPTED);

        tested.publishPresenceCheckFinished(createIdentityVerification(), result);

        verify(onboardingProvider, never()).processEvent(any());
    }

    private static OnboardingProcessEntity createProcess(final OnboardingStatus status) {
        final OnboardingProcessConfigurationEntity processConfiguration = new OnboardingProcessConfigurationEntity();
        processConfiguration.setProcessType("onboarding");

        final OnboardingProcessEntity process = new OnboardingProcessEntity();
        process.setId("p1");
        process.setStatus(status);
        process.setExternalUserId("ext-u1");
        process.setCustomData("{}");
        process.setProcessConfiguration(processConfiguration);
        return process;
    }

    private static IdentityVerificationEntity createIdentityVerification() {
        final IdentityVerificationEntity identityVerification = new IdentityVerificationEntity();
        identityVerification.setId("iv1");
        identityVerification.setProcessId("p1");
        identityVerification.setUserId("u1");
        identityVerification.setDocumentVerifications(Set.of());
        return identityVerification;
    }

    private static DocumentVerificationEntity createDocumentVerification(final IdentityVerificationEntity identityVerification) {
        final DocumentVerificationEntity documentVerification = new DocumentVerificationEntity();
        documentVerification.setId("dv1");
        documentVerification.setUploadId("upload1");
        documentVerification.setType(DocumentType.ID_CARD);
        documentVerification.setCountry("CZE");
        documentVerification.setIdentityVerification(identityVerification);
        documentVerification.setResults(new LinkedHashSet<>());
        return documentVerification;
    }
}
