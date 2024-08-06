/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2024 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wultra.app.enrollmentserver.api.model.enrollment.response.OidcApplicationConfigurationResponse;
import com.wultra.app.enrollmentserver.errorhandling.PowerAuthApplicationConfigurationException;
import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.request.GetApplicationConfigRequest;
import com.wultra.security.powerauth.client.model.request.LookupApplicationByAppKeyRequest;
import com.wultra.security.powerauth.client.model.response.GetApplicationConfigResponse;
import com.wultra.security.powerauth.client.model.response.LookupApplicationByAppKeyResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ApplicationConfigurationService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationServiceTest {

    @Mock
    private PowerAuthClient powerAuthClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ApplicationConfigurationService tested;

    @Test
    void testFetchOidcApplicationConfiguration() throws Exception {
        final LookupApplicationByAppKeyRequest lookupRequest = new LookupApplicationByAppKeyRequest();
        lookupRequest.setApplicationKey("AIsOlIghnLztV2np3SANnQ==");


        final LookupApplicationByAppKeyResponse lookupResponse = new LookupApplicationByAppKeyResponse();
        lookupResponse.setApplicationId("application-1");

        when(powerAuthClient.lookupApplicationByAppKey(lookupRequest))
                .thenReturn(lookupResponse);

        final GetApplicationConfigRequest configRequest = new GetApplicationConfigRequest();
        configRequest.setApplicationId("application-1");

        final GetApplicationConfigResponse configResponse = createResponse();
        when(powerAuthClient.getApplicationConfig(configRequest))
                .thenReturn(configResponse);

        final OidcApplicationConfigurationResponse result = tested.fetchOidcApplicationConfiguration(ApplicationConfigurationService.OidcQuery.builder()
                .applicationKey("AIsOlIghnLztV2np3SANnQ==")
                .providerId("xyz999")
                .build());

        assertEquals("xyz999", result.getProviderId());
        assertEquals("jabberwocky", result.getClientId());
        assertEquals(List.of("openid"), result.getScopes());
        assertEquals("https://redirect.example.com", result.getRedirectUri());
        assertEquals("https://authorize.example.com", result.getAuthorizeUri());
    }

    @Test
    void testFetchOidcApplicationConfiguration_invalidProviderId() throws Exception {
        final LookupApplicationByAppKeyRequest lookupRequest = new LookupApplicationByAppKeyRequest();
        lookupRequest.setApplicationKey("AIsOlIghnLztV2np3SANnQ==");


        final LookupApplicationByAppKeyResponse lookupResponse = new LookupApplicationByAppKeyResponse();
        lookupResponse.setApplicationId("application-1");

        when(powerAuthClient.lookupApplicationByAppKey(lookupRequest))
                .thenReturn(lookupResponse);

        final GetApplicationConfigRequest configRequest = new GetApplicationConfigRequest();
        configRequest.setApplicationId("application-1");

        final GetApplicationConfigResponse configResponse = createResponse();
        when(powerAuthClient.getApplicationConfig(configRequest))
                .thenReturn(configResponse);

        final Exception e = assertThrows(PowerAuthApplicationConfigurationException.class, () -> tested.fetchOidcApplicationConfiguration(ApplicationConfigurationService.OidcQuery.builder()
                .applicationKey("AIsOlIghnLztV2np3SANnQ==")
                .providerId("non-existing")
                .build()));

        assertEquals("Fetching application configuration failed, application ID: application-1, provider ID: non-existing", e.getMessage());
    }

    private GetApplicationConfigResponse createResponse() throws JsonProcessingException {
        final String json = """
                {
                   "applicationId": "application-1",
                   "applicationConfigs": [
                     {
                       "key": "oauth2_providers",
                       "values": [
                         {
                           "providerId": "abc123",
                           "clientId": "1234567890abcdef",
                           "clientSecret": "top secret",
                           "scopes": [
                             "openid"
                           ],
                           "authorizeUri": "https://...",
                           "redirectUri": "https://...",
                           "tokenUri": "https://...",
                           "userInfoUri": "https://..."
                         },
                         {
                           "providerId": "xyz999",
                           "clientId": "jabberwocky",
                           "clientSecret": "top secret",
                           "scopes": [
                             "openid"
                           ],
                           "authorizeUri": "https://authorize.example.com",
                           "redirectUri": "https://redirect.example.com",
                           "tokenUri": "https://...",
                           "userInfoUri": "https://..."
                         }
                       ]
                     }
                   ]
                 }
                """;

        return objectMapper.readValue(json, GetApplicationConfigResponse.class);
    }
}
