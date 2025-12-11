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
package com.wultra.app.onboardingserver.statemachine.guard.otp;

import com.wultra.app.onboardingserver.common.database.OnboardingProcessRepository;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessEntity;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.AllArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Guard to ensure enabled OTP verification
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
@AllArgsConstructor
public class OtpVerificationEnabledGuard implements Guard<OnboardingState, OnboardingEvent> {

    private final OnboardingProcessRepository onboardingProcessRepository;

    @Override
    public boolean evaluate(StateContext<OnboardingState, OnboardingEvent> context) {
        IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
        final String processId = identityVerification.getProcessId();
        return onboardingProcessRepository.findById(processId)
                .map(OnboardingProcessEntity::getProcessConfiguration)
                .map(OnboardingProcessConfigurationEntity::getConfiguration)
                .filter(OnboardingProcessConfigurationValue::otpForIdentityVerification)
                .isPresent();
    }

}
