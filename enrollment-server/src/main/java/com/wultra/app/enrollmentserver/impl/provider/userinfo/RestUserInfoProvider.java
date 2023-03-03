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

import com.wultra.core.rest.client.base.DefaultRestClient;
import com.wultra.core.rest.client.base.RestClient;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import com.wultra.core.rest.client.base.RestClientException;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.rest.api.spring.model.UserInfoContext;
import io.getlime.security.powerauth.rest.api.spring.provider.UserInfoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.Map;

/**
 * REST specialization of {@link UserInfoProvider}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Slf4j
public class RestUserInfoProvider implements UserInfoProvider {

    private static final MultiValueMap<String, String> EMPTY_HEADERS = new LinkedMultiValueMap<>();

    private final RestClient restClient;

    public RestUserInfoProvider(final RestClientConfiguration restClientConfig) throws RestClientException {
        restClient = new DefaultRestClient(restClientConfig);
    }

    @Override
    public Map<String, Object> fetchUserClaimsForUserId(final UserInfoContext context) {
        final String userId = context.getUserId();
        logger.info("Fetching claims of user ID: {}", userId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("userId", userId);

        try {
            final var response = restClient.getObject("/private/user-claims", queryParams, EMPTY_HEADERS, Map.class);
            if (!Response.Status.OK.equals(response.getStatus())) {
                logger.warn("Unable to fetch claims of user ID: {}, status: {}", userId, response.getStatus());
                return Collections.emptyMap();
            }

            logger.info("Fetched claims of user ID: {}", userId);
            @SuppressWarnings("unchecked")
            final Map<String, Object> claims = response.getResponseObject();
            if (claims == null) {
                logger.warn("Response claims to fetch claims of user ID: {} is null", userId);
                return Collections.emptyMap();
            }
            logger.debug("Fetched claims of user ID: {}, {}", userId, claims);
            return claims;
        } catch (RestClientException e) {
            logger.warn("Unable to fetch claims of user ID: {}", userId, e);
            return Collections.emptyMap();
        }
    }
}
