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

package com.wultra.app.onboardingserver.errorhandling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DefaultExceptionHandler}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class DefaultExceptionHandlerTest {

    @InjectMocks
    private DefaultExceptionHandler tested;

    @Test
    void testHandleIllegalArgumentException_expectedResponseIsReturned() {
        // given
        final var exception = new Base64DeserializationException("Test exception", new RuntimeException());

        // when
        final var response = tested.handleBase64DeserializationException(exception);

        // then
        assertEquals("ERROR", response.getStatus());

        final var responseObject = response.getResponseObject();
        assertEquals("Deserialization of base64 value failed.", responseObject.getMessage());
        assertEquals("INVALID_REQUEST", responseObject.getCode());
    }
}
