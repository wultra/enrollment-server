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

package com.wultra.app.onboardingserver.provider.zenid;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.VerificationSdkInfo;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.provider.zenid.model.api.ZenidWebInitSdkResponse;
import com.wultra.core.rest.client.base.RestClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ZenID document verification provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class ZenidDocumentVerificationProviderTest {

    @Mock
    private ZenidRestApiService zenidApiService;

    @InjectMocks
    private ZenidDocumentVerificationProvider provider;

    @Test
    void testInitVerificationSdk_providerAttributeNotPresent_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.initVerificationSdk(new OwnerId(), Map.of("sdk-init-token", "dummy-token"))
        );

        // then
        assertEquals("Requested unsupported provider 'null'", exception.getMessage());
    }

    @Test
    void testInitVerificationSdk_unsupportedProviderInAttributes_exceptionIsThrown() {
        // given
        // -

        // when
        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> provider.initVerificationSdk(new OwnerId(), Map.of("sdk-init-token", "dummy-token", "provider", "unsupported"))
        );

        // then
        assertEquals("Requested unsupported provider 'unsupported'", exception.getMessage());
    }

    @Test
    void testInitVerificationSdk_allMandatoryAttributesPresent_correctResponseIsReturned() throws RemoteCommunicationException, DocumentVerificationException, RestClientException {
        // given
        final var apiResponse = new ZenidWebInitSdkResponse();
        apiResponse.setResponse("dummy-response");

        when(zenidApiService.initSdk("dummy-token")).thenReturn(ResponseEntity.ok(apiResponse));

        // when
        final var response = provider.initVerificationSdk(new OwnerId(), Map.of("sdk-init-token", "dummy-token", "provider", "zenid"));

        // then
        final var expectedResponse = new VerificationSdkInfo(Map.of("provider", "zenid", "zenid-sdk-init-response", "dummy-response"));
        assertEquals(expectedResponse, response);
    }
}
