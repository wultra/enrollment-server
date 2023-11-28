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
import com.wultra.app.onboardingserver.api.provider.PresenceCheckProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Mock implementation of the {@link PresenceCheckProvider}
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server-onboarding.presence-check.provider", havingValue = "mock", matchIfMissing = true)
@Component
@Slf4j
public class WultraMockPresenceCheckProvider implements PresenceCheckProvider {

    private static final String SELFIE_PHOTO_PATH = "/images/specimen_photo.jpg";

    /**
     * Session parameter name of the verification token
     */
    private static final String VERIFICATION_TOKEN = "mockVerificationToken";
    private static final String SELFIE_FILENAME = "person_photo_from_mock.jpg";

    /**
     * Service constructor.
     */
    public WultraMockPresenceCheckProvider() {
        logger.warn("Using mocked version of {}", PresenceCheckProvider.class.getName());
    }

    @Override
    public void initPresenceCheck(OwnerId id, Image photo) {
        logger.info("Mock - initialized presence check with a photo, {}", id);
    }

    @Override
    public boolean shouldProvideTrustedPhoto() {
        return true;
    }

    @Override
    public SessionInfo startPresenceCheck(OwnerId id) {
        String token = UUID.randomUUID().toString();

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.getSessionAttributes().put(VERIFICATION_TOKEN, token);

        logger.info("Mock - started presence check, {}", id);

        return sessionInfo;
    }

    @Override
    public PresenceCheckResult getResult(OwnerId id, SessionInfo sessionInfo) {
        final Image photo = Image.builder()
                .data(readImage())
                .filename(SELFIE_FILENAME)
                .build();

        PresenceCheckResult result = new PresenceCheckResult();
        result.setStatus(PresenceCheckStatus.ACCEPTED);
        result.setPhoto(photo);

        logger.info("Mock - provided accepted result, {}", id);

        return result;
    }

    private static byte[] readImage() {
        try (final InputStream is = WultraMockPresenceCheckProvider.class.getResourceAsStream(SELFIE_PHOTO_PATH)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            logger.error("Unable to read image data from: {}", SELFIE_PHOTO_PATH);
        }
        return new byte[]{};
    }

    @Override
    public void cleanupIdentityData(OwnerId id) {
        logger.info("Mock - cleaned up identity data, {}", id);
    }

}
