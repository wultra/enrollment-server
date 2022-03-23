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
package com.wultra.app.docverify.zenid.config;

import com.wultra.app.docverify.zenid.model.api.ZenidSharedMineAllResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Zenid configuration properties.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.document-verification.provider", havingValue = "zenid")
@Configuration
@ConfigurationProperties(prefix = "enrollment-server.document-verification.zenid")
@Getter @Setter
public class ZenidConfigProps {

    /**
     * // TODO consider removing this config option
     * Enabled/disabled additional doc submit validations
     */
    private boolean additionalDocSubmitValidationsEnabled;

    /**
     * Enabled/disabled asynchronous processing
     */
    private boolean asyncProcessingEnabled;

    /**
     * Identifies expected document country
     */
    private ZenidSharedMineAllResult.DocumentCountryEnum documentCountry;

    /**
     * NTLM domain to authenticate within
     */
    private String ntlmDomain;

    /**
     * NTLM password
     */
    private String ntlmPassword;

    /**
     * NTLM username
     */
    private String ntlmUsername;

    /**
     * Service base URL
     */
    private String serviceBaseUrl;

    public void setNtlmDomain(String ntlmDomain) {
        // prevent blank value which is invalid and potentially hard to catch
        this.ntlmDomain = StringUtils.isNotBlank(ntlmDomain) ? ntlmDomain : null;
    }

}
