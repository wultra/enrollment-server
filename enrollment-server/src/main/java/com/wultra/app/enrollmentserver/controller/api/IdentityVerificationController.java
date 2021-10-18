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
package com.wultra.app.enrollmentserver.controller.api;

import com.wultra.app.enrollmentserver.errorhandling.InvalidDocumentException;
import com.wultra.app.enrollmentserver.impl.service.IdentityVerificationService;
import com.wultra.app.enrollmentserver.model.request.DocumentStatusRequest;
import com.wultra.app.enrollmentserver.model.request.DocumentSubmitRequest;
import com.wultra.app.enrollmentserver.model.response.DocumentStatusResponse;
import com.wultra.app.enrollmentserver.model.response.DocumentSubmitResponse;
import com.wultra.app.enrollmentserver.model.response.DocumentUploadResponse;
import io.getlime.core.rest.model.base.request.ObjectRequest;
import io.getlime.core.rest.model.base.response.ObjectResponse;
import io.getlime.core.rest.model.base.response.Response;
import io.getlime.security.powerauth.crypto.lib.encryptor.ecies.model.EciesScope;
import io.getlime.security.powerauth.crypto.lib.enums.PowerAuthSignatureTypes;
import io.getlime.security.powerauth.rest.api.base.authentication.PowerAuthApiAuthentication;
import io.getlime.security.powerauth.rest.api.base.encryption.EciesEncryptionContext;
import io.getlime.security.powerauth.rest.api.base.exception.PowerAuthAuthenticationException;
import io.getlime.security.powerauth.rest.api.spring.annotation.EncryptedRequestBody;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuth;
import io.getlime.security.powerauth.rest.api.spring.annotation.PowerAuthEncryption;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller publishing REST services for identity document verification.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@ConditionalOnProperty(
        value = "enrollment-server.identity-verification.enabled",
        havingValue = "true"
)
@RestController
@RequestMapping(value = "api/identity")
public class IdentityVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(IdentityVerificationController.class);

    private final IdentityVerificationService identityVerificationService;

    /**
     * Controller constructor.
     *
     * @param identityVerificationService Activation code service.
     */
    @Autowired
    public IdentityVerificationController(IdentityVerificationService identityVerificationService) {
        this.identityVerificationService = identityVerificationService;
    }

    /**
     * Submit identity-related documents for verification.
     * @param request Document submit request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document submit response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     */
    @RequestMapping(value = "submit", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/submit", signatureType = {
            PowerAuthSignatureTypes.POSSESSION_BIOMETRY,
            PowerAuthSignatureTypes.POSSESSION_KNOWLEDGE
    })
    public ObjectResponse<DocumentSubmitResponse> submitDocuments(@EncryptedRequestBody ObjectRequest<DocumentSubmitRequest> request,
                                                                  @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                  @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when submitting documents for verification");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when submitting documents for verification");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when submitting documents for verification");
        }

        // Submit documents for verification
        final DocumentSubmitResponse response = identityVerificationService.submitDocuments(request.getRequestObject(), apiAuthentication);
        return new ObjectResponse<>(response);
    }

    /**
     * Upload a single document related to identity verification. This endpoint is used for upload of large documents.
     * @param requestData Binary request data.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document upload response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     * @throws InvalidDocumentException Thrown when document is invalid.
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/upload", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentUploadResponse> uploadDocument(@EncryptedRequestBody byte[] requestData,
                                                                 @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                                 @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException, InvalidDocumentException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when uploading document for verification");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when uploading document for verification");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when submitting documents for verification");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when uploading document for verification");
        }

        if (requestData == null) {
            logger.error("Invalid request received when submitting documents for verification");
            throw new PowerAuthAuthenticationException("Invalid request received when uploading document for verification");
        }

        // Process upload document request
        final DocumentUploadResponse response = identityVerificationService.uploadDocument(requestData);
        return new ObjectResponse<>(response);
    }

    /**
     * Check status of document verification related to identity.
     * @param request Document status request.
     * @param eciesContext ECIES context.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     */
    @RequestMapping(value = "status", method = RequestMethod.POST)
    @PowerAuthEncryption(scope = EciesScope.ACTIVATION_SCOPE)
    @PowerAuth(resourceId = "/api/identity/status", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public ObjectResponse<DocumentStatusResponse> checkStatus(@EncryptedRequestBody ObjectRequest<DocumentStatusRequest> request,
                                                              @Parameter(hidden = true) EciesEncryptionContext eciesContext,
                                                              @Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when checking document verification status");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when checking document verification status");
        }

        // Check if the request was correctly decrypted
        if (eciesContext == null) {
            logger.error("ECIES encryption failed when checking document verification status");
            throw new PowerAuthAuthenticationException("ECIES decryption failed when checking document verification status");
        }

        if (request == null || request.getRequestObject() == null) {
            logger.error("Invalid request received when checking document verification status");
            throw new PowerAuthAuthenticationException("Invalid request received when checking document verification status");
        }

        // Process upload document request
        final DocumentStatusResponse response = identityVerificationService.checkStatus(request.getRequestObject(), apiAuthentication);
        return new ObjectResponse<>(response);
    }

    /**
     * Cleanup documents related to identity verification.
     * @param apiAuthentication PowerAuth authentication.
     * @return Document status response.
     * @throws PowerAuthAuthenticationException Thrown when request authentication fails.
     */
    @RequestMapping(value = "cleanup", method = RequestMethod.POST)
    @PowerAuth(resourceId = "/api/identity/cleanup", signatureType = {
            PowerAuthSignatureTypes.POSSESSION
    })
    public Response cleanup(@Parameter(hidden = true) PowerAuthApiAuthentication apiAuthentication) throws PowerAuthAuthenticationException {
        // Check if the authentication object is present
        if (apiAuthentication == null) {
            logger.error("Unable to verify device registration when performing document cleanup");
            throw new PowerAuthAuthenticationException("Unable to verify device registration when performing document cleanup");
        }

        // Process cleanup request
        return identityVerificationService.cleanup(apiAuthentication);
    }
}