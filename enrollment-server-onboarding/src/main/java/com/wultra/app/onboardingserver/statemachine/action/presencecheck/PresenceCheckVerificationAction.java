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
package com.wultra.app.onboardingserver.statemachine.action.presencecheck;

import com.wultra.app.enrollmentserver.model.enumeration.ErrorOrigin;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.api.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.impl.service.PresenceCheckService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.FAILED;

/**
 * Action to process verification result.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
@AllArgsConstructor
@Slf4j
public class PresenceCheckVerificationAction implements Action<OnboardingState, OnboardingEvent> {

    private final PresenceCheckService presenceCheckService;

    private final IdentityVerificationService identityVerificationService;

    @Override
    public void execute(final StateContext<OnboardingState, OnboardingEvent> context) {
        final OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        final IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        try {
            presenceCheckService.checkPresenceVerification(ownerId, identityVerification);
        } catch (PresenceCheckException | RemoteCommunicationException e) {
            logger.error("Checking presence verification failed, {}", ownerId, e);
            identityVerification.setErrorDetail(IdentityVerificationEntity.PRESENCE_CHECK_FAILED);
            identityVerification.setErrorOrigin(ErrorOrigin.PRESENCE_CHECK);
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
            identityVerificationService.moveToPhaseAndStatus(identityVerification, identityVerification.getPhase(), FAILED, ownerId);
        }
    }

}
