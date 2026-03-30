/*
 * PowerAuth Enrollment Server
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
package com.wultra.app.onboardingserver.provider.microblink.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Bundle of raw and parsed response from Microblink BlinkID REST API.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Getter
@AllArgsConstructor
public class DocumentVerificationResponseBundle {

    private static final String IMAGES_FIELD = "images";
    private static final String EXTRACTION_FIELD = "extraction";
    private static final String VIZ_FIELD = "viz";
    private static final String FRONT_FIELD = "front";
    private static final String BACK_FIELD = "back";

    private final DocumentVerificationResponse parsedResponseBody;

    @Getter(AccessLevel.NONE)
    private final ObjectNode responseBodyJson;
    
    public ObjectNode getResponseWithoutPersonalData() {
        final var modifiedObject = responseBodyJson.deepCopy();
        modifiedObject.remove(IMAGES_FIELD);
        modifiedObject.remove(EXTRACTION_FIELD);
        return modifiedObject;
    }

    public String getResponseWithoutImages() {
        final var modifiedObject = responseBodyJson.deepCopy();
        modifiedObject.remove(IMAGES_FIELD);
        return modifiedObject.toString();
    }

    public String getExtractionFront() {
        return responseBodyJson.path(EXTRACTION_FIELD)
                .path(VIZ_FIELD)
                .path(FRONT_FIELD)
                .toString();
    }

    public String getExtractionBack() {
        return responseBodyJson.path(EXTRACTION_FIELD)
                .path(VIZ_FIELD)
                .path(BACK_FIELD)
                .toString();
    }
}
