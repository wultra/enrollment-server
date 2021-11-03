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
package com.wultra.app.docverify.zenid.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
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
     * Enabled/disabled asynchronous processing
     */
    private boolean asyncProcessingEnabled;

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
        this.ntlmDomain = Strings.isNotBlank(ntlmDomain) ? ntlmDomain : null;
    }

}
