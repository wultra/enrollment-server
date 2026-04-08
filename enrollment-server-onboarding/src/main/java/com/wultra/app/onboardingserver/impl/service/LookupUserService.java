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

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.common.service.AuditService;
import com.wultra.app.onboardingserver.errorhandling.OnboardingProviderException;
import com.wultra.app.onboardingserver.provider.OnboardingProvider;
import com.wultra.app.onboardingserver.provider.model.request.LookupUserRequest;
import com.wultra.app.onboardingserver.provider.model.response.LookupUserResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Service for looking up a user by given identification.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Service
@AllArgsConstructor
@Slf4j
public class LookupUserService {

    private final OnboardingProvider onboardingProvider;

    private final AuditService auditService;

    private final OnboardingProcessRepository onboardingProcessRepository;

    /**
     * Look up a user by given identification.
     * Beware that the method causes a <strong>side effect</strong> to the given process entity in case of an error.
     *
     * @param process Onboarding process. In case of an error, the entity is modified
     * @param identification User identification.
     * @return user lookup response or {@code empty}
     */
    @Transactional
    public Optional<LookupUserResponse> lookupUser(final OnboardingProcessEntity process, final Map<String, Object> identification) {
        try {
            final LookupUserRequest lookupUserRequest = LookupUserRequest.builder()
                    .identification(identification)
                    .processId(process.getId())
                    .processType(process.getProcessConfiguration().getProcessType())
                    .build();
            final LookupUserResponse response = onboardingProvider.lookupUser(lookupUserRequest);
            auditService.auditOnboardingProvider(process, "Looked up user: {}", response.getUserId());
            if (response.isErrorOccurred()) {
                logger.warn("Business logic error occurred during user lookup, process ID: {}, error detail: {}", process.getId(), response.getErrorDetail());
                process.setErrorOrigin(ErrorOrigin.USER_REQUEST);
                process.setErrorDetail(OnboardingProcessEntity.ERROR_USER_LOOKUP);
                process.setTimestampLastUpdated(new Date());
                onboardingProcessRepository.save(process);
                auditService.auditOnboardingProvider(process, "Error to look up user: {}, {}", response.getUserId(), response.getErrorDetail());
            }
            return Optional.of(response);
        } catch (final OnboardingProviderException e) {
            logger.info("User lookup failed, using null user ID, error: {}", e.getMessage());
            logger.debug("User lookup failed, using null user ID", e);
            return Optional.empty();
        }
    }
}
