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

package com.wultra.app.onboardingserver.configuration;

import io.getlime.push.client.PushServerClient;
import io.getlime.push.client.PushServerClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration regarding Push Service connectivity.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Configuration
public class PushServiceConfig {

    private static final Logger logger = LoggerFactory.getLogger(PushServiceConfig.class);

    @Value("${powerauth.push.service.url:}")
    private String powerAuthPushServiceUrl;

    @Bean
    public PushServerClient pushServerClient() {
        try {
            return new PushServerClient(powerAuthPushServiceUrl);
        } catch (PushServerClientException ex) {
            // Log the error in case Rest client initialization failed
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

}
