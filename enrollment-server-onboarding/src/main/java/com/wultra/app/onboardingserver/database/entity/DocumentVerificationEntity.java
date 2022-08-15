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

package com.wultra.app.onboardingserver.database.entity;

import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing a document verification record.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString(of = {"id", "type", "uploadId"})
@NoArgsConstructor
@Entity
@Table(name = "es_document_verification")
public class DocumentVerificationEntity implements Serializable {

    private static final long serialVersionUID = -8237002126712707796L;

    /**
     * Autogenerated identifier
     */
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", nullable = false)
    private String id;

    /**
     * Activation identifier
     */
    @Column(name = "activation_id", nullable = false)
    private String activationId;

    /**
     * Identifier of the related identity verification entity
     */
    @ManyToOne
    @JoinColumn(name = "identity_verification_id", referencedColumnName = "id", nullable = false)
    private IdentityVerificationEntity identityVerification;

    /**
     * Type of the document
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DocumentType type;

    /**
     * Typically FRONT, BACK where relevant or null
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private CardSide side;

    /**
     * Identifier of document with opposite side
     */
    @Column(name = "other_side_id")
    private String otherSideId;

    /**
     * Name of provider which performed the verification
     */
    @Column(name = "provider_name")
    private String providerName;

    /**
     * Status of the document processing
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    /**
     * Filename specified during upload from mobile client
     */
    @Column(name = "filename", nullable = false)
    private String filename;

    /**
     * Upload identifier in remote document verification system
     */
    @Column(name = "upload_id")
    private String uploadId;

    /**
     * Verification identifier in remote document verification system
     */
    @Column(name = "verification_id")
    private String verificationId;

    /**
     * Identifier of extracted customer photograph from ID card
     */
    @Column(name = "photo_id")
    private String photoId;

    /**
     * Overall score achieved during document verification and fraud detection (0 - 100)
     */
    @Column(name = "verification_score")
    private Integer verificationScore;

    /**
     * Overall reason for the document rejection
     */
    @Column(name = "reject_reason")
    @Lob
    private String rejectReason;

    /**
     * Overall error detail in case a generic error occurred
     */
    @Column(name = "error_detail")
    private String errorDetail;

    /**
     * Identifier of an entity which was replaced by this entity
     */
    @Column(name = "original_document_id")
    private String originalDocumentId;

    /**
     * Whether the document is being used for customer verification or it has been replaced by another record
     */
    @Column(name = "used_for_verification")
    private boolean usedForVerification;

    /**
     * Timestamp when the entity was created
     */
    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    /**
     * Timestamp when the document was uploaded to document verification system
     */
    @Column(name = "timestamp_uploaded")
    private Date timestampUploaded;

    /**
     * Timestamp when the document was verified in document verification system
     */
    @Column(name = "timestamp_verified")
    private Date timestampVerified;

    /**
     * Timestamp when the document was disposed in document verification system
     */
    @Column(name = "timestamp_disposed")
    private Date timestampDisposed;

    /**
     * Timestamp when the entity was last updated
     */
    @Column(name = "timestamp_last_updated")
    private Date timestampLastUpdated;

    /**
     * Document results from different phases of processing (upload, verification) starting with the latest entity
     */
    @OneToMany(mappedBy = "documentVerification", cascade = CascadeType.ALL)
    @OrderBy("timestampCreated desc")
    private Set<DocumentResultEntity> results = new LinkedHashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentVerificationEntity)) return false;
        DocumentVerificationEntity that = (DocumentVerificationEntity) o;
        return type == that.type && side == that.side && filename.equals(that.filename) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, side, filename, timestampCreated);
    }

}
