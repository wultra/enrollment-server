/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.statemachine.action.verification;

import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.OnboardingApprovalService;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.AllArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Onboarding approval action.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@AllArgsConstructor
public class OnboardingApprovalAction implements Action<OnboardingState, OnboardingEvent> {

    private static final String RESULT_KEY = "APPROVAL_RESULT";

    private final OnboardingApprovalService onboardingApprovalService;

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        final OnboardingApprovalService.ApprovalResult result = onboardingApprovalService.approve(identityVerification);
        context.getExtendedState().getVariables().put(RESULT_KEY, result);
    }

    /**
     * Guard that checks if the approval result is OK.
     *
     * @return guard returning {@code true} if the approval result is OK
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultOk() {
        return isResult(OnboardingApprovalService.ApprovalResult.OK);
    }

    /**
     * Guard that checks if the approval result is NOK.
     *
     * @return guard returning {@code true} if the approval result is NOK
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultRejected() {
        return isResult(OnboardingApprovalService.ApprovalResult.NOK);
    }

    /**
     * Guard that checks if the approval result is WAIT.
     *
     * @return guard returning {@code true} if the approval result is WAIT
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultInProgress() {
        return isResult(OnboardingApprovalService.ApprovalResult.WAIT);
    }

    private static Guard<OnboardingState, OnboardingEvent> isResult(OnboardingApprovalService.ApprovalResult expectedResult) {
        return context -> evaluateResult(context, expectedResult);
    }

    private static boolean evaluateResult(final StateContext<OnboardingState, OnboardingEvent> context, final OnboardingApprovalService.ApprovalResult expectedResult) {
        final var contextValue = context.getExtendedState().getVariables().get(RESULT_KEY);

        if (contextValue instanceof OnboardingApprovalService.ApprovalResult result) {
            return expectedResult == result;
        }

        return false;
    }
}
