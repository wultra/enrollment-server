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
package com.wultra.app.onboardingserver.impl.service;

import com.wultra.app.onboardingserver.common.database.OnboardingProcessConfigurationRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Configuration service.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
public class ConfigurationService {

    private final OnboardingProcessConfigurationRepository onboardingProcessConfigurationRepository;

    @Transactional(readOnly = true)
    public Optional<OnboardingProcessConfigurationEntity> fetchConfiguration(final String processType) {
        return onboardingProcessConfigurationRepository.findByProcessType(processType);
    }
}
