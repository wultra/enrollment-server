/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.wultra.app.enrollmentserver.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class used for setting up Swagger documentation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO (racansky, 2022-09-30, #408) should be extracted to db or make it reloadable to change configuration at runtime
@Configuration
@ConfigurationProperties("enrollment-server.mobile-application")
@Getter
public class MobileApplicationConfigurationProperties {

    private final VersionSpecification iOs = new VersionSpecification();

    private final VersionSpecification android = new VersionSpecification();

    @Getter
    @Setter
    public static class VersionSpecification {

        private String minimalVersion;

        private String currentVersion;
    }
}
