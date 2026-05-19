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
import com.wultra.app.onboardingserver.impl.service.document.DocumentVerificationService;
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
 * Call final document verification at {@code DOCUMENT_VERIFICATION_FINAL} phase and move to the next state.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class DocumentVerificationFinalAction implements Action<OnboardingState, OnboardingEvent> {

    private static final String RESULT_KEY = "DOCUMENT_VERIFICATION_FINAL_RESULT";

    private final DocumentVerificationService documentVerificationService;

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        final var result = documentVerificationService.executeFinalDocumentVerification(identityVerification, ownerId);
        context.getExtendedState().getVariables().put(RESULT_KEY, result);
    }

    /**
     * Guard that checks if the final document verification result is OK.
     *
     * @return guard returning {@code true} if the final document verification result is OK
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultOk() {
        return isResult(DocumentVerificationService.FinalDocumentVerificationResult.OK);
    }

    /**
     * Guard that checks if the final document verification result is REJECTED.
     *
     * @return guard returning {@code true} if the final document verification result is REJECTED
     */
    public static Guard<OnboardingState, OnboardingEvent> isResultRejected() {
        return isResult(DocumentVerificationService.FinalDocumentVerificationResult.REJECTED);
    }

    private static Guard<OnboardingState, OnboardingEvent> isResult(final DocumentVerificationService.FinalDocumentVerificationResult expectedResult) {
        return context -> evaluateResult(context, expectedResult);
    }

    private static boolean evaluateResult(final StateContext<OnboardingState, OnboardingEvent> context, final DocumentVerificationService.FinalDocumentVerificationResult expectedResult) {
        final var contextValue = context.getExtendedState().getVariables().get(RESULT_KEY);

        if (contextValue instanceof DocumentVerificationService.FinalDocumentVerificationResult result) {
            return expectedResult == result;
        }

        return false;
    }
}
