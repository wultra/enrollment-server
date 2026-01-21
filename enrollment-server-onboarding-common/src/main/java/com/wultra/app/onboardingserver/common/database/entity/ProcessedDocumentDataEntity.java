package com.wultra.app.onboardingserver.common.database.entity;

/*
 * PowerAuth Enrollment Server
 * Copyright (C) 2026 Wultra s.r.o.
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

import com.wultra.app.enrollmentserver.model.enumeration.ProcessedDocumentDataType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Entity representing document data processed by provider.
 *
 * @author Michal Rozehnal, michal.rozehnal@wultra.com
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "es_processed_document_data")
public class ProcessedDocumentDataEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 7685715667785423080L;

    /**
     * ID of the processed document data.
     */
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    /**
     * Processed document binary data.
     */
    @Column(name = "data", nullable = false, columnDefinition = "CLOB")
    private byte[] data;

    /**
     * Type of the processed document data.
     */
    @Column(name = "data_type", nullable = false)
    private ProcessedDocumentDataType dataType;

    /**
     * Timestamp when the processed document data was created.
     */
    @Column(name = "timestamp_created", nullable = false)
    private Date timestampCreated;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final DocumentDataEntity that)) return false;
        return id.equals(that.getId()) && timestampCreated.equals(that.getTimestampCreated());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestampCreated);
    }

    @Override
    public String toString() {
        return "ProcessedDocumentDataEntity(" +
                "id=" + id +
                ", data=" + (data != null ? "byte[" + data.length + "]" : null) +
                ", dataType=" + dataType +
                ", timestampCreated=" + timestampCreated + ")";
    }
}
