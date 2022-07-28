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
package com.wultra.app.onboardingserver.statemachine;

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentEvent;
import com.wultra.app.onboardingserver.statemachine.enums.EnrollmentState;
import com.wultra.app.onboardingserver.statemachine.service.StateMachineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = {EnrollmentServerTestApplication.class})
@ActiveProfiles("test-onboarding")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class InitialStateTest {

    @Autowired
    private StateMachineService stateMachineService;

    @Test
    public void testInitialState() throws Exception {
        StateMachine<EnrollmentState, EnrollmentEvent> stateMachine =
                stateMachineService.prepareStateMachine("processId", EnrollmentState.INITIAL, null);

        StateMachineTestPlan<EnrollmentState, EnrollmentEvent> expected =
                StateMachineTestPlanBuilder.<EnrollmentState, EnrollmentEvent>builder()
                        .stateMachine(stateMachine)
                        .step()
                        .expectState(EnrollmentState.INITIAL)
                        .and()
                        .build();
        expected.test();
    }

}
