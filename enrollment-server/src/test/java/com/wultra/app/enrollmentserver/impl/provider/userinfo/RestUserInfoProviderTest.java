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
package com.wultra.app.enrollmentserver.impl.provider.userinfo;

import com.wultra.core.rest.client.base.RestClientConfiguration;
import io.getlime.security.powerauth.rest.api.spring.model.UserInfoContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link RestUserInfoProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class RestUserInfoProviderTest {

    private MockWebServer mockWebServer;

    private RestUserInfoProvider tested;

    @BeforeEach
    void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final RestClientConfiguration restClientConfig = new RestClientConfiguration();
        restClientConfig.setBaseUrl(mockWebServer.url("user-data-store").toString());
        tested = new RestUserInfoProvider(restClientConfig, Collections.emptySet());
    }

    @Test
    void testFetchUserClaimsForUserId() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setBody("""
                        {
                          "status": "OK",
                          "responseObject": {
                            "sub": "83692",
                            "name": "Petr Adams",
                            "email": "petr@example.com",
                            "birthdate": "1975-12-31",
                            "https://claims.example.com/department": "xxx"
                          }
                        }"""));

        final Map<String, Object> result = tested.fetchUserClaimsForUserId(UserInfoContext.builder()
                .userId("petr")
                .build());

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Petr Adams", result.get("name"));

        final RecordedRequest recordedRequest = mockWebServer.takeRequest(1L, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("GET /user-data-store/private/user-claims?userId=petr HTTP/1.1", recordedRequest.getRequestLine());
    }
}
