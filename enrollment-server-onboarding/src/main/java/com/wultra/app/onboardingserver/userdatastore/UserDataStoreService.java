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

package com.wultra.app.onboardingserver.userdatastore;

import com.wultra.security.userdatastore.client.UserDataStoreClient;
import com.wultra.security.userdatastore.client.model.error.UserDataStoreClientException;
import com.wultra.security.userdatastore.client.model.request.DocumentCreateRequest;
import com.wultra.security.userdatastore.client.model.response.DocumentCreateResponse;
import com.wultra.security.userdatastore.client.model.response.EmbeddedPhotoCreateResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service for User Data Store operations.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Slf4j
@AllArgsConstructor
public class UserDataStoreService {

    private final UserDataStoreClient client;

    void callCreateDocument(final DocumentCreateRequest request) throws UserDataStoreClientException {
        logger.info("action: callCreateDocument, state: initiated, userId: {}, externalId: {}, documentType: {}, dataType: {}", request.userId(), request.externalId(), request.documentType(), request.dataType());

        try {
            final var response = client.createDocument(request);
            logger.info("action: callCreateDocument, state: succeeded, documentId: {}, photoIds: {}", response.id(), getPhotoIds(response));
        } catch (final UserDataStoreClientException e) {
            logger.info("action: callCreateDocument, state: failed, errorMessage: {}", e.getMessage());
            throw e;
        }
    }

    private static List<String> getPhotoIds(final DocumentCreateResponse response) {
        return response.photos()
                .stream()
                .map(EmbeddedPhotoCreateResponse::id)
                .toList();
    }
}
