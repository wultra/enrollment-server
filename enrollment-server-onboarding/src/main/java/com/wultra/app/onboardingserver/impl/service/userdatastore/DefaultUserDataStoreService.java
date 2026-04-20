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
package com.wultra.app.onboardingserver.impl.service.userdatastore;

import com.wultra.security.userdatastore.client.UserDataStoreClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UserDataStoreService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
// TODO Lubos define correct property
@ConditionalOnProperty(name = "app.onboarding.user-data-store.enabled", havingValue = "true")
@Service
@AllArgsConstructor
@Slf4j
class DefaultUserDataStoreService implements UserDataStoreService {

    private final UserDataStoreClient userDataStoreClient;

    @Override
    public void storeDocumentData(final String processId) {
        logger.info("action: storeDocumentData, state: initiated, processId: {}", processId);
        // TODO
        logger.info("action: storeDocumentData, state: succeeded");
    }

    /*
    The configuration should specify which data is stored:
    - store only the document with a trusted image or all documents (i.e. document with selfie)
    - store document extracted data true/false (i.e. DocumentData and country)
    - store the document image scan true / false (store images of front and back side)
     */
}
