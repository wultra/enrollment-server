/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.presencecheck.iproov.service;

import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = EnrollmentServerTestApplication.class)
/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
class IProovRestApiServiceTest {

    @Test
    public void ensureValidUserIdValueTest() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            IProovRestApiService.ensureValidUserIdValue("invalidChars,[=");
        });

        String userIdTooLong = RandomStringUtils.randomAlphabetic(IProovRestApiService.USER_ID_MAX_LENGTH + 1);
        String userIdEnsured = IProovRestApiService.ensureValidUserIdValue(userIdTooLong);
        assertEquals(IProovRestApiService.USER_ID_MAX_LENGTH, userIdEnsured.length());
    }

}
