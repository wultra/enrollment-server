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

package com.wultra.app.onboardingserver.common.database.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DocumentDataEntity}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
class DocumentDataEntityTest {

    @Test
    void testToString_entityWithDefaultValues_stringIsCorrect() {
        // given
        final var entity = new DocumentDataEntity();

        // when
        final var toString = entity.toString();

        // then
        assertEquals("DocumentDataEntity(id=null, data=null, timestampCreated=null)", toString);
    }

    @Test
    void testToString_entityWithSetValues_stringIsCorrect() {
        // given
        final var timestampCreated = new Date();

        final var entity = new DocumentDataEntity();
        entity.setId("test-id");
        entity.setData(new byte[] { 1, 2, 3 } );
        entity.setTimestampCreated(timestampCreated);

        // when
        final var toString = entity.toString();

        // then
        assertEquals("DocumentDataEntity(id=test-id, data=byte[3], timestampCreated="+timestampCreated+")", toString);
    }
}
