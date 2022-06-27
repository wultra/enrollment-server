/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2021 Wultra s.r.o.
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
package com.wultra.app.onboardingserver.docverify;

import com.wultra.app.enrollmentserver.model.integration.DocumentSubmitResult;
import com.wultra.app.enrollmentserver.model.integration.DocumentsSubmitResult;
import com.wultra.app.enrollmentserver.model.integration.OwnerId;
import com.wultra.app.enrollmentserver.model.integration.SubmittedDocument;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
public class AbstractDocumentVerificationProviderTest {

    public void assertSubmittedDocuments(OwnerId ownerId, List<SubmittedDocument> documents, DocumentsSubmitResult result) throws Exception {
        assertEquals(documents.size(), result.getResults().size(), "Different size of submitted documents than expected");
        assertNotNull(result.getExtractedPhotoId(), "Missing extracted photoId");

        List<String> submittedDocsIds = result.getResults().stream()
                .map(DocumentSubmitResult::getDocumentId)
                .collect(Collectors.toList());
        assertEquals(documents.size(), submittedDocsIds.size(), "Different size of unique submitted documents than expected");
        documents.forEach(document -> {
            assertTrue(submittedDocsIds.contains(document.getDocumentId()));
        });

        result.getResults().forEach(submitResult -> {
            assertNull(submitResult.getErrorDetail());
            assertNull(submitResult.getRejectReason());

            assertNotNull(submitResult.getUploadId());
        });
    }

}
