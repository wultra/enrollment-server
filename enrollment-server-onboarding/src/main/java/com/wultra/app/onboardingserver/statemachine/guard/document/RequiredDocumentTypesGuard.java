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
package com.wultra.app.onboardingserver.statemachine.guard.document;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

/**
 * Wrapper of {@link RequiredDocumentTypesCheck} to spring state machine guard.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
public class RequiredDocumentTypesGuard implements Guard<OnboardingState, OnboardingEvent> {

    private final RequiredDocumentTypesCheck requiredDocumentTypesCheck;

    @Autowired
    public RequiredDocumentTypesGuard(final RequiredDocumentTypesCheck requiredDocumentTypesCheck) {
        this.requiredDocumentTypesCheck = requiredDocumentTypesCheck;
    }

    @Override
    public boolean evaluate(StateContext<OnboardingState, OnboardingEvent> context) {
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);
        final var acceptedDocumentVerifications = identityVerification.getDocumentVerifications().stream()
                .filter(it -> it.getStatus() == DocumentStatus.ACCEPTED)
                .collect(toList());
        return requiredDocumentTypesCheck.evaluate(acceptedDocumentVerifications, identityVerification.getId());
    }
}
