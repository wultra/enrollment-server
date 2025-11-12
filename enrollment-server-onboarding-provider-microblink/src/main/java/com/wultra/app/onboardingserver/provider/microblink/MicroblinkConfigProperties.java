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

import java.time.Duration;
import java.util.EnumMap;

/**
 * Configuration properties for Microblink document verification provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ConfigurationProperties(prefix = "enrollment-server-onboarding.document-verification.microblink")
@Getter
@Setter
public class MicroblinkConfigProperties {

    public static final String DOCUMENTS_CACHE_NAME = "microblinkDocumentsCache";
    public static final String PHOTO_CACHE_NAME = "microblinkPhotoCache";

    /**
     * Record time-to-live in the cache after it is written
     */
    private Duration cacheRecordTTL = Duration.ofHours(1);

    /**
     * REST client configuration
     */
    private RestClientConfiguration restClientConfig;

    /**
     * Mobile SDK license keys by platform.
     */
    private EnumMap<MicroblinkMobilePlatform, String> mobileSdkLicenseKeys = new EnumMap<>(MicroblinkMobilePlatform.class);
}
