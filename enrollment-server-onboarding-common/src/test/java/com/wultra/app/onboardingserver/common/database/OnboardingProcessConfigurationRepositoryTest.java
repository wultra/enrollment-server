/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.common.database;

import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationEntity;
import com.wultra.app.onboardingserver.common.database.entity.OnboardingProcessConfigurationValue;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link OnboardingProcessConfigurationRepository}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@DataJpaTest
@ActiveProfiles("test")
@Sql
class OnboardingProcessConfigurationRepositoryTest {

    @Autowired
    private OnboardingProcessConfigurationRepository tested;

    @Test
    void testJsonMapping() {
        final OnboardingProcessConfigurationEntity entity = tested.findByProcessType("reactivation").orElseThrow(AssertionFailedError::new);
        final OnboardingProcessConfigurationValue result = entity.getConfiguration();

        assertTrue(result.enabled());
        assertTrue(result.otpForIdentification());
        assertTrue(result.otpForIdentityVerification());
        assertTrue(result.useTemporaryActivation());

        final var documents = result.documents();
        assertNotNull(documents);
        assertEquals(2, documents.requiredDocumentsCount());

        final var document1 = documents.items().get(0);
        assertEquals("ID_CARD", document1.type().name());
        assertEquals(2, document1.sideCount());
        assertTrue(document1.mandatory());

        final var document2 = documents.items().get(1);
        assertEquals("DRIVING_LICENCE", document2.type().name());
        assertEquals(1, document2.sideCount());
        assertFalse(document2.mandatory());
        assertEquals(OnboardingProcessConfigurationValue.ActivationType.CODE, result.activationType());
    }

    @Test
    void testJsonMapping_defaultValues() {
        final OnboardingProcessConfigurationEntity entity = tested.findByProcessType("onboarding").orElseThrow(AssertionFailedError::new);
        final OnboardingProcessConfigurationValue result = entity.getConfiguration();

        assertFalse(result.enabled());
        assertFalse(result.otpForIdentification());
        assertFalse(result.otpForIdentityVerification());
        assertFalse(result.useTemporaryActivation());

        final var documents = result.documents();
        assertNotNull(documents);
        assertEquals(0, documents.requiredDocumentsCount());
        assertEquals(0, documents.items().size());
        assertEquals(OnboardingProcessConfigurationValue.ActivationType.IDENTITY, result.activationType());
    }
}
