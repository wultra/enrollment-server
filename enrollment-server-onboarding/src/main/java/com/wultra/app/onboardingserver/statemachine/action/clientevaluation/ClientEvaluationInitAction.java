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

import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase;
import com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus;
import com.wultra.app.onboardingserver.impl.service.IdentityVerificationService;
import com.wultra.app.onboardingserver.statemachine.action.MoveActionAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationPhase.CLIENT_EVALUATION;
import static com.wultra.app.enrollmentserver.model.enumeration.IdentityVerificationStatus.IN_PROGRESS;

/**
 * Action to initialize client evaluation.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Component
public class ClientEvaluationInitAction extends MoveActionAdapter {

    @Autowired
    public ClientEvaluationInitAction(final IdentityVerificationService identityVerificationService) {
        super(identityVerificationService);
    }

    @Override
    protected IdentityVerificationPhase getPhase() {
        return CLIENT_EVALUATION;
    }

    @Override
    protected IdentityVerificationStatus getStatus() {
        return IN_PROGRESS;
    }
}
