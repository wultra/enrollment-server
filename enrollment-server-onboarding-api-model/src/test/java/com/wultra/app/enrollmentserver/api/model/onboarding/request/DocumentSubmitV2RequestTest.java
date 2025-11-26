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

package com.wultra.app.enrollmentserver.api.model.onboarding.request;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link DocumentSubmitV2Request}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
class DocumentSubmitV2RequestTest {

    @Test
    void testToString_fieldDataIsNull_valueNullIsUsed() {
        // given
        final var request = DocumentSubmitV2Request.builder()
                .processId("process-1")
                .resubmit(true)
                .documents(
                        List.of(
                                DocumentSubmitV2Request.Document.builder()
                                        .originalDocumentId("doc-1")
                                        .uploadId("upload-1")
                                        .filename("file1.jpg")
                                        .type(DocumentType.ID_CARD)
                                        .side(CardSide.FRONT)
                                        .build()
                        )
                )
                .build();

        // when
        final var requestAsString = request.toString();

        // then
        assertEquals(
                "DocumentSubmitV2Request[processId=process-1, documents=[Document[originalDocumentId=doc-1, uploadId=upload-1, filename=file1.jpg, type=ID_CARD, side=FRONT, data=null]], resubmit=true]",
                requestAsString
        );
    }

    @Test
    void testToString_fieldDataWithValue_valueIsSanitized() {
        // given
        final var request = DocumentSubmitV2Request.builder()
                .processId("process-1")
                .resubmit(true)
                .documents(
                        List.of(
                                DocumentSubmitV2Request.Document.builder()
                                        .originalDocumentId("doc-1")
                                        .uploadId("upload-1")
                                        .filename("file1.jpg")
                                        .type(DocumentType.ID_CARD)
                                        .side(CardSide.FRONT)
                                        .data("base64-data")
                                        .build()
                        )
                )
                .build();

        // when
        final var requestAsString = request.toString();

        // then
        assertEquals(
                "DocumentSubmitV2Request[processId=process-1, documents=[Document[originalDocumentId=doc-1, uploadId=upload-1, filename=file1.jpg, type=ID_CARD, side=FRONT, data=length:11]], resubmit=true]",
                requestAsString
        );
    }
}
