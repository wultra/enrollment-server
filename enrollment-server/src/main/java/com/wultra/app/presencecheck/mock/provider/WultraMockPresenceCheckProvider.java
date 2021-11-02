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

import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.model.enumeration.PresenceCheckStatus;
import com.wultra.app.enrollmentserver.model.integration.Image;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.PresenceCheckResult;
import com.wultra.app.enrollmentserver.model.integration.SessionInfo;
import com.wultra.app.enrollmentserver.provider.PresenceCheckProvider;
import com.wultra.app.presencecheck.mock.MockConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock implementation of the {@link PresenceCheckProvider}
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@ConditionalOnProperty(value = "enrollment-server.presence-check.provider", havingValue = "mock")
@Component
public class WultraMockPresenceCheckProvider implements PresenceCheckProvider {

    private static final Logger logger = LoggerFactory.getLogger(WultraMockPresenceCheckProvider.class);

    public WultraMockPresenceCheckProvider() {
        logger.warn("Using mocked version of {}", PresenceCheckProvider.class.getName());
    }

    @Override
    public void initPresenceCheck(OwnerId id, Image photo) throws PresenceCheckException {
        logger.warn("Mock - initialized presence check with a photo, " + id);
    }

    @Override
    public SessionInfo startPresenceCheck(OwnerId id) throws PresenceCheckException {
        String token = UUID.randomUUID().toString();

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.getSessionAttributes().put(MockConst.VERIFICATION_TOKEN, token);

        logger.warn("Mock - started presence check, " + id);

        return sessionInfo;
    }

    @Override
    public PresenceCheckResult getResult(OwnerId id, SessionInfo sessionInfo) throws PresenceCheckException {
        Image photo = new Image();
        photo.setData(new byte[]{});
        photo.setFilename("selfie_photo.jpg");

        PresenceCheckResult result = new PresenceCheckResult();
        result.setStatus(PresenceCheckStatus.ACCEPTED);
        result.setPhoto(photo);

        logger.warn("Mock - provided accepted result, " + id);

        return result;
    }

    @Override
    public void cleanupIdentityData(OwnerId id) throws PresenceCheckException {
        logger.warn("Mock - cleaned up identity data, " + id);
    }

}
