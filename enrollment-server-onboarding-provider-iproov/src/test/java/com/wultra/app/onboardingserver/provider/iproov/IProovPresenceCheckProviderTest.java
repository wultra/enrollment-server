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
package com.wultra.app.onboardingserver.provider.iproov;

import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@SpringBootTest(classes = EnrollmentServerTestApplication.class)
@ActiveProfiles("external-service")
@ComponentScan(basePackages = {"com.wultra.app.onboardingserver.presencecheck.iproov"})
@EnableConfigurationProperties
@Tag("external-service")
class IProovPresenceCheckProviderTest {

    private static final String VERIFICATION_TOKEN = "iProovVerificationToken";

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
    void initPresenceCheckTest() throws Exception {
        initPresenceCheck(ownerId);
    }

    // FIXME temporary testing of repeated initialization (not implemented deletion of previous iProov enrollment)
    @Test
    void repeatInitPresenceCheckTest() throws Exception {
        initPresenceCheck(ownerId);
        initPresenceCheck(ownerId);
    }

    @Test
    void startPresenceCheckTest() throws Exception {
        initPresenceCheck(ownerId);

        SessionInfo sessionInfo = provider.startPresenceCheck(ownerId);

        assertNotNull(sessionInfo);
        assertNotNull(sessionInfo.getSessionAttributes());
        assertNotNull(sessionInfo.getSessionAttributes().get(VERIFICATION_TOKEN));
    }

    @Test
    void getResultTest() throws Exception {
        initPresenceCheck(ownerId);

        SessionInfo sessionInfo = provider.startPresenceCheck(ownerId);

        PresenceCheckResult result = provider.getResult(ownerId, sessionInfo);

        assertEquals(PresenceCheckStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void repeatPresenceCheckStartTest() throws Exception {
        initPresenceCheck(ownerId);

        final SessionInfo sessionInfo1 = provider.startPresenceCheck(ownerId);
        assertNotNull(
                sessionInfo1.getSessionAttributes().get(VERIFICATION_TOKEN),
                "Missing presence check verification token in session 1"
        );

        initPresenceCheck(ownerId);

        final SessionInfo sessionInfo2 = provider.startPresenceCheck(ownerId);
        assertNotNull(
                sessionInfo2.getSessionAttributes().get(VERIFICATION_TOKEN),
                "Missing presence check verification token in session 2"
        );
        assertNotEquals(
                sessionInfo1.getSessionAttributes().get(VERIFICATION_TOKEN),
                sessionInfo2.getSessionAttributes().get(VERIFICATION_TOKEN),
                "Same presence check verification tokens between session 1 and session 2");
    }

    private OwnerId createOwnerId() {
        OwnerId ownerId = new OwnerId();
        ownerId.setActivationId("integration-test-" + UUID.randomUUID());
        ownerId.setUserId("integration-test-user-id" + UUID.randomUUID());
        return ownerId;
    }

    private void initPresenceCheck(OwnerId ownerId) throws Exception {
        final Image photo = loadPhoto("/images/specimen_photo.jpg");
        provider.initPresenceCheck(ownerId, photo);
    }

    private static Image loadPhoto(final String path) throws IOException {
        final File file = new File(path);

        return Image.builder()
                .data(readImageData(path))
                .filename(file.getName())
                .build();
    }

    private static byte[] readImageData(final String path) throws IOException {
        try (InputStream stream = IProovPresenceCheckProviderTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Unable to get a stream for: " + path);
            }
            return stream.readAllBytes();
        }
    }

}
