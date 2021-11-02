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
package com.wultra.app.presencecheck.iproov.provider;

import com.wultra.app.enrollmentserver.EnrollmentServerTestApplication;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.presencecheck.iproov.IProovConst;
import com.wultra.app.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("external-service")
@ComponentScan(basePackages = {"com.wultra.app.presencecheck.iproov"})
@EnableConfigurationProperties
@Tag("external-service")
public class IProovPresenceCheckProviderTest {

    private IProovPresenceCheckProvider provider;

    private OwnerId ownerId;

    @BeforeEach
    public void init() {
        ownerId = createOwnerId();
    }

    @Autowired
    public void setProvider(IProovPresenceCheckProvider provider) {
        this.provider = provider;
    }

    @Test
    public void initPresenceCheckTest() throws Exception {
        initPresenceCheck(ownerId);
    }

    @Test
    public void startPresenceCheckTest() throws Exception {
        initPresenceCheck(ownerId);

        SessionInfo sessionInfo = provider.startPresenceCheck(ownerId);

        assertNotNull(sessionInfo);
        assertNotNull(sessionInfo.getSessionAttributes());
        assertNotNull(sessionInfo.getSessionAttributes().get(IProovConst.VERIFICATION_TOKEN));
    }

    @Test
    public void getResultTest() throws Exception {
        initPresenceCheck(ownerId);

        SessionInfo sessionInfo = provider.startPresenceCheck(ownerId);

        PresenceCheckResult result = provider.getResult(ownerId, sessionInfo);

        assertEquals(PresenceCheckStatus.IN_PROGRESS, result.getStatus());
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("integration-test-" + UUID.randomUUID());
        ownerId.setUserId("integration-test-user-id");
        return ownerId;
    }

    private void initPresenceCheck(OwnerId ownerId) throws Exception {
        Image photo = TestUtil.loadPhoto("/images/specimen_photo.jpg");
        provider.initPresenceCheck(ownerId, photo);
    }

}
