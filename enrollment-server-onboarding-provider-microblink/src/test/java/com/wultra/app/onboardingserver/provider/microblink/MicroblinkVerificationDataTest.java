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

package com.wultra.app.onboardingserver.provider.microblink;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import com.wultra.app.enrollmentserver.model.integration.Image;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MicroblinkVerificationData}.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@ExtendWith(MockitoExtension.class)
class MicroblinkVerificationDataTest {

    private static final String DOCUMENT_ID = "f3d1a2a8-5f5f-4c54-9c43-3ef52f5b0d44";
    private static final String UPLOAD_ID = "8ce0c42c-c97c-4cfa-8bf4-d54970cc0a0c";

    @Test
    void testToString_imageIsNull_correctStringIsProduced() {
        // given
        final var document = MicroblinkVerificationData.Document.builder()
                .documentId(DOCUMENT_ID)
                .uploadId(UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.FRONT)
                .image(null)
                .build();

        // when
        final var actualString = document.toString();

        // then
        final var expectedString = "Document{documentId=f3d1a2a8-5f5f-4c54-9c43-3ef52f5b0d44, uploadId=8ce0c42c-c97c-4cfa-8bf4-d54970cc0a0c, type=ID_CARD, side=FRONT, image=null}";

        assertEquals(expectedString, actualString);
    }

    @Test
    void testToString_imageDataIsNull_correctStringIsProduced() {
        // given
        final var document = MicroblinkVerificationData.Document.builder()
                .documentId(DOCUMENT_ID)
                .uploadId(UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.FRONT)
                .image(Image.builder()
                        .filename("test.jpg")
                        .build())
                .build();

        // when
        final var actualString = document.toString();

        // then
        final var expectedString = "Document{documentId=f3d1a2a8-5f5f-4c54-9c43-3ef52f5b0d44, uploadId=8ce0c42c-c97c-4cfa-8bf4-d54970cc0a0c, type=ID_CARD, side=FRONT, image=Image{filename=test.jpg, dataLength=null}}";

        assertEquals(expectedString, actualString);
    }

    @Test
    void testToString_imageDataIsNotNull_correctStringIsProduced() {
        // given
        final var document = MicroblinkVerificationData.Document.builder()
                .documentId(DOCUMENT_ID)
                .uploadId(UPLOAD_ID)
                .type(DocumentType.ID_CARD)
                .side(CardSide.FRONT)
                .image(Image.builder()
                        .filename("test.jpg")
                        .data(new byte[] { 1, 2 })
                        .build())
                .build();

        // when
        final var actualString = document.toString();

        // then
        final var expectedString = "Document{documentId=f3d1a2a8-5f5f-4c54-9c43-3ef52f5b0d44, uploadId=8ce0c42c-c97c-4cfa-8bf4-d54970cc0a0c, type=ID_CARD, side=FRONT, image=Image{filename=test.jpg, dataLength=2}}";

        assertEquals(expectedString, actualString);
    }
}
