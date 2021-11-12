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

import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.service.document.DocumentProcessingService;
import com.wultra.app.enrollmentserver.impl.service.internal.JsonSerializationService;
import com.wultra.app.enrollmentserver.impl.util.PowerAuthUtil;
import com.wultra.app.enrollmentserver.model.enumeration.*;
import com.wultra.app.enrollmentserver.model.integration.*;
import com.wultra.app.enrollmentserver.provider.PresenceCheckProvider;
import io.getlime.security.powerauth.rest.api.spring.authentication.PowerAuthApiAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

/**
 * Service implementing presence check.
 *
 * @author Lukas Lukovsky, lukas.lukovsky@wultra.com
 */
@Service
public class PresenceCheckService {

    private static final Logger logger = LoggerFactory.getLogger(PresenceCheckService.class);

    private final DocumentVerificationRepository documentVerificationRepository;

    private final DocumentProcessingService documentProcessingService;

    private final IdentityVerificationService identityVerificationService;

    private final JsonSerializationService jsonSerializationService;

    private final PresenceCheckProvider presenceCheckProvider;

    @Autowired
    public PresenceCheckService(
            DocumentVerificationRepository documentVerificationRepository,
            DocumentProcessingService documentProcessingService,
            IdentityVerificationService identityVerificationService,
            JsonSerializationService jsonSerializationService,
            PresenceCheckProvider presenceCheckProvider) {
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.jsonSerializationService = jsonSerializationService;
        this.presenceCheckProvider = presenceCheckProvider;
    }

    @Transactional
    public SessionInfo init(PowerAuthApiAuthentication apiAuthentication)
            throws DocumentVerificationException, PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        Optional<IdentityVerificationEntity> idVerificationOptional = identityVerificationService.findBy(ownerId);
        if (!idVerificationOptional.isPresent()) {
            logger.error("No identity verification entity found to initialize the presence check, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        }
        IdentityVerificationEntity idVerification = idVerificationOptional.get();

        if (IdentityVerificationPhase.PRESENCE_CHECK.equals(idVerification.getPhase()) &&
            IdentityVerificationStatus.ACCEPTED.equals(idVerification.getStatus())) {
            logger.error("The presence check is already accepted, not allowed to initialize it again, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        } else if (!IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())) {
            logger.error("The verification phase is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationPhase.DOCUMENT_UPLOAD, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        } else if (!IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            logger.error("The verification status is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationStatus.VERIFICATION_PENDING, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        Optional<DocumentVerificationEntity> docVerificationEntityWithPhoto =
                documentVerificationRepository.findByActivationIdAndPhotoIdNotNull(ownerId.getActivationId());

        if (docVerificationEntityWithPhoto.isPresent()) {
            String photoId = docVerificationEntityWithPhoto.get().getPhotoId();
            Image photo = identityVerificationService.getPhotoById(photoId);
            presenceCheckProvider.initPresenceCheck(ownerId, photo);
        } else {
            logger.error("Missing selfie photo to initialize presence check, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        SessionInfo sessionInfo = presenceCheckProvider.startPresenceCheck(ownerId);

        String sessionInfoJson = jsonSerializationService.serialize(sessionInfo);
        if (sessionInfoJson == null) {
            logger.error("JSON serialization of session info failed, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        idVerification.setSessionInfo(sessionInfoJson);
        idVerification.setPhase(IdentityVerificationPhase.PRESENCE_CHECK);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);
        idVerification.setTimestampLastUpdated(ownerId.getTimestamp());

        return sessionInfo;
    }

    @Transactional
    public PresenceCheckResult checkPresenceVerification(PowerAuthApiAuthentication apiAuthentication, SessionInfo sessionInfo)
            throws DocumentVerificationException, PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);
        PresenceCheckResult result = presenceCheckProvider.getResult(ownerId, sessionInfo);
        if (PresenceCheckStatus.ACCEPTED.equals(result.getStatus())) {
            Image photo = result.getPhoto();
            if (photo == null) {
                logger.error("Missing person photo from presence verification, {}", ownerId);
                throw new PresenceCheckException("Missing person photo from presence verification");
            }

            SubmittedDocument submittedDoc = new SubmittedDocument();
            submittedDoc.setDocumentId(ownerId.getActivationId() + "-selfie-photo");
            submittedDoc.setPhoto(photo);
            submittedDoc.setType(DocumentType.SELFIE_PHOTO);

            DocumentVerificationEntity docVerificationEntity = new DocumentVerificationEntity();
            docVerificationEntity.setStatus(DocumentStatus.VERIFICATION_PENDING);
            docVerificationEntity.setType(DocumentType.SELFIE_PHOTO);
            docVerificationEntity.setTimestampCreated(ownerId.getTimestamp());
            docVerificationEntity.setFilename(result.getPhoto().getFilename());

            DocumentSubmitResult documentSubmitResult =
                    documentProcessingService.submitDocumentToProvider(ownerId, docVerificationEntity, submittedDoc);
            docVerificationEntity.setTimestampUploaded(ownerId.getTimestamp());
            docVerificationEntity.setUploadId(documentSubmitResult.getUploadId());

            documentVerificationRepository.save(docVerificationEntity);

            documentVerificationRepository.setVerificationPending(ownerId.getActivationId());
        }
        return result;
    }

    public void cleanup(PowerAuthApiAuthentication apiAuthentication) throws PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        presenceCheckProvider.cleanupIdentityData(ownerId);
    }

}
