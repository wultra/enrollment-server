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
package com.wultra.app.enrollmentserver.provider;

import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.model.integration.*;

import java.util.List;

/**
 * Provider which allows customization of the document verification process.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
public interface DocumentVerificationProvider {

    /**
     * Check document submit result and return data extracted from the document (including photo) and identifiers
     *
     * @param id Owner identification.
     * @param document Submitted document.
     * @return Result of the document submit
     * @throws DocumentVerificationException When an error during submitting of documents occurred
     */
    DocumentsSubmitResult checkDocumentUpload(OwnerId id, SubmittedDocument document) throws DocumentVerificationException;

    /**
     * Analyze documents and return data extracted from documents (including photo) and identifiers
     *
     * @param id Owner identification.
     * @param documents Documents to be submitted
     * @return Result of the documents submit
     * @throws DocumentVerificationException When an error during submitting of documents occurred
     */
    DocumentsSubmitResult submitDocuments(OwnerId id, List<SubmittedDocument> documents) throws DocumentVerificationException;

    /**
     * Analyze previously submitted documents, detect frauds, return binary result
     *
     * @param id Owner identification.
     * @param uploadIds Ids of previously uploaded documents
     * @return Result of the documents verification
     * @throws DocumentVerificationException When an error during verification occurred
     */
    DocumentsVerificationResult verifyDocuments(OwnerId id, List<String> uploadIds) throws DocumentVerificationException;

    /**
     * Gets the result of verification
     *
     * @param id Owner identification.
     * @param verificationId Identification of a previously run verification
     * @return Result of a previously run documents verification
     * @throws DocumentVerificationException When an error during verification result obtaining occurred
     */
    DocumentsVerificationResult getVerificationResult(OwnerId id, String verificationId) throws DocumentVerificationException;

    /**
     * Gets a photo
     * @param photoId Identification of the photo
     * @return Photo image
     * @throws DocumentVerificationException When an error during getting of a photo occurred
     */
    Image getPhoto(String photoId) throws DocumentVerificationException;

    /**
     * Disposes documents which are no longer needed, throw away any sensitive data
     *
     * @param id Owner identification.
     * @param uploadIds Ids of previously uploaded documents
     * @throws DocumentVerificationException When an error during documents cleanup occurred
     */
    void cleanupDocuments(OwnerId id, List<String> uploadIds) throws DocumentVerificationException;

}
