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

import java.util.List;

/**
 * Service for storing user data after the onboarding process is finished.
 *
 * @author Lubos Racansky, lubos.racansky@wultra.com
 */
public interface UserDataStoreService {

    /**
     * Collect document data for the given process if the storage is configured; otherwise, return an empty list.
     *
     * @param processId Process ID.
     * @return Collected document data or empty if not configured or no data found.
     * @see #storeDocumentData(List)
     */
    default List<DocumentCreateRequest> collectDocumentData(String processId) {
        return List.of();
    }

    /**
     * Store document data in User Data Store if the storage is configured.
     *
     * @param documentRequests List of document creation requests.
     * @see #collectDocumentData(String)
     */
    void storeDocumentData(List<DocumentCreateRequest> documentRequests);
}
