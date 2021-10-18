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
package com.wultra.app.enrollmentserver.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Identity verification configuration.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Configuration
public class IdentityVerificationConfig {

    @Value("${enrollment-server.identity-verification.data-retention.hours:1}")
    private int dataRetentionTime;

    @Value("${enrollment-server.identity-verification.expiration.seconds:300}")
    private int verificationExpirationTime;

    /**
     * Get data retention time in hours.
     * @return Data retention time in hours.
     */
    public int getDataRetentionTime() {
        return dataRetentionTime;
    }

    /**
     * Get process expiration time in seconds.
     * @return Process expiration time in seconds.
     */
    public int getProcessExpirationTime() {
        return verificationExpirationTime;
    }
}