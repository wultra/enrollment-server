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

package com.wultra.app.onboardingserver.common.database.entity;

/**
 * View for ids related to {@link DocumentVerificationEntity}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
public interface DocumentVerificationIdsView {

    /**
     * Gets {@link DocumentVerificationEntity#getUploadId()}
     *
     * @return upload ID
     */
    String getUploadId();

    /**
     * Gets {@link DocumentVerificationEntity#getPhotoId()}
     *
     * @return photo ID
     */
    String getPhotoId();

    /**
     * Gets {@link DocumentVerificationEntity#getId()}
     *
     * @return ID
     */
    String getId();
}
