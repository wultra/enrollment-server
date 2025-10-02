/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
import com.wultra.security.powerauth.client.model.response.GetApplicationConfigResponse;
import com.wultra.security.powerauth.client.v4.PowerAuthClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Test for {@link PowerAuthActivationCodeHandler}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class PowerAuthActivationCodeHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PowerAuthClient powerAuthClient;

    @InjectMocks
    private PowerAuthActivationCodeHandler tested;

    @Test
    void testFetchTransferConfiguration_spawn() throws Exception {
        final GetApplicationConfigResponse response = createResponse();

        when(powerAuthClient.getApplicationConfig("source-1"))
                .thenReturn(response);

        final var request = DelegatingActivationCodeHandler.TransferConfigurationRequest.builder()
                .targetApplicationId("target-1")
                .sourceApplicationId("source-1")
                .build();

        final var result = tested.fetchTransferConfiguration(request);

        assertEquals("target-1", result.applicationId());
        assertEquals(DelegatingActivationCodeHandler.ActivationTransferType.SPAWN, result.type());
        assertEquals(List.of("foo"), result.initialFlags());
    }

    @Test
    void testFetchTransferConfiguration_move() throws Exception {
        final GetApplicationConfigResponse response = createResponse();

        when(powerAuthClient.getApplicationConfig("source-1"))
                .thenReturn(response);

        final var request = DelegatingActivationCodeHandler.TransferConfigurationRequest.builder()
                .targetApplicationId("target-4")
                .sourceApplicationId("source-1")
                .build();

        final var result = tested.fetchTransferConfiguration(request);

        assertEquals("target-4", result.applicationId());
        assertEquals(DelegatingActivationCodeHandler.ActivationTransferType.MOVE, result.type());
        assertNull(result.initialFlags());
    }

    @Test
    void testFetchTransferConfiguration_missingType() throws Exception {
        final GetApplicationConfigResponse response = createResponse();

        when(powerAuthClient.getApplicationConfig("source-1"))
                .thenReturn(response);

        final var request = DelegatingActivationCodeHandler.TransferConfigurationRequest.builder()
                .targetApplicationId("target-6")
                .sourceApplicationId("source-1")
                .build();

        final var result = tested.fetchTransferConfiguration(request);

        assertNull(result);
    }

    @Test
    void testFetchTransferConfiguration_invalidTarget() throws Exception {
        final GetApplicationConfigResponse response = createResponse();

        when(powerAuthClient.getApplicationConfig("source-1"))
                .thenReturn(response);

        final var request = DelegatingActivationCodeHandler.TransferConfigurationRequest.builder()
                .targetApplicationId("target-3")
                .sourceApplicationId("source-1")
                .build();

        final var result = tested.fetchTransferConfiguration(request);

        assertNull(result);
    }

    private GetApplicationConfigResponse createResponse() throws JsonProcessingException {
        final String json = """
                {
                  "applicationId": "source-1",
                  "applicationConfigs": [
                    {
                      "key": "activation_transfer",
                      "values": [
                        {
                          "allowedTargetApplicationIds": [
                            "target-1",
                            "target-2"
                          ],
                          "initialFlags": ["foo"],
                          "type": "SPAWN"
                        },
                        {
                          "allowedTargetApplicationIds": [
                            "target-4",
                            "target-5"
                          ],
                          "type": "MOVE"
                        },
                        {
                          "allowedTargetApplicationIds": [
                            "target-6"
                          ]
                        },
                        {
                          "type": "SPAWN"
                        }
                      ]
                    }
                  ]
                }""";

        return objectMapper.readValue(json, GetApplicationConfigResponse.class);
    }
}
