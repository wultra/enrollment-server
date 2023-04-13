/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.docverify.zenid.config;

import com.wultra.app.onboardingserver.docverify.zenid.model.api.ZenidSharedMineAllResult;
import com.wultra.core.rest.client.base.RestClientConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

/**
 * Zenid configuration properties.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.document-verification.provider", havingValue = "zenid")
@Configuration
@ConfigurationProperties(prefix = "enrollment-server-onboarding.document-verification.zenid")
@Getter @Setter
public class ZenidConfigProps {

    /**
     * // TODO consider removing this config option
     * Enabled/disabled additional doc submit validations
     */
    private boolean additionalDocSubmitValidationsEnabled;

    /**
     * API key
     */
    private String apiKey;

    /**
     * Enabled/disabled asynchronous processing
     */
    private boolean asyncProcessingEnabled;

    /**
     * Identifies expected document country
     */
    private ZenidSharedMineAllResult.DocumentCountryEnum documentCountry;

    /**
     * Optional name of profile.
     */
    private String profile;

    /**
     * Service base URL
     */
    private String serviceBaseUrl;

    /**
     * Identification of the application calling the REST services passed as the User-Agent header
     */
    private String serviceUserAgent;

    /**
     * REST client configuration
     */
    private RestClientConfiguration restClientConfig;

    /**
     * Return profile name.
     *
     * @return profile name
     */
    public Optional<String> getProfile() {
        return Optional.ofNullable(profile);
    }

    @PostConstruct
    void validate() {
        // TODO (racansky, 2022-11-03, #495) DocumentVerificationFinalAction works only in sync mode
        Assert.state(!asyncProcessingEnabled, "ZenId async functionality is not fully supported yet");
    }
}
