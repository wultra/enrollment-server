/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2020 Wultra s.r.o.
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

package com.wultra.app.enrollmentserver.configuration;

import com.wultra.app.enrollmentserver.impl.util.ConditionalOnPropertyNotEmpty;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import io.getlime.push.client.PushServerClient;
import io.getlime.push.client.PushServerClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration regarding Push Service connectivity.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Configuration
@Slf4j
@ConditionalOnPropertyNotEmpty("powerauth.push.service.url")
public class PushServiceConfig {

    @Bean
    public PushServerClient pushServerClient(final PushServiceConfigProperties pushServiceProperties) throws PushServerClientException {
        final String url = pushServiceProperties.getUrl();
        logger.info("Configuring PushServerClient for URL: {}", url);
        final RestClientConfiguration restClientConfig = pushServiceProperties.getRestClientConfig();
        restClientConfig.setBaseUrl(url);
        return new PushServerClient(restClientConfig);
    }

}
