/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2022 Wultra s.r.o.
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
package com.wultra.app.enrollmentserver.api.model.onboarding.response.data;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import lombok.Data;

import java.util.List;

/**
 * Response class used for document metadata.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Data
public class DocumentMetadataResponseDto {

    /**
     * Filename specified during upload from mobile client
     */
    private String filename;

    /**
     * Identifier of the document
     */
    private String id;

    /**
     * Type of the document
     */
    private DocumentType type;

    /**
     * Side of a card the document was captured from
     */
    private CardSide side;

    /**
     * Processing status of the document
     */
    private DocumentStatus status;

    /**
     * Errors discovered during processing of the document
     */
    private List<String> errors;

}
