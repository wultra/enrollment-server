/*
 * Signer Cloud
 * Copyright (C) 2025 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.provider.microblink;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.Image;
import lombok.Builder;

import java.util.List;

/**
 * Microblink verification data for one onboarding process.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Builder(toBuilder = true)
record MicroblinkVerificationData(
        List<Document> documents,
        String facePhotoId
) {

    @Builder(toBuilder = true)
    public record Document(
            String documentId,
            String uploadId,
            DocumentType type,
            CardSide side,
            Image image
    ) {}
}
