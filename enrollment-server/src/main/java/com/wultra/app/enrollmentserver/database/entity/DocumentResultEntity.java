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

import com.wultra.app.enrollmentserver.model.enumeration.DocumentProcessingPhase;
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
@Table(name = "es_document_result")
public class DocumentResultEntity implements Serializable {

    private static final long serialVersionUID = -760284276164288362L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    // TODO - FK relationship
    // @OneToOne(targetEntity = DocumentVerificationEntity.class)
    // @JoinColumn(name = "document_verification_id", referencedColumnName = "id", nullable = false)
    @Column(name = "document_verification_id", nullable = false)
    private String documentVerificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private DocumentProcessingPhase phase;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "verification_result")
    private String verificationResult;

    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "extracted_data")
    private String extractedData;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentResultEntity)) return false;
        DocumentResultEntity that = (DocumentResultEntity) o;
        return documentVerificationId.equals(that.documentVerificationId) && phase == that.phase && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentVerificationId, phase, timestampCreated);
    }
}