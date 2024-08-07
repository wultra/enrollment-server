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

import com.wultra.security.powerauth.client.PowerAuthClient;
import com.wultra.security.powerauth.client.model.error.PowerAuthClientException;
import com.wultra.security.powerauth.rest.client.PowerAuthRestClient;
import com.wultra.security.powerauth.rest.client.PowerAuthRestClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * PowerAuth service configuration class.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
@Configuration
@ConfigurationProperties("ext")
@ComponentScan(basePackages = {
        "com.wultra.app.onboardingserver.docverify",
        "com.wultra.app.onboardingserver.presencecheck",
        "com.wultra.security.powerauth",
        "io.getlime.security.powerauth",
})
public class PowerAuthWebServiceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PowerAuthWebServiceConfiguration.class);

    @Value("${powerauth.service.url}")
    private String powerAuthServiceUrl;

    @Value("${powerauth.service.restClientConfig.responseTimeout}")
    private Duration powerAuthServiceTimeout;

    @Value("${powerauth.service.restClientConfig.maxIdleTime}")
    private Duration powerAuthServiceMaxIdleTime;

    @Value("${powerauth.service.security.clientToken}")
    private String clientToken;

    @Value("${powerauth.service.security.clientSecret}")
    private String clientSecret;

    @Bean
    public PowerAuthClient powerAuthClient() throws PowerAuthClientException {
        final PowerAuthRestClientConfiguration config = new PowerAuthRestClientConfiguration();
        config.setResponseTimeout(powerAuthServiceTimeout);
        config.setMaxIdleTime(powerAuthServiceMaxIdleTime);

        if (StringUtils.hasText(clientToken)) {
            logger.info("Configuring security for PowerAuthRestClient.");
            config.setPowerAuthClientToken(clientToken);
            config.setPowerAuthClientSecret(clientSecret);
        }

        return new PowerAuthRestClient(powerAuthServiceUrl, config);
    }

}