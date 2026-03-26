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
package com.wultra.app.onboardingserver.statemachine.action.verification;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.NullObject;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * Action to start the verification process
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
@Slf4j
@AllArgsConstructor
public class VerificationDocumentStartAction implements Action<OnboardingState, OnboardingEvent> {

    private static final String RESULT_KEY = "DOCUMENT_VERIFICATION_RESULT";

    private final IdentityVerificationService identityVerificationService;

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        final var result = identityVerificationService.startDocumentVerification(ownerId, identityVerification);
        context.getExtendedState().getVariables().put(RESULT_KEY, result != null ? result : new NullObject());
    }

    /**
     * Guard that checks if all documents are accepted.
     *
     * @return guard returning {@code true} if all documents are accepted
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultOk() {
        return isResult(IdentityVerificationService.DocumentEvaluationStatus.OK);
    }

    /**
     * Guard that checks if the documents are not accepted or not all required documents are accepted yet
     *
     * @return guard returning {@code true} if some documents are not accepted or not all required documents are accepted yet
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultInProgress() {
        return isResult(IdentityVerificationService.DocumentEvaluationStatus.NOK);
    }

    private static Guard<OnboardingState, OnboardingEvent> isResult(final IdentityVerificationService.DocumentEvaluationStatus expectedResult) {
        return context -> evaluateDocumentResult(context, expectedResult);
    }

    private static boolean evaluateDocumentResult(final StateContext<OnboardingState, OnboardingEvent> context, final IdentityVerificationService.DocumentEvaluationStatus expectedResult) {
        final var contextValue = context.getExtendedState().getVariables().get(RESULT_KEY);

        if (contextValue instanceof IdentityVerificationService.DocumentEvaluationStatus result) {
            return expectedResult == result;
        }

        return false;
    }
}
