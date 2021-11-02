/*
 * PowerAuth Command-line utility
 * Copyright 2021 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wultra.app.docverify;

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

    public void assertSubmitDocumentsTest(OwnerId ownerId, List<SubmittedDocument> documents, DocumentsSubmitResult result) throws Exception {
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

            assertNotNull(submitResult.getExtractedData());
            assertNotNull(submitResult.getUploadId());
            assertNotNull(submitResult.getValidationResult());
        });

        assertNotNull(result.getExtractedPhotoId());
    }

}
