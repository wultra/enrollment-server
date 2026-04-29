/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

package com.wultra.app.onboardingserver.impl.service.userdatastore;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for User Data Store when integration is disabled.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Configuration
@Slf4j
class UserDataStoreNoOpConfig {

    @ConditionalOnMissingBean(UserDataStoreService.class)
    @Bean
    public UserDataStoreService userDataStoreService() {
        logger.info("Initializing NoOpUserDataStoreService.");
        return new NoOpUserDataStoreService();
    }
}

