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

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CreateCustomerResponse;
import com.wultra.app.onboardingserver.provider.innovatrics.model.api.CreateDocumentPageResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test of {@link InnovatricsApiService}.
 *
 * @author Jan Pesek, jan.pesek@wultra.com
 */
@SpringBootTest(
        classes = EnrollmentServerTestApplication.class,
        properties = {
                "enrollment-server-onboarding.provider.innovatrics.serviceBaseUrl=http://localhost:" + InnovatricsRestApiServiceTest.PORT
        })
@ActiveProfiles("test")
class InnovatricsRestApiServiceTest {

    static final int PORT = 52936;

    @Autowired
    private InnovatricsApiService tested;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(PORT);
    }

    @AfterEach
    void cleanup() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testCreateCustomer() throws Exception {
        final OwnerId ownerId = createOwnerId();
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                // Real response to POST /api/v1/customers
                .setBody("""
                        {
                            "id": "c2e91b1f-0ccb-4ba0-93ae-d255a2a443af",
                            "links": {
                                "self": "/api/v1/customers/c2e91b1f-0ccb-4ba0-93ae-d255a2a443af"
                            }
                        }
                        """)
                .setResponseCode(HttpStatus.OK.value()));

        final CreateCustomerResponse response = tested.createCustomer(ownerId);
        assertEquals("c2e91b1f-0ccb-4ba0-93ae-d255a2a443af", response.getId());
        assertEquals("/api/v1/customers/c2e91b1f-0ccb-4ba0-93ae-d255a2a443af", response.getLinks().getSelf());
    }

    @Test
    void testErrorResponse() {
        final OwnerId ownerId = createOwnerId();
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                // Real response to uploading a page without previous document resource creation
                .setBody("""
                        {
                            "errorCode": "NOT_FOUND",
                            "errorMessage": "string"
                        }
                        """)
                .setResponseCode(500));

        assertThrows(RemoteCommunicationException.class, () -> tested.createCustomer(ownerId));
    }

    @Test
    void testNonMatchingPageType() throws Exception {
        final OwnerId ownerId = createOwnerId();
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                // Real response to uploading a second page that is different from the first one
                .setBody("""
                        {
                            "errorCode": "PAGE_DOESNT_MATCH_DOCUMENT_TYPE_OF_PREVIOUS_PAGE"
                        }
                        """)
                .setResponseCode(HttpStatus.OK.value()));

        final CreateDocumentPageResponse response = tested.provideDocumentPage("123", CardSide.FRONT, "data".getBytes(), ownerId);
        assertNotNull(response.getErrorCode());

        final RecordedRequest recordedRequest = mockWebServer.takeRequest(1L, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("PUT /api/v1/customers/123/document/pages HTTP/1.1", recordedRequest.getRequestLine());
    }

    private OwnerId createOwnerId() {
        final OwnerId ownerId = new OwnerId();
        ownerId.setUserId("joe");
        ownerId.setActivationId("a123");
        return ownerId;
    }

}
