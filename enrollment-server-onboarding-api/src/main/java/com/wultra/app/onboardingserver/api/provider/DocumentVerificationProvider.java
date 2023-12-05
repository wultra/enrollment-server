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
package com.wultra.app.onboardingserver.api.provider;

import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.onboardingserver.api.errorhandling.DocumentVerificationException;
import com.wultra.app.onboardingserver.common.database.entity.DocumentResultEntity;
import com.wultra.app.onboardingserver.common.database.entity.DocumentVerificationEntity;
import com.wultra.app.onboardingserver.common.errorhandling.RemoteCommunicationException;
import com.wultra.core.annotations.PublicSpi;

import java.util.List;
import java.util.Map;

/**
 * Provider which allows customization of the document verification process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@PublicSpi
public interface DocumentVerificationProvider {

    /**
     * Check document submit result and return data extracted from the document (including photo) and identifiers
     *
     * @param id Owner identification.
     * @param document Document entity.
     * @return Result of the document submit
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    DocumentsSubmitResult checkDocumentUpload(OwnerId id, DocumentVerificationEntity document) throws RemoteCommunicationException, DocumentVerificationException;

    /**
     * Analyze documents and return data extracted from documents (including photo) and identifiers. With enabled
     * asynchronous processing no extracted data are returned. The data has be checked with {@link #checkDocumentUpload(OwnerId, DocumentVerificationEntity)}
     *
     * @param id Owner identification.
     * @param documents Documents to be submitted
     * @return Result of the documents submit
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws RemoteCommunicationException, DocumentVerificationException;

    /**
     * A feature flag whether the selfie result gained by {@link PresenceCheckProvider} should be stored by {@link #submitDocuments(OwnerId, List)}.
     * <p>
     * Some implementation may require this cross-sending between providers by the Onboarding server, some providers may handle it internally.
     *
     * @return {@code true} if cross-sending between providers should be handled by Onboarding server, {@code false} otherwise.
     */
    boolean shouldStoreSelfie();

    /**
     * Analyze previously submitted documents, detect frauds, return binary result
     *
     * @param id Owner identification.
     * @param uploadIds Ids of previously uploaded documents
     * @return Result of the documents verification
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException;

    /**
     * Gets the result of verification
     *
     * @param id Owner identification.
     * @param verificationId Identification of a previously run verification
     * @return Result of a previously run documents verification
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws RemoteCommunicationException, DocumentVerificationException;

    /**
     * Gets a photo
     * @param photoId Identification of the photo
     * @return Photo image
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    Image getPhoto(String photoId) throws RemoteCommunicationException, DocumentVerificationException;

    /**
     * Disposes documents which are no longer needed, throw away any sensitive data
     *
     * @param id Owner identification.
     * @param uploadIds Ids of previously uploaded documents
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    void cleanupDocuments(OwnerId id, List<String> uploadIds) throws RemoteCommunicationException, DocumentVerificationException;

    // TODO reconsider this method, mention it in the tldr doc
    /**
     * Parses all detected rejection reasons from a JSON result of verification
     * @param docResult Document result entity
     * @return List of rejection reasons parsed from the verification result
     * @throws DocumentVerificationException When an error during parsing rejection reasons occurred
     */
    List<String> parseRejectionReasons(DocumentResultEntity docResult) throws DocumentVerificationException;

    /**
     * Initializes document verification SDK.
     *
     * @param id Owner identification.
     * @param initAttributes Initialization attributes
     * @return Info with data related to the verification process in SDK.
     * @throws RemoteCommunicationException In case of remote communication error
     * @throws DocumentVerificationException In case of business logic error
     */
    VerificationSdkInfo initVerificationSdk(OwnerId id, Map<String, String> initAttributes) throws RemoteCommunicationException, DocumentVerificationException;

}
