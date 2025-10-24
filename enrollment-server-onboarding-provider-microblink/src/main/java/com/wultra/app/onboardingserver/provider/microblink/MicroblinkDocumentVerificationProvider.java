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

import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.api.provider.DocumentVerificationProvider;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link DocumentVerificationProvider} with <a href="https://www.microblink.com/">Microblink</a>.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
class MicroblinkDocumentVerificationProvider implements DocumentVerificationProvider {

    @Override
    public DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public boolean shouldStoreSelfie() {
        throw new NotImplementedException();
    }

    @Override
    public DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public Image getPhoto(String photoId) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public void cleanupDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException {
        throw new NotImplementedException();
    }

    @Override
    public VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) throws RemoteCommunicationException, DocumentVerificationException {
        throw new NotImplementedException();
    }
}
