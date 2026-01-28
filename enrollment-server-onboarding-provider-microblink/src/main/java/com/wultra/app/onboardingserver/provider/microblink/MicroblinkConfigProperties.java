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
package com.wultra.app.onboardingserver.provider.microblink;

import com.wultra.core.rest.client.base.RestClientConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Microblink document verification provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ConfigurationProperties(prefix = "enrollment-server-onboarding.document-verification.microblink")
@Getter
@Setter
public class MicroblinkConfigProperties {

    /**
     * REST client configuration
     */
    private RestClientConfiguration restClientConfig;

    /**
     * Check of extracted data from documents.
     */
    private boolean extractedDataCheckEnabled;

    /**
     * Configurations for mobile SDK.
     */
    private List<SdkConfig> mobileSdkConfigs = new ArrayList<>();

    /**
     * Microblink mobile SDK configuration.
     *
     * @param source Bundle ID / App ID
     * @param platform mobile platform
     * @param licenseKey license key for mobile SDK
     */
    public record SdkConfig(
            String source,
            String platform,
            String licenseKey
    ) {}
}
