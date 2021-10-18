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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.database.DocumentDataRepository;
import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.IdentityVerificationRepository;
import com.wultra.app.enrollmentserver.errorhandling.InvalidDocumentException;
import com.wultra.app.enrollmentserver.model.Document;
import com.wultra.app.enrollmentserver.model.request.DocumentStatusRequest;
import com.wultra.app.enrollmentserver.model.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.model.response.DocumentStatusResponse;
import com.wultra.app.enrollmentserver.model.response.DocumentSubmitResponse;
import com.wultra.app.enrollmentserver.model.response.DocumentUploadResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * Service implementing document identity verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Service
public class IdentityVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationService.class);

    private final DataExtractionService dataExtractionService;
    private final DocumentDataRepository documentDataRepository;
    private final DocumentVerificationRepository documentVerificationRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    /**
     * Service constructor.
     * @param dataExtractionService Data extraction service.
     * @param documentDataRepository Document data repository.
     * @param documentVerificationRepository Document verification repository.
     * @param identityVerificationRepository Identity verification repository.
     */
    @Autowired
    public IdentityVerificationService(DataExtractionService dataExtractionService, DocumentDataRepository documentDataRepository, DocumentVerificationRepository documentVerificationRepository, IdentityVerificationRepository identityVerificationRepository) {
        this.dataExtractionService = dataExtractionService;
        this.documentDataRepository = documentDataRepository;
        this.documentVerificationRepository = documentVerificationRepository;
        this.identityVerificationRepository = identityVerificationRepository;
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document submit response.
     */
    @Transactional
    public DocumentSubmitResponse submitDocuments(DocumentSubmitRequest request, PowerAuthApiAuthentication apiAuthentication) {
        return new DocumentSubmitResponse();
    }

    /**
     * Upload a single document related to identity verification.
     * @param requestData Binary document data.
     * @return Document upload response.
     * @throws InvalidDocumentException Thrown when document is invalid.
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(byte[] requestData) throws InvalidDocumentException {
        Document document = dataExtractionService.extractDocument(requestData);
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setFilename(document.getFilename());
        response.setId(document.getId());
        return response;
    }

    /**
     * Check status of document verification related to identity.
     * @param request Document status request.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     */
    @Transactional
    public DocumentStatusResponse checkStatus(DocumentStatusRequest request, PowerAuthApiAuthentication apiAuthentication) {
        return new DocumentStatusResponse();
    }

    /**
     * Cleanup documents related to identity verification.
     * @param apiAuthentication PowerAuth authentication.
     * @return Response.
     */
    @Transactional
    public Response cleanup(PowerAuthApiAuthentication apiAuthentication) {
        String activationId = apiAuthentication.getActivationId();
        // Delete all large documents by activation ID
        documentDataRepository.deleteAllByActivationId(activationId);
        // Set status of all in-progress document verifications to failed
        documentVerificationRepository.failInProgressVerifications(activationId);
        // Set status of all in-progress identity verifications to failed
        identityVerificationRepository.failInProgressVerifications(activationId);
        return new Response();
    }
}