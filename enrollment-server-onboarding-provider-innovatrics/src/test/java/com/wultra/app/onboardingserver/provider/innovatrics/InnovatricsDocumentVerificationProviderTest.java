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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.*;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test of {@link InnovatricsDocumentVerificationProvider}.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class InnovatricsDocumentVerificationProviderTest {

    @Autowired
    private InnovatricsDocumentVerificationProvider tested;

    @MockBean
    private InnovatricsApiService apiService;

    @Test
    void testSubmitDocuments() throws Exception {
        when(apiService.createCustomer()).thenReturn(Optional.of(new CreateCustomerResponse("c123")));

        final Links docLink = new Links("docResource");
        final CreateDocumentResponse documentResponse = new CreateDocumentResponse();
        documentResponse.setLinks(docLink);
        when(apiService.createDocument("c123", DocumentType.PASSPORT)).thenReturn(Optional.of(documentResponse));

        final CreateDocumentPageResponse pageResponse = new CreateDocumentPageResponse();
        when(apiService.provideDocumentPage("c123", CardSide.FRONT, "img".getBytes()))
                .thenReturn(Optional.of(pageResponse));

        final BiometricMultiValueAttribute ageAttr = new BiometricMultiValueAttribute("42", null, null, null, "40");
        final MultiValueAttribute surnameAttr = new MultiValueAttribute("SPECIMEN", null, null, null);
        final Customer customer = new Customer();
        customer.age(ageAttr).setSurname(surnameAttr);
        final GetCustomerResponse customerResponse = new GetCustomerResponse();
        customerResponse.customer(customer);
        when(apiService.getCustomer("c123")).thenReturn(Optional.of(customerResponse));

        final OwnerId ownerId = new OwnerId();
        ownerId.setUserId("joe");
        ownerId.setActivationId("a123");
        final SubmittedDocument doc = new SubmittedDocument();
        doc.setType(DocumentType.PASSPORT);
        doc.setSide(CardSide.FRONT);
        doc.setPhoto(Image.builder().data("img".getBytes()).build());

        final DocumentsSubmitResult results = tested.submitDocuments(ownerId, List.of(doc));
        verify(apiService).getCustomer("c123");
        assertEquals(1, results.getResults().size());

        final DocumentSubmitResult result = results.getResults().get(0);
        assertEquals("c123", result.getUploadId());
        assertFalse(StringUtils.hasText(result.getErrorDetail()));
        assertFalse(StringUtils.hasText(result.getRejectReason()));
        assertNotNull(result.getExtractedData());

        JSONObject json = new JSONObject(result.getExtractedData());
        assertEquals("42", json.getJSONObject("customer").getJSONObject("age").getString("visualZone"));
        assertEquals("40", json.getJSONObject("customer").getJSONObject("age").getString("documentPortrait"));
        assertEquals("SPECIMEN", json.getJSONObject("customer").getJSONObject("surname").getString("visualZone"));
    }

    @Test
    void testSubmitDocument_handleCreateCustomerError() throws Exception {
        when(apiService.createCustomer()).thenReturn(Optional.of(new CreateCustomerResponse()));
        final OwnerId ownerId = new OwnerId();
        ownerId.setUserId("joe");
        ownerId.setActivationId("a123");

        final SubmittedDocument doc = new SubmittedDocument();
        doc.setType(DocumentType.PASSPORT);

        assertThrows(RemoteCommunicationException.class, () -> tested.submitDocuments(ownerId, List.of(doc)));
        verify(apiService).createCustomer();
    }

    @Test
    void testSubmitDocument_handleCreateDocumentError() throws Exception {
        when(apiService.createCustomer()).thenReturn(Optional.of(new CreateCustomerResponse("c123")));
        when(apiService.createDocument("c123", DocumentType.PASSPORT)).thenReturn(Optional.empty());
        final OwnerId ownerId = new OwnerId();
        ownerId.setUserId("joe");
        ownerId.setActivationId("a123");

        final SubmittedDocument doc = new SubmittedDocument();
        doc.setDocumentId("doc1");
        doc.setType(DocumentType.PASSPORT);

        assertThrows(RemoteCommunicationException.class, () -> tested.submitDocuments(ownerId, List.of(doc)));
        verify(apiService).createDocument("c123", DocumentType.PASSPORT);
    }

    @Test
    void testSubmitDocument_handleProvideDocumentPageError() throws Exception {
        when(apiService.createCustomer()).thenReturn(Optional.of(new CreateCustomerResponse("c123")));

        final Links docLink = new Links("docResource");
        final CreateDocumentResponse documentResponse = new CreateDocumentResponse();
        documentResponse.setLinks(docLink);
        when(apiService.createDocument("c123", DocumentType.PASSPORT)).thenReturn(Optional.of(documentResponse));

        final CreateDocumentPageResponse pageResponse = new CreateDocumentPageResponse(
                "front",
                CreateDocumentPageResponse.ErrorCodeEnum.NO_CARD_CORNERS_DETECTED,
                List.of(CreateDocumentPageResponse.WarningsEnum.DOCUMENT_TYPE_NOT_RECOGNIZED));
        when(apiService.provideDocumentPage("c123", CardSide.FRONT, "img".getBytes()))
                .thenReturn(Optional.of(pageResponse));

        final OwnerId ownerId = new OwnerId();
        ownerId.setUserId("joe");
        ownerId.setActivationId("a123");
        final SubmittedDocument doc = new SubmittedDocument();
        doc.setType(DocumentType.PASSPORT);
        doc.setSide(CardSide.FRONT);
        doc.setPhoto(Image.builder().data("img".getBytes()).build());

        final DocumentsSubmitResult results = tested.submitDocuments(ownerId, List.of(doc));
        verify(apiService).provideDocumentPage("c123", CardSide.FRONT, "img".getBytes());
        assertEquals(1, results.getResults().size());

        final DocumentSubmitResult result = results.getResults().get(0);
        assertEquals("c123", result.getUploadId());
        assertTrue(StringUtils.hasText(result.getRejectReason()));
    }

    @Test
    void testGetPhoto() throws Exception {
        when(apiService.getDocumentPortrait("c123")).thenReturn(Optional.of(new ImageCrop("img".getBytes())));

        final Image image = tested.getPhoto("c123");
        verify(apiService).getDocumentPortrait("c123");
        assertArrayEquals("img".getBytes(), image.getData());
    }

    @Test
    void testGetPhoto_notExists() throws Exception {
        when(apiService.getDocumentPortrait("c123")).thenReturn(Optional.empty());
        assertThrows(RemoteCommunicationException.class, () -> tested.getPhoto("c123"));
    }

    @Test
    void testParseRejectionReason() throws Exception {
        final DocumentResultEntity entity = new DocumentResultEntity();
        entity.setRejectReason(new ObjectMapper().writeValueAsString(List.of("Reason1", "Reason2")));
        assertEquals(List.of("Reason1", "Reason2"), tested.parseRejectionReasons(entity));
    }

    @Test
    void testParseEmptyRejectionReason() throws Exception {
        final DocumentResultEntity entity = new DocumentResultEntity();
        assertTrue(tested.parseRejectionReasons(entity).isEmpty());
    }

    @Test
    void testVerifyDocuments() throws Exception {
        final DocumentInspectResponse response = new DocumentInspectResponse();
        when(apiService.inspectDocument("c123")).thenReturn(Optional.of(response));

        final DocumentsVerificationResult result = tested.verifyDocuments(new OwnerId(), List.of("c123"));
        assertTrue(result.isAccepted());
        assertEquals("c123", result.getResults().get(0).getUploadId());
        assertNotNull(result.getVerificationId());
        assertNotNull(result.getResults().get(0).getVerificationResult());
    }

    @Test
    void testVerifyDocuments_expired() throws Exception {
        final DocumentInspectResponse response = new DocumentInspectResponse(true, null);
        when(apiService.inspectDocument("c123")).thenReturn(Optional.of(response));

        final DocumentsVerificationResult result = tested.verifyDocuments(new OwnerId(), List.of("c123"));
        assertEquals(DocumentVerificationStatus.REJECTED, result.getStatus());
        assertEquals(List.of("Document expired."), new ObjectMapper().readValue(result.getRejectReason(), new TypeReference<List<String>>() {}));
        assertEquals("c123", result.getResults().get(0).getUploadId());
        assertNotNull(result.getVerificationId());
    }

}
