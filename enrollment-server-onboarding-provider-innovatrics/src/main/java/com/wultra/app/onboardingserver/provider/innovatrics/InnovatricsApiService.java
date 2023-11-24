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

import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Implementation of the REST service to<a href="https://www.innovatrics.com/">Innovatrics</a>.
 * <p>
 * It is not possible to combine Innovatrics with other providers such as iProov or ZenID.
 * Both providers, document verifier and presence check, must be configured to {@code innovatrics}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
        """)
@Service
@Slf4j
class InnovatricsApiService {

    private static final ParameterizedTypeReference<String> STRING_TYPE_REFERENCE = new ParameterizedTypeReference<>() { };

    /**
     * REST client for Innovatrics calls.
     */
    private final RestClient restClient;

    /**
     * Service constructor.
     *
     * @param restClient REST template for Innovatrics calls.
     */
    @Autowired
    public InnovatricsApiService(@Qualifier("restClientInnovatrics") final RestClient restClient) {
        this.restClient = restClient;
    }

    // TODO remove - temporal test call
    @PostConstruct
    public void testCall() throws RestClientException {
        logger.info("Trying a test call");
        final ResponseEntity<String> response = restClient.get("/api/v1/metadata", STRING_TYPE_REFERENCE);
        logger.info("Result of test call: {}", response.getBody());
    }
}
