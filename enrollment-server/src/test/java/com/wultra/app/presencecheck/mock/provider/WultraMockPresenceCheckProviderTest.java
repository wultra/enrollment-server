/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.app.presencecheck.mock.provider;

import com.wultra.app.enrollmentserver.EnrollmentServerTestApplication;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.presencecheck.mock.MockConst;
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
@ActiveProfiles("mock")
@ComponentScan(basePackages = {"com.wultra.app.presencecheck.mock"})
@EnableConfigurationProperties
public class WultraMockPresenceCheckProviderTest {

    private WultraMockPresenceCheckProvider provider;

    private OwnerId ownerId;

    @BeforeEach
    public void init() {
        ownerId = createOwnerId();
    }

    @Autowired
    public void setProvider(WultraMockPresenceCheckProvider provider) {
        this.provider = provider;
    }

    @Test
    public void initPresenceCheckTest() throws Exception {
        initPresenceCheck();
    }

    @Test
    public void startPresenceCheckTest() throws Exception {
        initPresenceCheck();

        SessionInfo sessionInfo = provider.startPresenceCheck(ownerId);

        assertNotNull(sessionInfo);
        assertNotNull(sessionInfo.getSessionAttributes());
        assertNotNull(sessionInfo.getSessionAttributes().get(MockConst.VERIFICATION_TOKEN));
    }

    @Test
    public void getResultTest() throws Exception {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.getSessionAttributes().put(MockConst.VERIFICATION_TOKEN, "token");

        PresenceCheckResult result = provider.getResult(ownerId, sessionInfo);

        assertEquals(PresenceCheckStatus.ACCEPTED, result.getStatus());
        assertNotNull(result.getPhoto());
    }

    @Test
    public void cleanupIdentityDataTest() throws Exception {
        provider.cleanupIdentityData(ownerId);
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("activation-id");
        ownerId.setUserId("user-id");
        return ownerId;
    }

    private void initPresenceCheck() throws Exception {
        Image photo = new Image();
        photo.setData(new byte[]{});
        photo.setFilename("id_photo.jpg");
        provider.initPresenceCheck(ownerId, photo);
    }

}
