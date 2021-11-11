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
package com.wultra.app.enrollmentserver.impl.service;

import com.wultra.app.enrollmentserver.database.DocumentVerificationRepository;
import com.wultra.app.enrollmentserver.database.entity.DocumentVerificationEntity;
import com.wultra.app.enrollmentserver.database.entity.IdentityVerificationEntity;
import com.wultra.app.enrollmentserver.errorhandling.DocumentVerificationException;
import com.wultra.app.enrollmentserver.errorhandling.PresenceCheckException;
import com.wultra.app.enrollmentserver.impl.service.document.DocumentProcessingService;
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

    private final PresenceCheckProvider presenceCheckProvider;

    @Autowired
    public PresenceCheckService(
            DocumentVerificationRepository documentVerificationRepository,
            DocumentProcessingService documentProcessingService,
            IdentityVerificationService identityVerificationService,
            PresenceCheckProvider presenceCheckProvider) {
        this.documentVerificationRepository = documentVerificationRepository;
        this.documentProcessingService = documentProcessingService;
        this.identityVerificationService = identityVerificationService;
        this.presenceCheckProvider = presenceCheckProvider;
    }

    @Transactional
    public void init(PowerAuthApiAuthentication apiAuthentication)
            throws DocumentVerificationException, PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        Optional<IdentityVerificationEntity> idVerificationOptional = identityVerificationService.findBy(ownerId);
        if (!idVerificationOptional.isPresent()) {
            logger.error("No identity verification entity found to initialize the presence check, {}", ownerId);
            throw new PresenceCheckException("Unable to initialize presence check");
        }
        IdentityVerificationEntity idVerification = idVerificationOptional.get();

        if (!IdentityVerificationPhase.DOCUMENT_UPLOAD.equals(idVerification.getPhase())) {
            logger.error("The verification phase is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationPhase.DOCUMENT_UPLOAD, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        }
        if (!IdentityVerificationStatus.VERIFICATION_PENDING.equals(idVerification.getStatus())) {
            logger.error("The verification status is {} but expected {}, {}",
                    idVerification.getPhase(), IdentityVerificationStatus.VERIFICATION_PENDING, ownerId
            );
            throw new PresenceCheckException("Unable to initialize presence check");
        }

        idVerification.setPhase(IdentityVerificationPhase.PRESENCE_CHECK);
        idVerification.setStatus(IdentityVerificationStatus.IN_PROGRESS);

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
    }

    @Transactional
    public SessionInfo start(PowerAuthApiAuthentication apiAuthentication) throws PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        SessionInfo sessionInfo = presenceCheckProvider.startPresenceCheck(ownerId);
        identityVerificationService.markVerificationPending(apiAuthentication);

        return sessionInfo;
    }

    @Transactional
    public PresenceCheckStatus checkPresenceVerification(PowerAuthApiAuthentication apiAuthentication, SessionInfo sessionInfo)
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

            identityVerificationService.startVerification(ownerId);
        }
        return result.getStatus();
    }

    public void cleanup(PowerAuthApiAuthentication apiAuthentication) throws PresenceCheckException {
        OwnerId ownerId = PowerAuthUtil.getOwnerId(apiAuthentication);

        presenceCheckProvider.cleanupIdentityData(ownerId);
    }

}
