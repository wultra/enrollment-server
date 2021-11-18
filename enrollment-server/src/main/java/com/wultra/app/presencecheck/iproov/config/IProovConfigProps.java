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
package com.wultra.app.presencecheck.iproov.config;

import com.wultra.app.presencecheck.iproov.model.api.ServerClaimRequest;
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
     * API key
     */
    private String apiKey;

    /**
     * API secret
     */
    private String apiSecret;

    /**
     * The assurance type of the claim
     */
    private ServerClaimRequest.AssuranceTypeEnum assuranceType;

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
        // prevent blank value which is invalid and potentially hard to catch
        this.riskProfile = Strings.isNotBlank(riskProfile) ? riskProfile : null;
    }

}
