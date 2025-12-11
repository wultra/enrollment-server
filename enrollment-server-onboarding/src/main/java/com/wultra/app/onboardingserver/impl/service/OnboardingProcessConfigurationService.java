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

import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for onboarding process configuration.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Service
@AllArgsConstructor
public class OnboardingProcessConfigurationService {

    private final OnboardingProcessRepository onboardingProcessRepository;

    public OnboardingProcessConfigurationValue findConfigByProcessId(final String processId) {
        final var onboardingProcess = onboardingProcessRepository.findById(processId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding process not found for id: " + processId));

        return Optional.ofNullable(onboardingProcess.getProcessConfiguration())
                .map(OnboardingProcessConfigurationEntity::getConfiguration)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding process configuration not found for process id: " + processId));
    }

}
