/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.app.presencecheck.iproov.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * iProov configuration properties.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.presence-check.provider", havingValue = "iproov")
@Configuration
@ConfigurationProperties(prefix = "enrollment-server.presence-check.iproov")
@Getter @Setter
public class IProovConfigProps {

    /**
     * API secret
     */
    private String apiKey;

    /**
     * API key
     */
    private String apiSecret;

    /**
     * The pre-defined risk profile to use (optional value)
     */
    private String riskProfile;

    /**
     * Service base URL
     */
    private String serviceBaseUrl;

    /**
     * Service hostname
     */
    private String serviceHostname;

    public void setRiskProfile(String riskProfile) {
        this.riskProfile = Strings.isNotBlank(riskProfile) ? riskProfile : null;
    }

}
