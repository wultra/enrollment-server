/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wultra.app.onboardingserver.statemachine.interceptor;

import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.app.onboardingserver.statemachine.consts.ExtendedStateVariable;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingEvent;
import com.wultra.app.onboardingserver.statemachine.enums.OnboardingState;
import com.wultra.core.rest.model.base.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.DefaultExtendedState;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CustomStateMachineInterceptor}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class CustomStateMachineInterceptorTest {

    @Mock
    private StateMachine<OnboardingState, OnboardingEvent> stateMachine;

    private final CustomStateMachineInterceptor tested = new CustomStateMachineInterceptor();

    @Test
    void testStateMachineError_remoteCommunicationException() {
        final ExtendedState extendedState = new DefaultExtendedState();
        when(stateMachine.getExtendedState()).thenReturn(extendedState);
        final RemoteCommunicationException exception = new RemoteCommunicationException("test");

        final Exception result = tested.stateMachineError(stateMachine, exception);

        assertSame(exception, result);
        final Map<Object, Object> variables = extendedState.getVariables();
        assertEquals(HttpStatus.CONFLICT, variables.get(ExtendedStateVariable.RESPONSE_STATUS));
        final ErrorResponse response = assertInstanceOf(ErrorResponse.class, variables.get(ExtendedStateVariable.RESPONSE_OBJECT));
        assertEquals("REMOTE_COMMUNICATION_ERROR", response.getResponseObject().getCode());
    }

}
