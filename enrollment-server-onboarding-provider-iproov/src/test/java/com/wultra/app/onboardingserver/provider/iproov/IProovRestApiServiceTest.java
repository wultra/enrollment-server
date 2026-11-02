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
package com.wultra.app.onboardingserver.provider.iproov;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.core.rest.client.base.RestClient;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.StartStop;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Test of {@link IProovRestApiService}.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
class IProovRestApiServiceTest {

    @StartStop
    private final MockWebServer mockWebServer = new MockWebServer();

    private IProovRestApiService tested;

    @BeforeEach
    void setup() {
        final IProovConfigProps configProps = new IProovConfigProps();
        configProps.setServiceBaseUrl(mockWebServer.url("/api/v2").toString());
        tested = new IProovRestApiService(configProps, mock(RestClient.class), WebClient.create());
    }

    @Test
    void testDeleteUserIfAlreadyExists_userNotExists() throws Exception {
        mockWebServer.enqueue(jsonResponse(HttpStatus.BAD_REQUEST, """
                {"error": "invalid_user_id", "error_description": "User not found"}
                """));

        tested.deleteUserIfAlreadyExists(createOwnerId());

        assertRequest(HttpMethod.GET);
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testDeleteUserIfAlreadyExists_userDeleted() throws Exception {
        mockWebServer.enqueue(jsonResponse(HttpStatus.OK, "{}"));
        mockWebServer.enqueue(jsonResponse(HttpStatus.OK, "{}"));

        tested.deleteUserIfAlreadyExists(createOwnerId());

        assertRequest(HttpMethod.GET);
        assertRequest(HttpMethod.DELETE);
    }

    @Test
    void testDeleteUserIfAlreadyExists_deleteFailed() throws Exception {
        mockWebServer.enqueue(jsonResponse(HttpStatus.OK, "{}"));
        mockWebServer.enqueue(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        final OwnerId ownerId = createOwnerId();
        assertThrows(RemoteCommunicationException.class, () -> tested.deleteUserIfAlreadyExists(ownerId));

        assertRequest(HttpMethod.GET);
        assertRequest(HttpMethod.DELETE);
    }

    @Test
    void testDeleteUserIfAlreadyExists_getUserFailed() throws Exception {
        mockWebServer.enqueue(jsonResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        final OwnerId ownerId = createOwnerId();
        assertThrows(RemoteCommunicationException.class, () -> tested.deleteUserIfAlreadyExists(ownerId));

        assertRequest(HttpMethod.GET);
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testDeleteUserIfAlreadyExists_unexpectedErrorCode() {
        mockWebServer.enqueue(jsonResponse(HttpStatus.BAD_REQUEST, """
                {"error": "invalid_key"}
                """));

        final OwnerId ownerId = createOwnerId();
        assertThrows(RemoteCommunicationException.class, () -> tested.deleteUserIfAlreadyExists(ownerId));

        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testDeleteUserIfAlreadyExists_emptyBadRequestBody() {
        mockWebServer.enqueue(new MockResponse.Builder().code(HttpStatus.BAD_REQUEST.value()).build());

        final OwnerId ownerId = createOwnerId();
        assertThrows(RemoteCommunicationException.class, () -> tested.deleteUserIfAlreadyExists(ownerId));

        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void ensureValidUserIdValueTest() {
        assertThrows(IllegalArgumentException.class, () ->
                IProovRestApiService.ensureValidUserIdValue("invalidChars,[="));

        String userIdTooLong = RandomStringUtils.randomAlphabetic(IProovRestApiService.USER_ID_MAX_LENGTH + 1);
        String userIdEnsured = IProovRestApiService.ensureValidUserIdValue(userIdTooLong);
        assertEquals(IProovRestApiService.USER_ID_MAX_LENGTH, userIdEnsured.length());
    }

    private void assertRequest(final HttpMethod expectedMethod) throws InterruptedException {
        final RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request, "Expected request not received");
        assertEquals(expectedMethod.name(), request.getMethod());
        assertEquals("/api/v2/users/" + createOwnerId().getUserIdSecured(), request.getTarget());
    }

    private static MockResponse jsonResponse(final HttpStatus status, final String body) {
        return new MockResponse.Builder()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .code(status.value())
                .build();
    }

    private static OwnerId createOwnerId() {
        final OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("a1");
        ownerId.setUserId("u1");
        return ownerId;
    }
}
