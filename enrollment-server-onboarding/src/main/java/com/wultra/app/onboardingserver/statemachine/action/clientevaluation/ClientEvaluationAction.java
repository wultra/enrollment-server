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
package com.wultra.app.onboardingserver.statemachine.action.clientevaluation;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.ClientEvaluationService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.AllArgsConstructor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Action to process client evaluation.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@AllArgsConstructor
public class ClientEvaluationAction implements Action<OnboardingState, OnboardingEvent> {

    private static final String RESULT_KEY = "EVALUATION_RESULT";

    private final ClientEvaluationService clientEvaluationService;

    @Override
    public void execute(final StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        final var result = clientEvaluationService.processClientEvaluation(identityVerification, ownerId);
        context.getExtendedState().getVariables().put(RESULT_KEY, result);
    }

    /**
     * Guard that checks if the evaluation result is OK.
     *
     * @return guard returning {@code true} if the evaluation result is OK
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultOk() {
        return isResult(ClientEvaluationService.ClientEvaluationResult.OK);
    }

    /**
     * Guard that checks if the evaluation result is NOK.
     *
     * @return guard returning {@code true} if the evaluation result is NOK
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultRejected() {
        return isResult(ClientEvaluationService.ClientEvaluationResult.NOK);
    }

    /**
     * Guard that checks if the evaluation result is WAIT.
     *
     * @return guard returning {@code true} if the evaluation result is WAIT
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultInProgress() {
        return isResult(ClientEvaluationService.ClientEvaluationResult.WAIT);
    }

    private static Guard<OnboardingState, OnboardingEvent> isResult(final ClientEvaluationService.ClientEvaluationResult expectedResult) {
        return context -> evaluateResult(expectedResult, context);
    }

    private static boolean evaluateResult(final ClientEvaluationService.ClientEvaluationResult expectedResult, final StateContext<OnboardingState, OnboardingEvent> context) {
        final var contextValue = context.getExtendedState().getVariables().get(RESULT_KEY);

        if (contextValue instanceof ClientEvaluationService.ClientEvaluationResult result) {
            return expectedResult == result;
        }

        return false;
    }
}
