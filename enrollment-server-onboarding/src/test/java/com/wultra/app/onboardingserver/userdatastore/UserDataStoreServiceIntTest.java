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

package com.wultra.app.onboardingserver.userdatastore;

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.request.EmbeddedPhotoCreateRequest;
import okhttp3.Credentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for {@link UserDataStoreService}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
class UserDataStoreServiceIntTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private UserDataStoreService tested;

    @BeforeAll
    static void beforeAll() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add(
                "enrollment-server-onboarding.user-data-store.rest-client-config.base-url",
                () -> mockWebServer.url("/user-data-store").toString()
        );
    }
    @Test
    void testCreateDocument() throws Exception {
        // given
        final var request = buildRequest();
        mockWebServer.enqueue(buildResponse());

        // when
        tested.callCreateDocument(request);

        // then
        final var recordedRequest = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertRecordedRequest(recordedRequest);
    }

    private static DocumentCreateRequest buildRequest() {
        final var documentData = """
                {
                    "surname": "Doe",
                    "givenNames": "John",
                    "dateOfBirth": "1997-06-14",
                    "placeOfBirth": "Ostrava",
                    "sex": "M",
                    "nationality": "Czech",
                    "personalNumber": "123456789",
                    "documentNumber": "AB123456",
                    "dateOfIssue": "2019-12-27",
                    "dateOfExpiry": "2029-11-30",
                    "authority": "City Hall",
                    "country": "CZE"
                }""";

        return DocumentCreateRequest.builder()
                .userId("admin")
                .documentType("personal_id")
                .dataType("claims")
                .externalId("test-process-1")
                .documentData(documentData)
                .attributes(Map.of("trustedImage", true))
                .photos(List.of(
                        EmbeddedPhotoCreateRequest.builder()
                                .photoType("person")
                                .photoData("ZmFjZVBob3Rv")
                                .externalId("1")
                                .build(),
                        EmbeddedPhotoCreateRequest.builder()
                                .photoType("document_front_side")
                                .photoData("aWRDYXJkRnJvbnQ=")
                                .externalId("2")
                                .build(),
                        EmbeddedPhotoCreateRequest.builder()
                                .photoType("document_back_side")
                                .photoData("aWRDYXJkQmFjaw==")
                                .externalId("3")
                                .build()
                ))
                .build();
    }

    private static MockResponse buildResponse() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "status": "OK",
                            "responseObject": {
                                "id": "16f63774-2c6d-4827-be4b-7b8cc066d971",
                                "documentDataId": null,
                                "photos": [
                                    {
                                        "id": "22f2ac50-1caa-433b-8f20-692089dcec42"
                                    },
                                    {
                                        "id": "d065a2f2-fc3d-4476-b960-85e58ab8a3f4"
                                    },
                                    {
                                        "id": "d334f7b1-2a50-4ab3-8558-22fc7f0a6c29"
                                    }
                                ],
                                "attachments": []
                            }
                        }""");
    }

    private static void assertRecordedRequest(final RecordedRequest recordedRequest) throws JSONException {
        assertNotNull(recordedRequest);

        assertEquals("POST", recordedRequest.getMethod());

        final var expectedAuth = Credentials.basic("user", "password", StandardCharsets.UTF_8);
        final var actualAuth = recordedRequest.getHeader("Authorization");
        assertEquals(expectedAuth, actualAuth);

        final var body = recordedRequest.getBody().readUtf8();
        JSONAssert.assertEquals(buildExpectedRequestBody(), body, false);
    }

    private static String buildExpectedRequestBody() {
        return """
            {
                "requestObject": {
                  "userId": "admin",
                  "documentType": "personal_id",
                  "dataType": "claims",
                  "externalId": "test-process-1",
                  "documentData": "{\\n    \\"surname\\": \\"Doe\\",\\n    \\"givenNames\\": \\"John\\",\\n    \\"dateOfBirth\\": \\"1997-06-14\\",\\n    \\"placeOfBirth\\": \\"Ostrava\\",\\n    \\"sex\\": \\"M\\",\\n    \\"nationality\\": \\"Czech\\",\\n    \\"personalNumber\\": \\"123456789\\",\\n    \\"documentNumber\\": \\"AB123456\\",\\n    \\"dateOfIssue\\": \\"2019-12-27\\",\\n    \\"dateOfExpiry\\": \\"2029-11-30\\",\\n    \\"authority\\": \\"City Hall\\",\\n    \\"country\\": \\"CZE\\"\\n}",
                  "attributes": {
                    "trustedImage": true
                  },
                  "photos": [
                    { "photoType": "person", "externalId": "1" },
                    { "photoType": "document_front_side", "externalId": "2" },
                    { "photoType": "document_back_side", "externalId": "3" }
                  ]
                }
            }""";
    }
}
