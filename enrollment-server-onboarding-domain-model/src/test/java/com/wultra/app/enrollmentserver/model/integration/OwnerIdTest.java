/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2024 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.model.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link OwnerId}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
class OwnerIdTest {

    @Test
    void testUserIdSecured() {
        final OwnerId tested = new OwnerId();
        tested.setUserId("Joe");

        final String result = tested.getUserIdSecured();

        assertEquals("NXMLPV6TYXCGRGZT4UNZ6EF4NKN6RH7I7IVBE7EMNQB42BOWRLHA", result);
    }
}
