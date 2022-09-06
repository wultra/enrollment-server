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
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.common.database.entity.IdentityVerificationEntity;
import com.wultra.app.onboardingserver.errorhandling.PresenceCheckException;
import com.wultra.app.onboardingserver.impl.service.PresenceCheckService;
import com.wultra.app.onboardingserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.onboardingserver.statemachine.consts.EventHeaderName;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action to process verification result
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class PresenceCheckVerificationAction implements Action<OnboardingState, OnboardingEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PresenceCheckVerificationAction.class);

    private final JsonSerializationService jsonSerializationService;

    private final PresenceCheckService presenceCheckService;

    @Autowired
    public PresenceCheckVerificationAction(JsonSerializationService jsonSerializationService, PresenceCheckService presenceCheckService) {
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckService = presenceCheckService;
    }

    @Override
    public void execute(StateContext<OnboardingState, OnboardingEvent> context) {
        OwnerId ownerId = (OwnerId) context.getMessageHeader(EventHeaderName.OWNER_ID);
        IdentityVerificationEntity identityVerification = context.getExtendedState().get(ExtendedStateVariable.IDENTITY_VERIFICATION, IdentityVerificationEntity.class);

        SessionInfo sessionInfo =
                jsonSerializationService.deserialize(identityVerification.getSessionInfo(), SessionInfo.class);
        if (sessionInfo == null) {
            logger.error("Checking presence verification failed due to invalid session info, {}", ownerId);
            identityVerification.setErrorDetail("Unable to deserialize session info");
            identityVerification.setErrorOrigin(ErrorOrigin.PRESENCE_CHECK);
            identityVerification.setStatus(IdentityVerificationStatus.FAILED);
            identityVerification.setTimestampLastUpdated(ownerId.getTimestamp());
            identityVerification.setTimestampFailed(ownerId.getTimestamp());
        } else {
            try {
                presenceCheckService.checkPresenceVerification(ownerId, identityVerification, sessionInfo);
            } catch (PresenceCheckException e) {
                logger.error("Checking presence verification failed, {}", ownerId, e);
                identityVerification.setErrorDetail(e.getMessage());
                identityVerification.setErrorOrigin(ErrorOrigin.PRESENCE_CHECK);
                identityVerification.setStatus(IdentityVerificationStatus.FAILED);
                identityVerification.setTimestampLastUpdated(ownerId.getTimestamp());
                identityVerification.setTimestampFailed(ownerId.getTimestamp());
            }
        }
    }

}
