/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.presencecheck.mock.provider;

import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.onboardingserver.EnrollmentServerTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("test")
@ComponentScan(basePackages = "com.wultra.app.onboardingserver.presencecheck.mock")
@EnableConfigurationProperties
class WultraMockPresenceCheckProviderTest {

    private WultraMockPresenceCheckProvider provider;

    private OwnerId ownerId;

    @BeforeEach
    void init() {
        ownerId = createOwnerId();
    }

    @Autowired
    public void setProvider(WultraMockPresenceCheckProvider provider) {
        this.provider = provider;
    }

    @Test
    void initPresenceCheckTest() {
        initPresenceCheck();
    }

    @Test
    void startPresenceCheckTest() {
        initPresenceCheck();

        SessionInfo sessionInfo = provider.startPresenceCheck(ownerId);

        assertNotNull(sessionInfo);
        assertNotNull(sessionInfo.getSessionAttributes());
        assertNotNull(sessionInfo.getSessionAttributes().get("mockVerificationToken"));
    }

    @Test
    void getResultTest() {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.getSessionAttributes().put("mockVerificationToken", "token");

        PresenceCheckResult result = provider.getResult(ownerId, sessionInfo);

        assertEquals(PresenceCheckStatus.ACCEPTED, result.getStatus());
        assertNotNull(result.getPhoto());
    }

    @Test
    void cleanupIdentityDataTest() {
        provider.cleanupIdentityData(ownerId, new SessionInfo());
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("activation-id");
        ownerId.setUserId("user-id");
        return ownerId;
    }

    private void initPresenceCheck() {
        final Image photo = Image.builder()
                .data(new byte[]{})
                .filename("id_photo.jpg")
                .build();
        provider.initPresenceCheck(ownerId, photo);
    }

}
