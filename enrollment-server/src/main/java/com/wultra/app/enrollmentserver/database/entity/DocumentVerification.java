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

package com.wultra.app.enrollmentserver.database.entity;

import com.wultra.app.enrollmentserver.model.enumeration.DocumentStatus;
import com.wultra.app.enrollmentserver.model.enumeration.CardSide;
import com.wultra.app.enrollmentserver.model.enumeration.DocumentType;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing a document verification record.
 *
 * @author Roman Strobl, roman.strobl@wultra.com
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "es_document_verification")
public class DocumentVerification implements Serializable {

    private static final long serialVersionUID = -8237002126712707796L;

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "activation_id", nullable = false)
    private String activationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "side")
    private CardSide side;

    @Column(name = "other_side_id")
    private String otherSideId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "verification_id")
    private String verificationId;

    @Column(name = "verification_score")
    private Integer verificationScore;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "original_document_id")
    private String originalDocumentId;

    @Column(name = "used_for_verification")
    private boolean usedForVerification;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Column(name = "timestamp_uploaded")
    private Date timestampUploaded;

    @Column(name = "timestamp_verified")
    private Date timestampVerified;

    @Column(name = "timestamp_disposed")
    private Date timestampDisposed;

    @Column(name = "timestamp_last_updated")
    private Date timestampLastUpdated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentVerification)) return false;
        DocumentVerification that = (DocumentVerification) o;
        return type == that.type && side == that.side && filename.equals(that.filename) && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, side, filename, timestampCreated);
    }
}