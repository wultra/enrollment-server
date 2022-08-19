/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine;

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.errorhandling.IdentityVerificationException;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enrollment state service
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class EnrollmentStateProvider {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentStateProvider.class);

    private final Map<IdentityVerificationPhase, Map<IdentityVerificationStatus, OnboardingState>> stateByPhaseStatus;

    public EnrollmentStateProvider() {
        this.stateByPhaseStatus = new HashMap<>();
        initAllStates();
    }

    public OnboardingState findByPhaseAndStatus(IdentityVerificationPhase phase, IdentityVerificationStatus status)
            throws IdentityVerificationException {
        OnboardingState state = stateByPhaseStatus.getOrDefault(phase, Collections.emptyMap()).get(status);
        if (state == null) {
            throw new IdentityVerificationException(
                    String.format("Unknown state for phase=%s, status=%s", phase, status)
            );
        }
        return state;
    }

    private void initAllStates() {
        Arrays.stream(OnboardingState.values())
                .filter(state -> !state.isChoiceState())
                .forEach(value -> {
                    Map<IdentityVerificationStatus, OnboardingState> stateByStatus =
                            stateByPhaseStatus.computeIfAbsent(value.getPhase(), k -> new HashMap<>());
                    if (stateByStatus.containsKey(value.getStatus())) {
                        throw new IllegalStateException("Already mapped phase and status: " + value);
                    }
                    stateByStatus.put(value.getStatus(), value);
                });
    }

}
