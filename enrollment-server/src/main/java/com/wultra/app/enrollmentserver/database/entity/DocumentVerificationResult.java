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
public class DocumentVerificationResult implements Serializable {

    private static final long serialVersionUID = -760284276164288362L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    // TODO - FK relationship
    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private DocumentProcessingPhase phase;

    @Column(name = "data")
    private String data;

    @Column(name = "validation_result")
    private String validationResult;

    @Column(name = "errors")
    private String errors;

    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentVerificationResult)) return false;
        DocumentVerificationResult that = (DocumentVerificationResult) o;
        return documentId.equals(that.documentId) && phase == that.phase && timestampCreated.equals(that.timestampCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, phase, timestampCreated);
    }
}