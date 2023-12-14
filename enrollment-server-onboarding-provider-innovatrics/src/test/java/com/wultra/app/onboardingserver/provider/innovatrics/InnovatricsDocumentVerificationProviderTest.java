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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

import java.util.List;

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
        final OwnerId ownerId = createOwnerId();
        when(apiService.createCustomer(ownerId)).thenReturn(new CreateCustomerResponse("c123"));

        final Links docLink = new Links("docResource");
        final CreateDocumentResponse documentResponse = new CreateDocumentResponse();
        documentResponse.setLinks(docLink);
        when(apiService.createDocument("c123", DocumentType.PASSPORT, ownerId)).thenReturn(documentResponse);

        final CreateDocumentPageResponse pageResponse = new CreateDocumentPageResponse();
        when(apiService.provideDocumentPage("c123", CardSide.FRONT, "img".getBytes(), ownerId)).thenReturn(pageResponse);

        final BiometricMultiValueAttribute ageAttr = new BiometricMultiValueAttribute("42", null, null, null, "40");
        final MultiValueAttribute surnameAttr = new MultiValueAttribute("SPECIMEN", null, null, null);
        final Customer customer = new Customer();
        customer.age(ageAttr).setSurname(surnameAttr);
        final GetCustomerResponse customerResponse = new GetCustomerResponse();
        customerResponse.customer(customer);
        when(apiService.getCustomer("c123", ownerId)).thenReturn(customerResponse);

        final SubmittedDocument doc = new SubmittedDocument();
        doc.setType(DocumentType.PASSPORT);
        doc.setSide(CardSide.FRONT);
        doc.setPhoto(Image.builder().data("img".getBytes()).build());

        final DocumentsSubmitResult results = tested.submitDocuments(ownerId, List.of(doc));
        verify(apiService).getCustomer("c123", ownerId);
        assertEquals(1, results.getResults().size());

        final DocumentSubmitResult result = results.getResults().get(0);
        assertEquals("c123", result.getUploadId());
        assertFalse(StringUtils.hasText(result.getErrorDetail()));
        assertFalse(StringUtils.hasText(result.getRejectReason()));
        assertNotNull(result.getExtractedData());

        assertEquals("42", JsonPath.read(result.getExtractedData(), "$.customer.age.visualZone"));
        assertEquals("40", JsonPath.read(result.getExtractedData(), "$.customer.age.documentPortrait"));
        assertEquals("SPECIMEN", JsonPath.read(result.getExtractedData(), "$.customer.surname.visualZone"));
    }

    @Test
    void testSubmitDocument_handleProvideDocumentPageError() throws Exception {
        final OwnerId ownerId = createOwnerId();
        when(apiService.createCustomer(ownerId)).thenReturn(new CreateCustomerResponse("c123"));

        final Links docLink = new Links("docResource");
        final CreateDocumentResponse documentResponse = new CreateDocumentResponse();
        documentResponse.setLinks(docLink);
        when(apiService.createDocument("c123", DocumentType.PASSPORT, ownerId)).thenReturn(documentResponse);

        final CreateDocumentPageResponse pageResponse = new CreateDocumentPageResponse(
                "front",
                CreateDocumentPageResponse.ErrorCodeEnum.NO_CARD_CORNERS_DETECTED,
                List.of(CreateDocumentPageResponse.WarningsEnum.DOCUMENT_TYPE_NOT_RECOGNIZED));
        when(apiService.provideDocumentPage("c123", CardSide.FRONT, "img".getBytes(), ownerId)).thenReturn(pageResponse);

        final SubmittedDocument doc = new SubmittedDocument();
        doc.setType(DocumentType.PASSPORT);
        doc.setSide(CardSide.FRONT);
        doc.setPhoto(Image.builder().data("img".getBytes()).build());

        final DocumentsSubmitResult results = tested.submitDocuments(ownerId, List.of(doc));
        verify(apiService).provideDocumentPage("c123", CardSide.FRONT, "img".getBytes(), ownerId);
        assertEquals(1, results.getResults().size());

        final DocumentSubmitResult result = results.getResults().get(0);
        assertEquals("c123", result.getUploadId());
        assertTrue(StringUtils.hasText(result.getRejectReason()));
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
        final OwnerId ownerId = createOwnerId();
        final DocumentInspectResponse response = new DocumentInspectResponse();
        when(apiService.inspectDocument("c123", ownerId)).thenReturn(response);

        final DocumentsVerificationResult result = tested.verifyDocuments(ownerId, List.of("c123"));
        assertTrue(result.isAccepted());
        assertEquals("c123", result.getResults().get(0).getUploadId());
        assertNotNull(result.getVerificationId());
        assertNotNull(result.getResults().get(0).getVerificationResult());
    }

    @Test
    void testVerifyDocuments_expired() throws Exception {
        final OwnerId ownerId = createOwnerId();
        final DocumentInspectResponse response = new DocumentInspectResponse(true, null);
        when(apiService.inspectDocument("c123", ownerId)).thenReturn(response);

        final DocumentsVerificationResult result = tested.verifyDocuments(ownerId, List.of("c123"));
        assertEquals(DocumentVerificationStatus.REJECTED, result.getStatus());
        assertEquals(List.of("Document expired."), new ObjectMapper().readValue(result.getRejectReason(), new TypeReference<List<String>>() {}));
        assertEquals("c123", result.getResults().get(0).getUploadId());
        assertNotNull(result.getVerificationId());
    }

    private OwnerId createOwnerId() {
        final OwnerId ownerId = new OwnerId();
        ownerId.setUserId("joe");
        ownerId.setActivationId("a123");
        return ownerId;
    }

}
