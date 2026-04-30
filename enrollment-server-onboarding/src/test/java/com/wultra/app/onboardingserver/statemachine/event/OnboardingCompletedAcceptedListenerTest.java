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

package com.wultra.app.onboardingserver.statemachine.event;

import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.onboardingserver.impl.service.userdatastore.UserDataStoreService;
import com.wultra.security.userdatastore.client.model.error.UserDataStoreClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnboardingCompletedAcceptedListener}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class OnboardingCompletedAcceptedListenerTest {

    @Mock
    private UserDataStoreService userDataStoreService;

    @InjectMocks
    private OnboardingCompletedAcceptedListener tested;

    @Test
    void testOnOnboardingCompletedAccepted_collectingDataThrowsException_exceptionIsNotPropagated() {
        // given
        when(userDataStoreService.collectDocumentData(anyString())).thenThrow(new RuntimeException("test exception"));

        // when / then
        assertDoesNotThrow(() -> tested.onOnboardingCompletedAccepted(createEvent()));
    }

    @Test
    void testOnOnboardingCompletedAccepted_storingThrowsException_exceptionIsNotPropagated() throws Exception {
        // given
        when(userDataStoreService.collectDocumentData(anyString())).thenReturn(List.of());
        doThrow(new UserDataStoreClientException("test exception")).when(userDataStoreService).storeDocumentData(anyList());

        // when / then
        assertDoesNotThrow(() -> tested.onOnboardingCompletedAccepted(createEvent()));
    }

    private static OnboardingCompletedAcceptedEvent createEvent() {
        return new OnboardingCompletedAcceptedEvent(new Object(), new OwnerId(), "processId");
    }
}
