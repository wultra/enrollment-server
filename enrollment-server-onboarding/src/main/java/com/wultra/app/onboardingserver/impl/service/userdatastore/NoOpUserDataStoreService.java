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

import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Empty implementation {@link UserDataStoreService}.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
@ConditionalOnMissingBean(UserDataStoreService.class)
@Service
@Slf4j
class NoOpUserDataStoreService implements UserDataStoreService {

    @Override
    public void storeDocumentData(final List<DocumentCreateRequest> documentRequests) {
        logger.info("action: storeDocumentData, state: skipped");
    }
}
