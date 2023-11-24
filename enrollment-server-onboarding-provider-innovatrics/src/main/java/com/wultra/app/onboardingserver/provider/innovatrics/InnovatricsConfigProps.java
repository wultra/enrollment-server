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

import com.wultra.core.rest.client.base.RestClientConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Innovatrics configuration properties.
 * <p>
 * It is not possible to combine Innovatrics with other providers such as iProov or ZenID.
 * Both providers, document verifier and presence check, must be configured to {@code innovatrics}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnExpression("""
        '${enrollment-server-onboarding.presence-check.provider}' == 'innovatrics' and '${enrollment-server-onboarding.document-verification.provider}' == 'innovatrics'
        """)
@Configuration
@ConfigurationProperties(prefix = "enrollment-server-onboarding.provider.innovatrics")
@Getter @Setter
class InnovatricsConfigProps {

    /**
     * Service base URL.
     */
    private String serviceBaseUrl;

    /**
     * Authentication for Innovatrics.
     */
    private String serviceToken;

    /**
     * Identification of the application calling the REST services passed as the User-Agent header.
     */
    private String serviceUserAgent;

    /**
     * REST client configuration.
     */
    private RestClientConfiguration restClientConfig;

}
